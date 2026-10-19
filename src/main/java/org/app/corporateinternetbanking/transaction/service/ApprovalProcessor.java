package org.app.corporateinternetbanking.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.commons.locking.RowLocks;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.integration.currency.CurrencyExchangeService;
import org.app.corporateinternetbanking.ledger.enums.EntryType;
import org.app.corporateinternetbanking.ledger.service.LedgerService;
import org.app.corporateinternetbanking.security.TenantAccess;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.domain.repository.TransactionRepository;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalRequest;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.transaction.exceptions.InvalidStatus;
import org.app.corporateinternetbanking.transaction.exceptions.IsNull;
import org.app.corporateinternetbanking.transaction.exceptions.TransactionAlreadyProcessed;
import org.app.corporateinternetbanking.transaction.exceptions.TransactionDoesNotExist;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.app.corporateinternetbanking.user.service.UserServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// The database half of an approval. Each method runs in its own transaction and holds row locks on the
// transaction and its accounts, so two approvers acting at the same moment are serialized: the second
// one sees the transaction already processed instead of moving the money twice.
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalProcessor {

    private final TransactionRepository transactionRepository;
    private final CurrencyExchangeService currencyService;
    private final LedgerService ledgerService;
    private final UserServiceImpl userService;
    private final RowLocks rowLocks;

    // Applies the approver's decision. Internal transfers and rejections are completed here; payouts are
    // only marked APPROVED and committed, and the caller then asks Paystack to send the money.
    @Transactional(rollbackFor = Exception.class)
    public Transaction decide(TransactionApprovalRequest request)
            throws TransactionDoesNotExist, UserNotFound, UnauthorizedAccess, TransactionAlreadyProcessed,
                    InvalidStatus, CurrencyNotFound, IsNull {
        Transaction transaction = transactionRepository
                .findById(request.getTransactionId())
                .orElseThrow(() -> new TransactionDoesNotExist("This transaction does not exist"));
        User approver = userService.getCurrentUser();
        if (!approver.getRole().equals(UserRole.APPROVER)) {
            throw new UnauthorizedAccess("The approver must have the role  APPROVER");
        }
        // Funds leave the source account's organization, so only that organization's approvers may approve.
        if (transaction.getSourceAccount() != null) {
            TenantAccess.assertMemberOf(approver, transaction.getSourceAccount().getOrganization());
        }
        if (request.getStatus() != TransactionStatus.SUCCESS && request.getStatus() != TransactionStatus.REJECTED) {
            throw new InvalidStatus("invalid status");
        }

        // Fetch the exchange rate before taking any locks so no lock is held during the HTTP call.
        BigDecimal rate = null;
        if (request.getStatus() == TransactionStatus.SUCCESS
                && transaction.getType() == TransactionType.INTERNAL_TRANSFER
                && !sameCurrency(transaction.getSourceAccount(), transaction.getDestinationAccount())) {
            rate = currencyService.getRate(
                    transaction.getSourceAccount().getCurrency().getCode(),
                    transaction.getDestinationAccount().getCurrency().getCode());
        }

        rowLocks.lockTransaction(transaction);
        // Re-checked under the lock: another approver (or the expiry job) may have just processed it.
        // Only maker-initiated transfers/payouts await approval. Funding (PENDING) is settled by the
        // Paystack webhook and has no source account, so it must not go through this path.
        if (transaction.getStatus() != TransactionStatus.PENDING_APPROVAL) {
            throw new TransactionAlreadyProcessed("This transaction is not awaiting approval");
        }
        rowLocks.lockAccounts(transaction.getSourceAccount(), transaction.getDestinationAccount());

        transaction.setProcessedBy(approver);
        transaction.setProcessedAt(LocalDateTime.now());
        if (request.getStatus() == TransactionStatus.REJECTED) {
            transaction.setStatus(TransactionStatus.REJECTED);
            // Funds were only moved from available to reserved at initiation; totalBalance never changed.
            Account sourceAccount = transaction.getSourceAccount();
            BigDecimal amount = transaction.getAmount();
            sourceAccount.setAvailableBalance(
                    sourceAccount.getAvailableBalance().add(amount));
            sourceAccount.setReservedBalance(sourceAccount.getReservedBalance().subtract(amount));
        } else if (transaction.getType() == TransactionType.INTERNAL_TRANSFER) {
            processInternalTransfer(transaction, rate);
        } else if (transaction.getType() == TransactionType.EXTERNAL_PAYOUT) {
            if (transaction.getPayoutRecipient() == null) {
                throw new IsNull("Recipient missing");
            }
            // Committed before Paystack is called: in test mode the transfer.success webhook can arrive
            // before the approval request finishes, and the webhook only settles APPROVED payouts.
            transaction.setStatus(TransactionStatus.APPROVED);
        }
        return transactionRepository.save(transaction);
    }

    // Paystack refused the transfer, so no money moved: put the payout back so it can be approved again or rejected.
    @Transactional(rollbackFor = Exception.class)
    public void revertPayoutApproval(Long transactionId) {
        transactionRepository.findById(transactionId).ifPresent(transaction -> {
            rowLocks.lockTransaction(transaction);
            if (transaction.getStatus() == TransactionStatus.APPROVED) {
                transaction.setStatus(TransactionStatus.PENDING_APPROVAL);
                transaction.setProcessedBy(null);
                transaction.setProcessedAt(null);
                transactionRepository.save(transaction);
            }
        });
    }

    private static boolean sameCurrency(Account source, Account destination) {
        return source.getCurrency().getCode().equals(destination.getCurrency().getCode());
    }

    private void processInternalTransfer(Transaction transaction, BigDecimal rate) {
        Account source = transaction.getSourceAccount();
        Account destination = transaction.getDestinationAccount();
        BigDecimal amount = transaction.getAmount();
        BigDecimal creditedAmount = rate == null ? amount : amount.multiply(rate);

        BigDecimal newSourceBalance = source.getTotalBalance().subtract(amount);
        source.setTotalBalance(newSourceBalance);
        source.setReservedBalance(source.getReservedBalance().subtract(amount));
        ledgerService.createEntry(
                source, transaction, EntryType.DEBIT, source.getCurrency().getCode(), amount, newSourceBalance);

        BigDecimal newDestinationBalance = destination.getTotalBalance().add(creditedAmount);
        destination.setTotalBalance(newDestinationBalance);
        destination.setAvailableBalance(destination.getAvailableBalance().add(creditedAmount));
        ledgerService.createEntry(
                destination,
                transaction,
                EntryType.CREDIT,
                destination.getCurrency().getCode(),
                creditedAmount,
                newDestinationBalance);

        if (rate != null) {
            transaction.setConvertedAmount(creditedAmount);
            transaction.setExchangeRate(rate);
        }
        transaction.setStatus(TransactionStatus.SUCCESS);
    }
}

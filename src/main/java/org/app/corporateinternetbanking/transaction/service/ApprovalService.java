package org.app.corporateinternetbanking.transaction.service;

import static org.app.corporateinternetbanking.transaction.utils.mapper.ApprovalMap.mapApprovalResponse;

import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.integration.currency.CurrencyExchangeService;
import org.app.corporateinternetbanking.integration.paystack.PayStackClient;
import org.app.corporateinternetbanking.ledger.enums.EntryType;
import org.app.corporateinternetbanking.ledger.service.LedgerService;
import org.app.corporateinternetbanking.security.TenantAccess;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.domain.repository.TransactionRepository;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalRequest;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalResponse;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.transaction.exceptions.*;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.app.corporateinternetbanking.user.service.UserServiceImpl;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final TransactionRepository transactionRepository;
    private final CurrencyExchangeService currencyService;
    private final LedgerService ledgerService;
    private final PayStackClient payStackClient;
    private final UserServiceImpl userService;

    public TransactionApprovalResponse approveInternalTransaction(TransactionApprovalRequest request)
            throws TransactionAlreadyProcessed, TransactionDoesNotExist, InvalidStatus, UnsupportedTransactionType,
                    UserNotFound, UnauthorizedAccess, InvalidAmount, AccountDoesNotExist, CurrencyNotFound,
                    InsufficientBalance, IsNull {
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
        // Only maker-initiated transfers/payouts await approval. Funding (PENDING) is settled by the
        // Paystack webhook and has no source account, so it must not go through this path.
        if (transaction.getStatus() != TransactionStatus.PENDING_APPROVAL) {
            throw new TransactionAlreadyProcessed("This transaction is not awaiting approval");
        }
        if (request.getStatus() != TransactionStatus.SUCCESS && request.getStatus() != TransactionStatus.REJECTED) {
            throw new InvalidStatus("invalid status");
        }
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
            transactionRepository.save(transaction);
            return mapApprovalResponse(transaction);
        }
        if (transaction.getType() == TransactionType.INTERNAL_TRANSFER) {
            processInternalTransfer(transaction);
        } else if (transaction.getType() == TransactionType.EXTERNAL_PAYOUT) {
            processExternalPayout(transaction);
        }
        transaction = transactionRepository.save(transaction);
        return mapApprovalResponse(transaction);
    }

    private void processExternalPayout(Transaction transaction)
            throws AccountDoesNotExist, InsufficientBalance, IsNull {

        if (transaction.getPayoutRecipient() == null) {
            throw new IsNull("Recipient missing");
        }
        // Persist APPROVED before calling Paystack: in test mode the transfer.success webhook can
        // arrive before this request finishes, and the webhook only settles APPROVED payouts.
        transaction.setStatus(TransactionStatus.APPROVED);
        transactionRepository.save(transaction);

        Map<String, Object> body = new HashMap<>();
        body.put("source", "balance");
        body.put("amount", PayStackClient.toKobo(transaction.getAmount()));
        body.put("recipient", transaction.getPayoutRecipient().getRecipientCode());
        body.put("reference", transaction.getReference());
        body.put("reason", "CIB payout " + transaction.getReference());
        log.info("Initiating Paystack transfer {} for {} kobo", transaction.getReference(), body.get("amount"));
        try {
            Map<String, Object> response = payStackClient.initiateTransfer(body);
            log.info("Paystack transfer response: {}", response.get("message"));
        } catch (FeignException e) {
            // Transfer was not created: put it back so it can be approved again or rejected.
            transaction.setStatus(TransactionStatus.PENDING_APPROVAL);
            transaction.setProcessedBy(null);
            transaction.setProcessedAt(null);
            transactionRepository.save(transaction);
            throw e;
        }
    }

    private void processInternalTransfer(Transaction transaction) throws CurrencyNotFound {
        Account source = transaction.getSourceAccount();
        Account destination = transaction.getDestinationAccount();
        BigDecimal amount = transaction.getAmount();
        if (source.getCurrency().getCode().equals(destination.getCurrency().getCode())) {
            BigDecimal newSourceBalance = source.getTotalBalance().subtract(amount);
            source.setTotalBalance(newSourceBalance);
            source.setReservedBalance(source.getReservedBalance().subtract(transaction.getAmount()));

            BigDecimal newDestinationBalance = destination.getTotalBalance().add(amount);
            BigDecimal newDestinationAvailableBalance =
                    destination.getAvailableBalance().add(amount);
            destination.setTotalBalance(newDestinationBalance);
            destination.setAvailableBalance(newDestinationAvailableBalance);
        } else {
            String from = source.getCurrency().getCode();
            String to = destination.getCurrency().getCode();

            BigDecimal rate = currencyService.getRate(from, to);
            BigDecimal convertedAmount = amount.multiply(rate);

            BigDecimal newSourceBalance = source.getTotalBalance().subtract(amount);
            source.setTotalBalance(newSourceBalance);
            ledgerService.createEntry(
                    source, transaction, EntryType.DEBIT, source.getCurrency().getCode(), amount, newSourceBalance);

            BigDecimal newDestinationBalance = destination.getTotalBalance().add(convertedAmount);
            destination.setTotalBalance(newDestinationBalance);

            BigDecimal newDestinationAvailableBalance =
                    destination.getAvailableBalance().add(convertedAmount);
            destination.setAvailableBalance(newDestinationAvailableBalance);

            ledgerService.createEntry(
                    destination,
                    transaction,
                    EntryType.CREDIT,
                    destination.getCurrency().getCode(),
                    convertedAmount,
                    newDestinationBalance);
            source.setReservedBalance(source.getReservedBalance().subtract(transaction.getAmount()));
            transaction.setConvertedAmount(convertedAmount);
            transaction.setExchangeRate(rate);
            transaction.setAmount(amount);
        }

        transaction.setType(TransactionType.INTERNAL_TRANSFER);
        transaction.setStatus(TransactionStatus.SUCCESS);
    }
}

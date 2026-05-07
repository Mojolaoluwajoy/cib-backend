package org.app.corporateinternetbanking.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.service.AccountService;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.integration.currency.CurrencyExchangeService;
import org.app.corporateinternetbanking.integration.paystack.PayStackClient;
import org.app.corporateinternetbanking.ledger.enums.EntryType;
import org.app.corporateinternetbanking.ledger.service.LedgerService;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.domain.repository.TransactionRepository;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalRequest;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalResponse;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.transaction.exceptions.*;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.app.corporateinternetbanking.transaction.utils.mapper.ApprovalMap.mapApprovalRequest;
import static org.app.corporateinternetbanking.transaction.utils.mapper.ApprovalMap.mapApprovalResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CurrencyExchangeService currencyService;
    private final LedgerService ledgerService;
    private final AccountService accountService;
    private final PayStackClient payStackClient;

    public TransactionApprovalResponse approveInternalTransaction(TransactionApprovalRequest request) throws TransactionAlreadyProcessed, TransactionDoesNotExist, InvalidStatus, UnsupportedTransactionType, UserNotFound,
            UnauthorizedAccess, InvalidAmount, AccountDoesNotExist, CurrencyNotFound, InsufficientBalance,
            IsNull {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new TransactionDoesNotExist("This transaction does not exist"));
        User user = userRepository.findById(request.getApproverId())
                .orElseThrow(() -> new UserNotFound("This user does not exist"));
        if (!user.getRole().equals(UserRole.APPROVER)) {
            throw new UnauthorizedAccess("The approver must have the role  APPROVER");
        }
        if (!transaction.getStatus().name().equalsIgnoreCase("PENDING") &&
                !transaction.getStatus().name().equalsIgnoreCase("PENDING_APPROVAL")) {
            throw new TransactionAlreadyProcessed("This transaction has been processed");
        }
        if (request.getStatus() != TransactionStatus.SUCCESS && request.getStatus() != TransactionStatus.REJECTED) {
            throw new InvalidStatus("invalid status");
        }
        transaction.setProcessedBy(user);
        if (request.getStatus() == TransactionStatus.REJECTED) {
            transaction.setStatus(TransactionStatus.REJECTED);
            transactionRepository.save(transaction);
            return mapApprovalResponse(transaction);
        }
        transaction.setProcessedAt(LocalDateTime.now());
        mapApprovalRequest(request);
        if (transaction.getType() == TransactionType.INTERNAL_TRANSFER) {
            processInternalTransfer(transaction);
        } else if (transaction.getType() == TransactionType.EXTERNAL_PAYOUT) {
            processExternalPayout(transaction);
        }
        transaction = transactionRepository.save(transaction);
        return mapApprovalResponse(transaction);
    }

    private void processExternalPayout(Transaction transaction) throws AccountDoesNotExist, InsufficientBalance, IsNull {

        if (transaction.getPayoutRecipient() == null) {
            throw new IsNull("Recipient missing");
        }
        transaction.setStatus(TransactionStatus.APPROVED);

        Map<String, Object> body = new HashMap<>();
        body.put("amount", transaction.getAmount().multiply(BigDecimal.valueOf(100)));
        log.info("Transfer amount in kobo: {}", transaction.getAmount().multiply(BigDecimal.valueOf(100)));
        body.put("recipient", transaction.getPayoutRecipient().getRecipientCode());
        body.put("reference", transaction.getReference());
        payStackClient.initiateTransfer(body);
    }


    private void processInternalTransfer(Transaction transaction) throws CurrencyNotFound {
        Account source = transaction.getSourceAccount();
        Account destination = transaction.getDestinationAccount();
        BigDecimal amount = transaction.getAmount();
        if (source.getCurrency().getCode().equals(destination.getCurrency().getCode())) {
            BigDecimal newSourceBalance = source.getTotalBalance().subtract(amount);
            source.setTotalBalance(newSourceBalance);
            source.setReservedBalance(source.getReservedBalance().subtract(transaction.getAmount()));
        } else {
            String from = source.getCurrency().getCode();
            String to = destination.getCurrency().getCode();

            BigDecimal rate = currencyService.getRate(from, to);
            BigDecimal convertedAmount = amount.multiply(rate);

            BigDecimal newSourceBalance = source.getTotalBalance().subtract(amount);
            source.setTotalBalance(newSourceBalance);
            ledgerService.createEntry(source, transaction, EntryType.DEBIT, source.getCurrency().getCode(), amount, newSourceBalance);
            BigDecimal newDestinationBalance = destination.getTotalBalance().add(convertedAmount);
            destination.setTotalBalance(newDestinationBalance);
            ledgerService.createEntry(destination, transaction, EntryType.CREDIT, destination.getCurrency().getCode(), convertedAmount, newDestinationBalance);
            source.setReservedBalance(source.getReservedBalance().subtract(transaction.getAmount()));
            transaction.setConvertedAmount(convertedAmount);
            transaction.setExchangeRate(rate);
            transaction.setAmount(amount);

        }

        transaction.setType(TransactionType.INTERNAL_TRANSFER);
        transaction.setStatus(TransactionStatus.SUCCESS);
    }
}

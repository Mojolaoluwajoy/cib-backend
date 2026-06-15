package org.app.corporateinternetbanking.transaction.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.domain.repository.AccountRepository;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.transaction.domain.entity.PayoutRecipient;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.domain.repository.PayoutRecipientRepository;
import org.app.corporateinternetbanking.transaction.domain.repository.TransactionRepository;
import org.app.corporateinternetbanking.transaction.dto.TransactionResponse;
import org.app.corporateinternetbanking.transaction.dto.TransferRequest;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.exceptions.*;
import org.app.corporateinternetbanking.transaction.utils.mapper.TransactionMap;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.app.corporateinternetbanking.user.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.app.corporateinternetbanking.transaction.utils.mapper.TransactionMap.mapRequest;
import static org.app.corporateinternetbanking.transaction.utils.mapper.TransactionMap.mapResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final PayoutRecipientRepository payoutRecipientRepository;
    private final UserServiceImpl userService;
    private final OrganizationRepository organizationRepository;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    TransactionRepository transactionRepository;

    @Override
    public TransactionResponse initiateTransaction(TransferRequest request) throws InvalidAmount, AccountDoesNotExist, UserNotFound, UnauthorizedAccess, DuplicateTransaction, InsufficientBalance, InvalidAccount, IsNull {
        Transaction savedTransaction = switch (request.getType()) {
            case INTERNAL_TRANSFER -> validateInternalTransaction(request);

            case EXTERNAL_FUNDING -> validateFunding(request);

            case EXTERNAL_PAYOUT -> validatePayout(request);

            case EXTERNAL_DEPOSIT ->
                    throw new IllegalArgumentException("External Deposit cannot be initiated manually.They are processed automatically by the system.");
        };
        return mapResponse(savedTransaction);
    }

    private Account validateAccountNumber(String accountNumber) throws AccountDoesNotExist {
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountDoesNotExist("Account number does not exist"));

        return account;
    }

    private Transaction validateInternalTransaction(TransferRequest request) throws InvalidAccount, IsNull, AccountDoesNotExist, UserNotFound, InvalidAmount, UnauthorizedAccess, DuplicateTransaction {
        Transaction transaction = validateTransaction(request);

        Account source = validateAccountNumber(request.getSourceAccount());
        Account destination = validateAccountNumber(request.getDestinationAccount());
        if (request.getSourceAccount().equals(request.getDestinationAccount())) {
            throw new InvalidAccount("Cannot transfer to the same account");
        }

        source.setAvailableBalance(source.getTotalBalance().subtract(request.getAmount()));
        source.setReservedBalance(source.getReservedBalance().add(request.getAmount()));

        transaction.setSourceAccount(source);
        transaction.setDestinationAccount(destination);
        transaction.setStatus(TransactionStatus.PENDING_APPROVAL);
        return transactionRepository.save(transaction);

    }

    private Transaction validateFunding(TransferRequest request) throws IsNull, AccountDoesNotExist, UserNotFound, InvalidAmount, UnauthorizedAccess, DuplicateTransaction {
        Transaction transaction = validateTransaction(request);

        Account destination = validateAccountNumber(request.getDestinationAccount());
        transaction.setDestinationAccount(destination);
        transaction.setStatus(TransactionStatus.PENDING);
        return transactionRepository.save(transaction);

    }

    private Transaction validatePayout(TransferRequest request) throws IsNull, AccountDoesNotExist, UserNotFound, InvalidAmount, UnauthorizedAccess, DuplicateTransaction {
        Transaction transaction = validateTransaction(request);

        Account source = validateAccountNumber(request.getSourceAccount());
        transaction.setSourceAccount(source);
        transaction.setStatus(TransactionStatus.PENDING_APPROVAL);
        source.setAvailableBalance(source.getTotalBalance().subtract(request.getAmount()));
        source.setReservedBalance(source.getReservedBalance().add(request.getAmount()));

        PayoutRecipient recipient = payoutRecipientRepository.findById(request.getPayoutRecipientId())
                .orElseThrow(() -> new IsNull("Recipient not found"));
        transaction.setPayoutRecipient(recipient);
        return transactionRepository.save(transaction);


    }

    private Transaction validateTransaction(TransferRequest request) throws DuplicateTransaction, InvalidAmount, UserNotFound, UnauthorizedAccess {
        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());

        if (existingTransaction.isPresent()) {
            throw new DuplicateTransaction("Duplicate transaction detected");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmount("Amount must be greater than zero");
        }

        User maker = userService.getCurrentUser();
        if (!maker.getRole().equals(UserRole.MAKER)) {
            throw new UnauthorizedAccess("the transaction creator must be a MAKER");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmount("depositAmount must not be less than zero");
        }
        Transaction transaction = mapRequest(request);
        transaction.setCreatedBy(maker);
        return transaction;
    }

    public Transaction findByTransactionReference(String transactionReference) throws TransactionDoesNotExist {
        return transactionRepository.findByIdempotencyKey(transactionReference)
                .orElseThrow(() -> new TransactionDoesNotExist("Transaction not found"));
    }

    @Override
    public List<TransactionResponse> viewPendingTransactions() {
        List<Transaction> transactions =
                transactionRepository.findByStatus(TransactionStatus.PENDING_APPROVAL);
        return transactions
                .stream()
                .map(TransactionMap::mapResponse)
                .toList();
    }

    @Override
    public Page<TransactionResponse> getTransactions(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null && !status.isBlank()) {
            try {
                TransactionStatus txnStatus = TransactionStatus.valueOf(status.toUpperCase());
                return transactionRepository.findByStatus(txnStatus, pageable)
                        .map(TransactionMap::mapResponse);
            } catch (IllegalArgumentException e) {

            }
        }
        return transactionRepository.findAll(pageable)
                .map(TransactionMap::mapResponse);
    }

    @Transactional
    @Override
    public void expirePendingTransactions() {
        log.info("Processing pending orders");
        LocalDateTime expirationTime = LocalDateTime.now().minusHours(24);
        List<Transaction> expiredTransactions = transactionRepository.findByStatusAndCreatedAtBefore(TransactionStatus.PENDING, expirationTime);

        for (Transaction transaction : expiredTransactions) {
            transaction.setStatus(TransactionStatus.EXPIRED);
            transaction.setProcessedAt(LocalDateTime.now());
            log.info("Automatically expired due to time-out");
        }

    }

    @Override
    public BigDecimal calculateTransactionVolume() {
        log.info("Calculating 24 hours window transaction volume");
        LocalDateTime end = LocalDateTime.now().minusHours(24);
        LocalDateTime start = LocalDateTime.now();
        List<Transaction> expiredTransactions = transactionRepository.findByCreatedAtBetweenAndStatus(start, end, TransactionStatus.SUCCESS);
        BigDecimal transactionVolume = BigDecimal.ZERO;
        for (Transaction transaction : expiredTransactions) {
            transactionVolume = transactionVolume.add(transaction.getAmount());
        }

        return transactionVolume;
    }

    @Override
    public void markSuccess(String reference) throws TransactionDoesNotExist {
        Transaction transaction = findByTransactionReference(reference);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);
    }

    @Override
    public void markFailed(String reference) throws TransactionDoesNotExist {
        Transaction transaction = findByTransactionReference(reference);
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
    }

    @Override
    public Page<TransactionResponse> getTransactionsByOrganization(
            Long orgId, int page, int size) throws OrganizationDoesNotExist {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new OrganizationDoesNotExist("Organization not found"));

        Pageable pageable = PageRequest.of(page, size);
        // You'll need to add this query to your TransactionRepository
        Page<Transaction> transactions = transactionRepository
                .findAllBySourceAccount_Organization(org, pageable);

        return transactions.map(TransactionMap::mapResponse);
    }


}

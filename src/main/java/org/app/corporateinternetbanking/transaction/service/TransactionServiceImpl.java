package org.app.corporateinternetbanking.transaction.service;

import static org.app.corporateinternetbanking.transaction.utils.mapper.TransactionMap.mapRequest;
import static org.app.corporateinternetbanking.transaction.utils.mapper.TransactionMap.mapResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.domain.repository.AccountRepository;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.commons.locking.RowLocks;
import org.app.corporateinternetbanking.notification.service.NotificationService;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.security.TenantAccess;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final PayoutRecipientRepository payoutRecipientRepository;
    private final UserServiceImpl userService;
    private final OrganizationRepository organizationRepository;
    private final NotificationService notificationService;
    private final RowLocks rowLocks;

    // Transfers/payouts nobody approves within this window expire and release their reserved funds.
    @Value("${transaction.approval.expiry-hours}")
    private long approvalExpiryHours;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TransactionRepository transactionRepository;

    // One database transaction per initiation: the source account is row-locked while funds are reserved,
    // so concurrent requests against the same account cannot both pass the balance check.
    @Transactional(rollbackFor = Exception.class)
    @Override
    public TransactionResponse initiateTransaction(TransferRequest request)
            throws InvalidAmount, AccountDoesNotExist, UserNotFound, UnauthorizedAccess, DuplicateTransaction,
                    InsufficientBalance, InvalidAccount, IsNull {
        Transaction savedTransaction =
                switch (request.getType()) {
                    case INTERNAL_TRANSFER -> validateInternalTransaction(request);

                    case EXTERNAL_FUNDING -> validateFunding(request);

                    case EXTERNAL_PAYOUT -> validatePayout(request);

                    case EXTERNAL_DEPOSIT -> throw new IllegalArgumentException(
                            "External Deposit cannot be initiated manually.They are processed automatically by the system.");
                };
        return mapResponse(savedTransaction);
    }

    private Account validateAccountNumber(String accountNumber) throws AccountDoesNotExist {
        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountDoesNotExist("Account number does not exist"));

        return account;
    }

    private Transaction validateInternalTransaction(TransferRequest request)
            throws InvalidAccount, IsNull, AccountDoesNotExist, UserNotFound, InvalidAmount, UnauthorizedAccess,
                    DuplicateTransaction, InsufficientBalance {
        Transaction transaction = validateTransaction(request);

        Account source = validateAccountNumber(request.getSourceAccount());
        Account destination = validateAccountNumber(request.getDestinationAccount());
        if (request.getSourceAccount().equals(request.getDestinationAccount())) {
            throw new InvalidAccount("Cannot transfer to the same account");
        }
        // Money can only leave the maker's own organization; the destination may be any account in the bank.
        TenantAccess.assertMemberOf(transaction.getCreatedBy(), source.getOrganization());

        rowLocks.lockAccounts(source);
        reserveFunds(source, request.getAmount());

        transaction.setSourceAccount(source);
        transaction.setDestinationAccount(destination);
        transaction.setStatus(TransactionStatus.PENDING_APPROVAL);
        return transactionRepository.save(transaction);
    }

    private Transaction validateFunding(TransferRequest request)
            throws IsNull, AccountDoesNotExist, UserNotFound, InvalidAmount, UnauthorizedAccess, DuplicateTransaction {
        Transaction transaction = validateTransaction(request);

        Account destination = validateAccountNumber(request.getDestinationAccount());
        TenantAccess.assertMemberOf(transaction.getCreatedBy(), destination.getOrganization());
        transaction.setDestinationAccount(destination);
        transaction.setStatus(TransactionStatus.PENDING);
        return transactionRepository.save(transaction);
    }

    private Transaction validatePayout(TransferRequest request)
            throws IsNull, AccountDoesNotExist, UserNotFound, InvalidAmount, UnauthorizedAccess, DuplicateTransaction,
                    InsufficientBalance {
        Transaction transaction = validateTransaction(request);
        if (request.getPayoutRecipientId() == null) {
            throw new IsNull("payoutRecipientId is required for a payout");
        }

        Account source = validateAccountNumber(request.getSourceAccount());
        TenantAccess.assertMemberOf(transaction.getCreatedBy(), source.getOrganization());
        transaction.setSourceAccount(source);
        transaction.setStatus(TransactionStatus.PENDING_APPROVAL);
        rowLocks.lockAccounts(source);
        reserveFunds(source, request.getAmount());

        PayoutRecipient recipient = payoutRecipientRepository
                .findById(request.getPayoutRecipientId())
                .orElseThrow(() -> new IsNull("Recipient not found"));
        transaction.setPayoutRecipient(recipient);
        return transactionRepository.save(transaction);
    }

    // Moves funds from available to reserved; they leave totalBalance only once the transaction settles.
    private void reserveFunds(Account source, BigDecimal amount) throws InsufficientBalance {
        if (source.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalance("Insufficient available balance");
        }
        source.setAvailableBalance(source.getAvailableBalance().subtract(amount));
        source.setReservedBalance(source.getReservedBalance().add(amount));
    }

    private Transaction validateTransaction(TransferRequest request)
            throws DuplicateTransaction, InvalidAmount, UserNotFound, UnauthorizedAccess, IsNull {
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new IsNull("idempotencyKey is required");
        }
        if (request.getAmount() == null) {
            throw new IsNull("amount is required");
        }
        Optional<Transaction> existingTransaction =
                transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());

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
        return transactionRepository
                .findByReference(transactionReference)
                .orElseThrow(() -> new TransactionDoesNotExist("Transaction not found"));
    }

    @Override
    public List<TransactionResponse> viewPendingTransactions() throws UserNotFound {
        User currentUser = userService.getCurrentUser();
        List<Transaction> transactions = TenantAccess.isSuperAdmin(currentUser)
                ? transactionRepository.findByStatus(TransactionStatus.PENDING_APPROVAL)
                : transactionRepository.findAllForOrganizationByStatus(
                        currentUser.getOrganization(), TransactionStatus.PENDING_APPROVAL);
        return transactions.stream().map(TransactionMap::mapResponse).toList();
    }

    @Override
    public Page<TransactionResponse> getTransactions(int page, int size, String status) throws UserNotFound {
        User currentUser = userService.getCurrentUser();
        boolean superAdmin = TenantAccess.isSuperAdmin(currentUser);
        Organization organization = currentUser.getOrganization();
        Pageable pageable = PageRequest.of(page, size);
        if (status != null && !status.isBlank()) {
            try {
                TransactionStatus txnStatus = TransactionStatus.valueOf(status.toUpperCase());
                Page<Transaction> transactions = superAdmin
                        ? transactionRepository.findByStatus(txnStatus, pageable)
                        : transactionRepository.findAllForOrganizationByStatus(organization, txnStatus, pageable);
                return transactions.map(TransactionMap::mapResponse);
            } catch (IllegalArgumentException e) {
                // Unknown status: fall through and return the unfiltered list.
            }
        }
        Page<Transaction> transactions = superAdmin
                ? transactionRepository.findAll(pageable)
                : transactionRepository.findAllForOrganization(organization, pageable);
        return transactions.map(TransactionMap::mapResponse);
    }

    @Transactional
    @Override
    public void expirePendingTransactions() {
        log.info("Processing pending orders");
        LocalDateTime expirationTime = LocalDateTime.now().minusHours(24);
        List<Transaction> expiredTransactions =
                transactionRepository.findByStatusAndCreatedAtBefore(TransactionStatus.PENDING, expirationTime);

        for (Transaction transaction : expiredTransactions) {
            transaction.setStatus(TransactionStatus.EXPIRED);
            transaction.setProcessedAt(LocalDateTime.now());
            log.info("Automatically expired due to time-out");
        }
    }

    @Transactional
    @Override
    public void expireUnapprovedTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(approvalExpiryHours);
        List<Transaction> unapproved =
                transactionRepository.findByStatusAndCreatedAtBefore(TransactionStatus.PENDING_APPROVAL, cutoff);

        for (Transaction transaction : unapproved) {
            rowLocks.lockTransaction(transaction);
            // An approver may have processed it while this job was running.
            if (transaction.getStatus() != TransactionStatus.PENDING_APPROVAL) {
                continue;
            }
            // Funds were moved from available to reserved at initiation; give them back.
            Account source = transaction.getSourceAccount();
            rowLocks.lockAccounts(source);
            BigDecimal amount = transaction.getAmount();
            source.setAvailableBalance(source.getAvailableBalance().add(amount));
            source.setReservedBalance(source.getReservedBalance().subtract(amount));
            transaction.setStatus(TransactionStatus.EXPIRED);
            transaction.setProcessedAt(LocalDateTime.now());
            log.info(
                    "Expired unapproved transaction {} after {} hours",
                    transaction.getReference(),
                    approvalExpiryHours);

            if (transaction.getCreatedBy() != null) {
                notificationService.createNotification(
                        transaction.getCreatedBy().getEmail(),
                        "Your " + transaction.getType() + " of " + amount + " (reference "
                                + transaction.getReference() + ") was not approved within " + approvalExpiryHours
                                + " hours and has expired. The reserved funds have been released.");
            }
        }
    }

    @Override
    public BigDecimal calculateTransactionVolume() {
        log.info("Calculating 24 hours window transaction volume");
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(24);
        List<Transaction> successfulTransactions =
                transactionRepository.findByCreatedAtBetweenAndStatus(start, end, TransactionStatus.SUCCESS);
        BigDecimal transactionVolume = BigDecimal.ZERO;
        for (Transaction transaction : successfulTransactions) {
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
    public Page<TransactionResponse> getTransactionsByOrganization(Long orgId, int page, int size)
            throws OrganizationDoesNotExist, UserNotFound, UnauthorizedAccess {
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new OrganizationDoesNotExist("Organization not found"));
        TenantAccess.assertCanAccess(userService.getCurrentUser(), org);

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionRepository.findAllForOrganization(org, pageable);

        return transactions.map(TransactionMap::mapResponse);
    }
}

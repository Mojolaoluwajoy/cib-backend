package org.app.corporateinternetbanking.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.domain.repository.AccountRepository;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.service.AccountService;
import org.app.corporateinternetbanking.commons.locking.RowLocks;
import org.app.corporateinternetbanking.integration.paystack.PayStackClient;
import org.app.corporateinternetbanking.ledger.enums.EntryType;
import org.app.corporateinternetbanking.ledger.service.LedgerService;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.domain.repository.TransactionRepository;
import org.app.corporateinternetbanking.transaction.dto.PaystackWebhookRequest;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.transaction.exceptions.InvalidSignature;
import org.app.corporateinternetbanking.transaction.exceptions.NotApproved;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookHandler {

    private final LedgerService ledgerService;
    private final AccountRepository accountRepository;
    private final OrganizationRepository organizationRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final ObjectMapper objectMapper;
    private final RowLocks rowLocks;

    @Value("${paystack.secret.key}")
    private String secretKey;

    // Paystack retries any non-2xx response, so events we have already processed (or don't
    // care about) return normally. Only "not ready yet" situations throw, to get a retry.
    @Transactional(rollbackFor = Exception.class)
    public String handleWebhook(String payload, String signature)
            throws InvalidSignature, AccountDoesNotExist, NotApproved {
        verifySignature(payload, signature);

        PaystackWebhookRequest webhookRequest;
        try {
            webhookRequest = objectMapper.readValue(payload, PaystackWebhookRequest.class);
        } catch (JsonProcessingException e) {
            throw new InvalidSignature("Malformed webhook payload");
        }
        String event = webhookRequest.getEvent();
        PaystackWebhookRequest.Data data = webhookRequest.getData();
        if (data == null || data.getReference() == null) {
            log.warn("Ignoring Paystack event {} without a reference", event);
            return null;
        }
        String reference = data.getReference();
        log.info("Paystack webhook: event={} reference={}", event, reference);

        Optional<Transaction> existing = transactionRepository.findByReference(reference);

        switch (event) {
            case "charge.success" -> {
                if (existing.isPresent()) {
                    settleFunding(existing.get(), data);
                } else {
                    recordDvaDeposit(data);
                }
            }
            case "transfer.success" -> settlePayout(existing, reference, true, TransactionStatus.SUCCESS);
            case "transfer.failed" -> settlePayout(existing, reference, false, TransactionStatus.FAILED);
            case "transfer.reversed" -> settlePayout(existing, reference, false, TransactionStatus.REVERSED);
            default -> log.info("Ignoring unhandled Paystack event {}", event);
        }
        return reference;
    }

    private void verifySignature(String payload, String signature) throws InvalidSignature {
        if (signature == null || signature.isBlank()) {
            throw new InvalidSignature("Missing x-paystack-signature header");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
                throw new InvalidSignature("Invalid Paystack signature");
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to verify Paystack signature", e);
        }
    }

    // Card/bank funding started via /external/transaction/fund: reference is our own transaction reference.
    private void settleFunding(Transaction txn, PaystackWebhookRequest.Data data) throws AccountDoesNotExist {
        if (txn.getType() != TransactionType.EXTERNAL_FUNDING) {
            log.info(
                    "charge.success for {} transaction {} already recorded, ignoring",
                    txn.getType(),
                    txn.getReference());
            return;
        }
        // Lock before checking the status so a retried webhook arriving concurrently waits, then sees SUCCESS.
        rowLocks.lockTransaction(txn);
        if (txn.getStatus() != TransactionStatus.PENDING) {
            log.info("Funding {} already {}, ignoring duplicate webhook", txn.getReference(), txn.getStatus());
            return;
        }
        if (data.getAmount() == null || data.getAmount() != PayStackClient.toKobo(txn.getAmount())) {
            log.warn(
                    "Funding {} amount mismatch: expected {} kobo, Paystack reported {}",
                    txn.getReference(),
                    PayStackClient.toKobo(txn.getAmount()),
                    data.getAmount());
            txn.setStatus(TransactionStatus.FAILED);
            txn.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(txn);
            return;
        }

        Account destination = txn.getDestinationAccount();
        accountService.credit(destination.getId(), txn.getAmount());
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setProcessedAt(LocalDateTime.now());
        transactionRepository.save(txn);
        ledgerService.createEntry(
                destination,
                txn,
                EntryType.CREDIT,
                destination.getCurrency().getCode(),
                txn.getAmount(),
                destination.getTotalBalance());
    }

    // Transfer into an organization's dedicated virtual account: reference is generated by Paystack.
    private void recordDvaDeposit(PaystackWebhookRequest.Data data) throws AccountDoesNotExist {
        String customerCode = data.getCustomerCode();
        Optional<Organization> organization = customerCode == null
                ? Optional.empty()
                : organizationRepository.findByPaystackCustomerCode(customerCode);
        if (organization.isEmpty()) {
            log.warn(
                    "charge.success {} matches no transaction or organization (customer {}), ignoring",
                    data.getReference(),
                    customerCode);
            return;
        }

        Account receivingAccount = accountRepository
                .findFirstByOrganizationAndCurrencyCodeOrderByCreatedAtAsc(organization.get(), "NGN")
                .orElseThrow(() -> new AccountDoesNotExist(
                        "No NGN account found for org " + organization.get().getId()));

        // Two deliveries of the same deposit can arrive together: serialize them on the account,
        // then re-check under the lock whether the first one already recorded it.
        rowLocks.lockAccounts(receivingAccount);
        if (transactionRepository.findByReference(data.getReference()).isPresent()) {
            log.info("DVA deposit {} already recorded, ignoring duplicate webhook", data.getReference());
            return;
        }

        BigDecimal amountInNaira = BigDecimal.valueOf(data.getAmount()).movePointLeft(2);
        accountService.credit(receivingAccount.getId(), amountInNaira);

        Transaction transaction = new Transaction();
        transaction.setDestinationAccount(receivingAccount);
        transaction.setAmount(amountInNaira);
        transaction.setReference(data.getReference());
        transaction.setType(TransactionType.EXTERNAL_DEPOSIT);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setProcessedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        ledgerService.createEntry(
                receivingAccount,
                transaction,
                EntryType.CREDIT,
                receivingAccount.getCurrency().getCode(),
                amountInNaira,
                receivingAccount.getTotalBalance());
    }

    private void settlePayout(
            Optional<Transaction> existing, String reference, boolean succeeded, TransactionStatus outcome)
            throws NotApproved {
        if (existing.isEmpty() || existing.get().getType() != TransactionType.EXTERNAL_PAYOUT) {
            log.warn("Transfer event for unknown payout {}, ignoring", reference);
            return;
        }
        Transaction txn = existing.get();
        rowLocks.lockTransaction(txn);
        if (txn.getStatus() == TransactionStatus.PENDING_APPROVAL) {
            // Approval is still being committed; throw so Paystack retries shortly.
            throw new NotApproved("Payout " + reference + " is not yet approved");
        }
        if (txn.getStatus() != TransactionStatus.APPROVED) {
            log.info("Payout {} already {}, ignoring duplicate webhook", reference, txn.getStatus());
            return;
        }

        Account source = txn.getSourceAccount();
        rowLocks.lockAccounts(source);
        BigDecimal amount = txn.getAmount();
        source.setReservedBalance(source.getReservedBalance().subtract(amount));
        if (succeeded) {
            source.setTotalBalance(source.getTotalBalance().subtract(amount));
            ledgerService.createEntry(
                    source, txn, EntryType.DEBIT, source.getCurrency().getCode(), amount, source.getTotalBalance());
        } else {
            // Money never left: release the reservation back to available balance.
            source.setAvailableBalance(source.getAvailableBalance().add(amount));
        }
        txn.setStatus(outcome);
        txn.setProcessedAt(LocalDateTime.now());
        accountRepository.save(source);
        transactionRepository.save(txn);
    }
}

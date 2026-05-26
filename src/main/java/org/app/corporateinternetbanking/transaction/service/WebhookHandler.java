package org.app.corporateinternetbanking.transaction.service;

import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.domain.repository.AccountRepository;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.service.AccountService;
import org.app.corporateinternetbanking.ledger.enums.EntryType;
import org.app.corporateinternetbanking.ledger.service.LedgerService;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.domain.repository.TransactionRepository;
import org.app.corporateinternetbanking.transaction.dto.PaystackWebhookRequest;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.transaction.exceptions.InsufficientBalance;
import org.app.corporateinternetbanking.transaction.exceptions.NotApproved;
import org.app.corporateinternetbanking.transaction.exceptions.TransactionDoesNotExist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WebhookHandler {

    private final LedgerService ledgerService;
    private final AccountRepository accountRepository;
    private final OrganizationRepository organizationRepository;
    @Autowired
    TransactionRepository transactionRepository;
    @Value("${paystack.secret.key}")
    private String secretKey;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private AccountService accountService;

    public String handleWebhook(PaystackWebhookRequest webhookRequest) throws AccountDoesNotExist, TransactionDoesNotExist, InsufficientBalance, NotApproved, OrganizationDoesNotExist {
        String reference = webhookRequest.getData().getReference();
        String event = webhookRequest.getEvent();

        if ("charge.success".equals(event)) {
            String customerCode = webhookRequest.getData().getCustomerCode();
            if (customerCode != null) {
                Organization organization = organizationRepository.findByPaystackCustomerCode(customerCode)
                        .orElseThrow(() -> new OrganizationDoesNotExist("Organization not found for customer code" + customerCode));

                Account receivingAccount = accountRepository.findFirstByOrganizationAndCurrencyCodeOrderByCreatedAtAsc
                                (organization, "NGN")
                        .orElseThrow(() -> new AccountDoesNotExist("No NGN account found for org"));

                BigDecimal amountInNaira = BigDecimal.valueOf(webhookRequest.getData().getAmount()).divide(BigDecimal.valueOf(100));

                accountService.credit(receivingAccount.getId(), amountInNaira);

                Transaction transaction = new Transaction();

                transaction.setDestinationAccount(receivingAccount);
                transaction.setAmount(amountInNaira);
                transaction.setReference(reference);
                transaction.setType(TransactionType.EXTERNAL_DEPOSIT);
                transaction.setStatus(TransactionStatus.SUCCESS);
                transactionRepository.save(transaction);

                ledgerService.createEntry(receivingAccount, transaction, EntryType.CREDIT, receivingAccount.getCurrency().getCode(),
                        amountInNaira, receivingAccount.getTotalBalance());

                return webhookRequest.getData().getReference();

            }
        }
        Transaction txn = transactionService.findByTransactionReference(reference);
        Account source = txn.getSourceAccount();
        Account destination = txn.getDestinationAccount();
        BigDecimal amount = txn.getAmount();

        if (txn.getType().equals(TransactionType.EXTERNAL_PAYOUT)
                && !txn.getStatus().equals(TransactionStatus.APPROVED)) {
            throw new NotApproved("This transaction needs to have been approved before paystack can do it's thing");
        }
        if ("charge.success".equals(event)) {
            accountService.credit(txn.getDestinationAccount().getId(), txn.getAmount());
            transactionService.markSuccess(reference);
            BigDecimal newDestinationBalance = destination.getTotalBalance();
            ledgerService.createEntry(destination, txn, EntryType.CREDIT, destination.getCurrency().getCode(), amount, newDestinationBalance);
        } else if ("transfer.success".equals(event)) {
            BigDecimal newSourceBalance = source.getTotalBalance().subtract(amount);
            source.setTotalBalance(newSourceBalance);
            source.setReservedBalance(source.getReservedBalance().subtract(amount));
            transactionService.markSuccess(reference);

            ledgerService.createEntry(source, txn, EntryType.DEBIT, source.getCurrency().getCode(), amount, newSourceBalance);
            transactionRepository.save(txn);
            accountRepository.save(source);
        } else if ("transfer.failed".equals(webhookRequest.getEvent())) {
            source.setAvailableBalance(source.getAvailableBalance().add(amount));
            source.setReservedBalance(source.getReservedBalance().subtract(amount));
            transactionService.markFailed(reference);
            txn.setStatus(TransactionStatus.REVERSED);
            transactionRepository.save(txn);
            accountRepository.save(source);
        }
        return reference;
    }

}

package org.app.corporateinternetbanking.commons.locking;

import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// Pessimistic row locks for anything that changes money. Each method takes a SELECT ... FOR UPDATE
// lock on the row and then reloads the entity, so the caller works with current values that no
// other request can change until the surrounding database transaction commits.
@Component
@RequiredArgsConstructor
public class RowLocks {

    private final EntityManager entityManager;

    // Accounts are always locked in id order, so two transfers between the same pair of accounts
    // in opposite directions cannot deadlock.
    public void lockAccounts(Account... accounts) {
        requireTransaction();
        Arrays.stream(accounts)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(Account::getId))
                .forEach(account -> {
                    entityManager
                            .createNativeQuery("select id from account where id = :id for update")
                            .setParameter("id", account.getId())
                            .getSingleResult();
                    entityManager.refresh(account);
                });
    }

    public void lockTransaction(Transaction transaction) {
        requireTransaction();
        entityManager
                .createNativeQuery("select id from \"transaction\" where id = :id for update")
                .setParameter("id", transaction.getId())
                .getSingleResult();
        entityManager.refresh(transaction);
    }

    private static void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Row locks must be taken inside a database transaction");
        }
    }
}

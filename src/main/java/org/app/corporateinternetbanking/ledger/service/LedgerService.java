package org.app.corporateinternetbanking.ledger.service;

import java.math.BigDecimal;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.ledger.domain.entity.LedgerEntry;
import org.app.corporateinternetbanking.ledger.enums.EntryType;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;

public interface LedgerService {
    LedgerEntry createEntry(
            Account account,
            Transaction transaction,
            EntryType type,
            String currency,
            BigDecimal amount,
            BigDecimal balanceAfter);
}

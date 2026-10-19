package org.app.corporateinternetbanking.ledger.service;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.ledger.domain.entity.LedgerEntry;
import org.app.corporateinternetbanking.ledger.domain.repository.LedgerRepository;
import org.app.corporateinternetbanking.ledger.enums.EntryType;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LedgerServiceImpl implements LedgerService {
    private final LedgerRepository ledgerRepository;

    @Override
    public LedgerEntry createEntry(
            Account account,
            Transaction transaction,
            EntryType type,
            String currency,
            BigDecimal amount,
            BigDecimal balanceAfter) {
        LedgerEntry entry = new LedgerEntry();
        entry.setAccount(account);
        entry.setTransaction(transaction);
        entry.setType(type);
        entry.setCurrency(currency);
        entry.setAmount(amount);
        entry.setBalanceAfter(balanceAfter);
        return ledgerRepository.save(entry);
    }
}

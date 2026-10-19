package org.app.corporateinternetbanking.audit;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.domain.repository.AccountRepository;
import org.app.corporateinternetbanking.ledger.domain.entity.LedgerEntry;
import org.app.corporateinternetbanking.ledger.domain.repository.LedgerRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;

    public void auditBalances() {

        List<Account> accounts = accountRepository.findAll();

        for (Account account : accounts) {

            LedgerEntry lastLedger = ledgerRepository.findByAccountIdOrderByCreatedAtDesc(account.getId());
            if (lastLedger == null) continue;

            BigDecimal storedBalance = account.getTotalBalance();
            BigDecimal computedBalance = lastLedger.getBalanceAfter();

            if (storedBalance.compareTo(computedBalance) != 0) {
                account.setFlagged(true);
                log.info(
                        "MISMATCH DETECTED FOR ACCOUNT: {},STORED BALANCE: {},COMPUTED BALANCE: {}",
                        account.getId(),
                        storedBalance,
                        computedBalance);
            } else {
                account.setFlagged(false);
            }
        }
    }
}

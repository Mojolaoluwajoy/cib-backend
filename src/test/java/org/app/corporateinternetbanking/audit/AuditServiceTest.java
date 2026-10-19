package org.app.corporateinternetbanking.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.domain.repository.AccountRepository;
import org.app.corporateinternetbanking.ledger.domain.entity.LedgerEntry;
import org.app.corporateinternetbanking.ledger.domain.repository.LedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    LedgerRepository ledgerRepository;

    @InjectMocks
    AuditService auditService;

    @Test
    void flagsMismatchedAccountsClearsMatchingOnesAndPersistsTheResult() {
        Account mismatched = account(1L, "500", false);
        Account matching = account(2L, "300", true);
        Account noLedger = account(3L, "100", false);
        List<Account> accounts = List.of(mismatched, matching, noLedger);

        when(accountRepository.findAll()).thenReturn(accounts);
        when(ledgerRepository.findFirstByAccountIdOrderByIdDesc(1L)).thenReturn(Optional.of(ledger("450")));
        when(ledgerRepository.findFirstByAccountIdOrderByIdDesc(2L)).thenReturn(Optional.of(ledger("300.00")));
        when(ledgerRepository.findFirstByAccountIdOrderByIdDesc(3L)).thenReturn(Optional.empty());

        auditService.auditBalances();

        assertThat(mismatched.isFlagged()).isTrue();
        assertThat(matching.isFlagged()).isFalse();
        assertThat(noLedger.isFlagged()).isFalse();
        // The old code changed the flags but never saved them.
        verify(accountRepository).saveAll(accounts);
    }

    @Test
    void newAccountsStartWithZeroBalance() {
        Account account = new Account();

        assertThat(account.getTotalBalance()).isEqualByComparingTo("0");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("0");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0");
    }

    private static Account account(Long id, String totalBalance, boolean flagged) {
        Account account = new Account();
        account.setId(id);
        account.setTotalBalance(new BigDecimal(totalBalance));
        account.setFlagged(flagged);
        return account;
    }

    private static LedgerEntry ledger(String balanceAfter) {
        LedgerEntry entry = new LedgerEntry();
        entry.setBalanceAfter(new BigDecimal(balanceAfter));
        return entry;
    }
}

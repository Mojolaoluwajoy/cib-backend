package org.app.corporateinternetbanking.ledger.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.ledger.enums.EntryType;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;

@Setter
@Getter
public class LedgerCreationRequest {
    private Account account;
    private Transaction transaction;
    private EntryType entryType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
}

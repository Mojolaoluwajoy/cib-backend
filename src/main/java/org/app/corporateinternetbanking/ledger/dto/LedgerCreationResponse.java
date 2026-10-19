package org.app.corporateinternetbanking.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.ledger.enums.EntryType;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;

@Setter
@Getter
public class LedgerCreationResponse {
    private Long id;
    private Account account;
    private Transaction transaction;
    private EntryType type;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;
}

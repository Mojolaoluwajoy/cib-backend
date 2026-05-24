package org.app.corporateinternetbanking.transaction.dto;

import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;

import java.math.BigDecimal;

@Setter
@Getter
public class FundRequest {

    private String accountNumber;
    private BigDecimal amount;
    private String email;
    private TransactionType type;
    private String idempotencyKey;
}

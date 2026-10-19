package org.app.corporateinternetbanking.transaction.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;

@Setter
@Getter
public class PayoutRequest {
    private String sourceAccount;
    private BigDecimal amount;
    private String accountNumber;
    private String bankCode;
    private String bankName;
    private String accountName;
    private TransactionType type;
    private String idempotencyKey;
}

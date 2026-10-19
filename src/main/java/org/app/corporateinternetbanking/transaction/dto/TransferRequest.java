package org.app.corporateinternetbanking.transaction.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;

@Setter
@Getter
public class TransferRequest {
    private String sourceAccount;
    private String destinationAccount;
    private BigDecimal amount;
    private TransactionType type;
    private String idempotencyKey;
    private Long payoutRecipientId;
}

package org.app.corporateinternetbanking.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;

@Setter
@Getter
public class TransactionApprovalRequest {
    @Positive(message = "is required")
    private long transactionId;

    @NotNull
    private TransactionStatus status;
}

package org.app.corporateinternetbanking.transaction.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;

@Setter
@Getter
public class TransferRequest {
    private String sourceAccount;
    private String destinationAccount;

    @NotNull
    @Positive
    @Digits(integer = 15, fraction = 2, message = "must have at most 2 decimal places")
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    @NotBlank
    private String idempotencyKey;

    private Long payoutRecipientId;
}

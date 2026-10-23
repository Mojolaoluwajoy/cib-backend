package org.app.corporateinternetbanking.transaction.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;

@Setter
@Getter
public class PayoutRequest {
    @NotBlank
    private String sourceAccount;

    @NotNull
    @Positive
    @Digits(integer = 15, fraction = 2, message = "must have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "\\d{10}", message = "must be a 10-digit NUBAN account number")
    private String accountNumber;

    @NotBlank
    private String bankCode;

    private String bankName;

    @NotBlank
    private String accountName;

    private TransactionType type;

    @NotBlank
    private String idempotencyKey;
}

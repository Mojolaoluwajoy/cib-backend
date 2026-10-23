package org.app.corporateinternetbanking.currency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.currency.enums.CurrencyStatus;

@Setter
@Getter
public class CurrencyRequest {
    @NotBlank
    private String code;

    @NotNull
    private CurrencyStatus status;
}

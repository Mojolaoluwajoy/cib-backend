package org.app.corporateinternetbanking.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.account.enums.AccountType;

@Setter
@Getter
public class AccountRequest {
    @NotNull
    private AccountType type;

    @NotBlank
    private String currencyCode;
}

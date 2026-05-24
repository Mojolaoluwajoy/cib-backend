package org.app.corporateinternetbanking.account.dto;

import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.account.enums.AccountType;

@Setter
@Getter
public class AccountRequest {
    private AccountType type;
    private String currencyCode;
}

package org.app.corporateinternetbanking.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class AccountNameResponse {
    private String accountNumber;
    private String bankCode;
    private String accountName;
}

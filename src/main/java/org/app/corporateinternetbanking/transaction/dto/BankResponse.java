package org.app.corporateinternetbanking.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class BankResponse {
    private String name;
    private String code;
}

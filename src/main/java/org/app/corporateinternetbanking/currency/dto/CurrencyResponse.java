package org.app.corporateinternetbanking.currency.dto;

import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.currency.enums.CurrencyStatus;

@Setter
@Getter
public class CurrencyResponse {
    private String code;
    private String name;
    private String symbol;
    private CurrencyStatus status;
}

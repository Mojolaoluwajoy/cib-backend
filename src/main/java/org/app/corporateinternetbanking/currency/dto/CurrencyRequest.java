package org.app.corporateinternetbanking.currency.dto;

import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.currency.enums.CurrencyStatus;

@Setter
@Getter
public class CurrencyRequest {
    private String code;
    private CurrencyStatus status;
}

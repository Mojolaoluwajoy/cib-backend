package org.app.corporateinternetbanking.currency.utils;

import org.app.corporateinternetbanking.currency.domain.entity.Currency;
import org.app.corporateinternetbanking.currency.dto.CurrencyRequest;
import org.app.corporateinternetbanking.currency.dto.CurrencyResponse;

public class CurrencyMap {

    public static Currency mapCurrencyRequest(CurrencyRequest currencyRequest) {
        Currency currency = new Currency();
        currency.setCode(currencyRequest.getCode());
        currency.setStatus(currencyRequest.getStatus());
        return currency;
    }

    public static CurrencyResponse mapCurrencyResponse(Currency currency) {
        CurrencyResponse currencyResponse = new CurrencyResponse();

        currencyResponse.setCode(currency.getCode());
        currencyResponse.setName(currency.getName());
        currencyResponse.setSymbol(currency.getSymbol());
        currencyResponse.setStatus(currency.getStatus());
        return currencyResponse;
    }
}

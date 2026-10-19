package org.app.corporateinternetbanking.currency.service;

import java.util.List;
import org.app.corporateinternetbanking.currency.dto.CurrencyRequest;
import org.app.corporateinternetbanking.currency.dto.CurrencyResponse;
import org.app.corporateinternetbanking.currency.enums.CurrencyStatus;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;

public interface CurrencyService {

    CurrencyResponse changeCurrencyStatus(CurrencyRequest currencyRequest) throws CurrencyNotFound;

    //  List<CurrencyResponse> viewActiveCurrencies();

    List<CurrencyResponse> viewAll();

    List<CurrencyResponse> viewByStatus(CurrencyStatus status);
}

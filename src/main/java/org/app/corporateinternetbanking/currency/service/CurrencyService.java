package org.app.corporateinternetbanking.currency.service;

import org.app.corporateinternetbanking.currency.dto.CurrencyRequest;
import org.app.corporateinternetbanking.currency.dto.CurrencyResponse;
import org.app.corporateinternetbanking.currency.enums.CurrencyStatus;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;

import java.util.List;

public interface CurrencyService {

    CurrencyResponse changeCurrencyStatus(CurrencyRequest currencyRequest) throws CurrencyNotFound;

    //  List<CurrencyResponse> viewActiveCurrencies();


    List<CurrencyResponse> viewAll();

    List<CurrencyResponse> viewByStatus(CurrencyStatus status);

}

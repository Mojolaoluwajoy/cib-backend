package org.app.corporateinternetbanking.currency.service;

import lombok.AllArgsConstructor;
import org.app.corporateinternetbanking.currency.domain.entity.Currency;
import org.app.corporateinternetbanking.currency.domain.repository.CurrencyRepository;
import org.app.corporateinternetbanking.currency.dto.CurrencyRequest;
import org.app.corporateinternetbanking.currency.dto.CurrencyResponse;
import org.app.corporateinternetbanking.currency.enums.CurrencyStatus;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.currency.utils.CurrencyMap;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {
    private final CurrencyRepository repository;


    @Override
    public CurrencyResponse changeCurrencyStatus(CurrencyRequest currencyRequest) throws CurrencyNotFound {
        Currency currency = repository.findByCode(currencyRequest.getCode())
                .orElseThrow(() -> new CurrencyNotFound("This currency does not exist"));
        if (currencyRequest.getStatus().equals(CurrencyStatus.ACTIVE)) {
            currency.setStatus(CurrencyStatus.ACTIVE);
            repository.save(currency);
        }
        if (currencyRequest.getStatus().equals(CurrencyStatus.INACTIVE)) {
            currency.setStatus(CurrencyStatus.INACTIVE);
            repository.save(currency);
        }
        return CurrencyMap.mapCurrencyResponse(currency);
    }


    public List<CurrencyResponse> viewAll() {
        return repository.findAll()
                .stream()
                .map(CurrencyMap::mapCurrencyResponse) // use your existing currency mapper
                .collect(Collectors.toList());
    }

    public List<CurrencyResponse> viewByStatus(CurrencyStatus status) {
        return repository.findAllByStatus(status)
                .stream()
                .map(CurrencyMap::mapCurrencyResponse)
                .collect(Collectors.toList());
    }
}

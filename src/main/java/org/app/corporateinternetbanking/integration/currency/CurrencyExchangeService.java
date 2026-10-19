package org.app.corporateinternetbanking.integration.currency;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.integration.currency.dto.CurrencyIntegrationResponse;
import org.app.corporateinternetbanking.integration.currency.mapper.CurrencyMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrencyExchangeService {

    private final CurrencyClient currencyClient;

    public BigDecimal getRate(String base, String target) throws CurrencyNotFound {
        CurrencyIntegrationResponse response = currencyClient.getRates(base);
        return CurrencyMapper.mapToRate(response, target);
    }
}

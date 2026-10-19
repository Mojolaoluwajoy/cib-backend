package org.app.corporateinternetbanking.integration.currency.mapper;

import java.math.BigDecimal;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.integration.currency.dto.CurrencyIntegrationResponse;

public class CurrencyMapper {

    public static BigDecimal mapToRate(CurrencyIntegrationResponse response, String target) throws CurrencyNotFound {
        Double rate = response.getConversionRates().get(target);

        if (rate == null) {
            throw new CurrencyNotFound("invalid target currency");
        }
        return BigDecimal.valueOf(rate);
    }
}

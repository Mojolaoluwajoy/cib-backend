package org.app.corporateinternetbanking.integration.currency.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CurrencyIntegrationResponse {
    private String base;

    @JsonProperty("conversion_rates")
    private Map<String, Double> conversionRates;
}

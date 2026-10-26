package org.app.corporateinternetbanking.currency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Collectors;
import org.app.corporateinternetbanking.currency.domain.entity.Currency;
import org.app.corporateinternetbanking.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

class CurrencySeedIntegrationTest extends IntegrationTestBase {

    @Test
    void seededCurrenciesHaveTheirRealSymbols() {
        Map<String, String> symbols =
                currencyRepository.findAll().stream().collect(Collectors.toMap(Currency::getCode, Currency::getSymbol));

        assertThat(symbols)
                .containsEntry("NGN", "₦")
                .containsEntry("USD", "$")
                .containsEntry("EUR", "€")
                .containsEntry("CAD", "C$")
                .containsEntry("AUD", "A$");
    }
}

package org.app.corporateinternetbanking.integration.currency;

import org.app.corporateinternetbanking.integration.currency.dto.CurrencyIntegrationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "currency-client", url = "${exchange.rate.base.url}")
public interface CurrencyClient {

    @GetMapping("/latest/{base}")
    CurrencyIntegrationResponse getRates(@PathVariable String base);
}

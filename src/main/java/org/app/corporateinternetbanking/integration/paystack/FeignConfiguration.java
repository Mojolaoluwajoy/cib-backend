package org.app.corporateinternetbanking.integration.paystack;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

// Deliberately NOT annotated with @Configuration: that would make component scan register
// this interceptor globally, attaching the Paystack secret key to every Feign client
// (including the exchange-rate client). It is applied only via PayStackClient's `configuration`.
public class FeignConfiguration {

    @Value("${paystack.secret.key}")
    private String secretKey;

    @Bean
    public RequestInterceptor paystackAuthInterceptor() {
        return template -> {
            template.removeHeader("Authorization");
            template.header("Authorization", "Bearer " + secretKey);
            template.header("Content-Type", "application/json");
        };
    }
}

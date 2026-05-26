package org.app.corporateinternetbanking.integration.paystack;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {

    @Value("${paystack.secret.key}")
    private String secretKey;

    @Bean
    public RequestInterceptor requestInterceptor() {

        return template -> {
            template.header("Authorization", "Bearer " + secretKey);
            template.header("Content-Type", "application/json");
        };
    }


}

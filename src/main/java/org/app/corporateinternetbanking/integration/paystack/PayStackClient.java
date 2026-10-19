package org.app.corporateinternetbanking.integration.paystack;

import org.app.corporateinternetbanking.integration.paystack.dto.CreateCustomerRequest;
import org.app.corporateinternetbanking.integration.paystack.dto.CreateDvaRequest;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackCustomerResponse;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackDvaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

// The Authorization header is added by FeignConfiguration's interceptor.
@FeignClient(
        name = "paystack-client",
        url = "https://api.paystack.co",
        configuration = FeignConfiguration.class)
public interface PayStackClient {

    @PostMapping("/transaction/initialize")
    Map<String, Object> initializeTransaction(@RequestBody Map<String, Object> body);

    @PostMapping("/transferrecipient")
    Map<String, Object> createRecipient(@RequestBody Map<String, Object> body);

    @PostMapping("/transfer")
    Map<String, Object> initiateTransfer(@RequestBody Map<String, Object> body);

    @PostMapping("/customer")
    PaystackCustomerResponse createCustomer(@RequestBody CreateCustomerRequest request);

    @PostMapping("/dedicated_account")
    PaystackDvaResponse createDedicatedAccount(@RequestBody CreateDvaRequest request);

    // Paystack expects amounts as an integer in the lowest denomination (kobo), not a decimal.
    static long toKobo(BigDecimal naira) {
        return naira.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

}

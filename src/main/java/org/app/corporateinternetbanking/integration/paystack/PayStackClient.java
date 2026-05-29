package org.app.corporateinternetbanking.integration.paystack;

import org.app.corporateinternetbanking.integration.paystack.dto.CreateCustomerRequest;
import org.app.corporateinternetbanking.integration.paystack.dto.CreateDvaRequest;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackCustomerResponse;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackDvaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "paystack-client",
        url = "https://api.paystack.co",
        configuration = FeignConfiguration.class)
public interface PayStackClient {

    @PostMapping("/transaction/initialize")
    java.util.Map<String, Object> initializeTransaction(Map<String, Object> body);

    @PostMapping("/transferrecipient")
    java.util.Map<String, Object> createRecipient(Map<String, Object> body);

    @PostMapping("/transfer")
    java.util.Map<String, Object> initiateTransfer(Map<String, Object> body);

    @PostMapping("/customer")
    PaystackCustomerResponse createCustomer(
            @RequestHeader("Authorization") String authorization,
            @RequestBody CreateCustomerRequest request
    );

    @PostMapping("/dedicated_account")
    PaystackDvaResponse createDedicatedAccount(
            @RequestHeader("Authorization") String authorization,
            @RequestBody CreateDvaRequest request
    );

}

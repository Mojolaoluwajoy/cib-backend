package org.app.corporateinternetbanking.integration.paystack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.app.corporateinternetbanking.integration.paystack.dto.CreateCustomerRequest;
import org.app.corporateinternetbanking.integration.paystack.dto.CreateDvaRequest;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackBankListResponse;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackCustomerResponse;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackDvaResponse;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackResolveAccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

// The Authorization header is added by FeignConfiguration's interceptor.
@FeignClient(name = "paystack-client", url = "https://api.paystack.co", configuration = FeignConfiguration.class)
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

    // One page of banks; pass the previous page's meta.next as "next" to get the following page.
    @GetMapping("/bank")
    PaystackBankListResponse listBanks(
            @RequestParam("country") String country,
            @RequestParam("use_cursor") boolean useCursor,
            @RequestParam("perPage") int perPage,
            @RequestParam(value = "next", required = false) String next);

    @GetMapping("/bank/resolve")
    PaystackResolveAccountResponse resolveAccount(
            @RequestParam("account_number") String accountNumber, @RequestParam("bank_code") String bankCode);

    // Paystack expects amounts as an integer in the lowest denomination (kobo), not a decimal.
    static long toKobo(BigDecimal naira) {
        return naira.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}

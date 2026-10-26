package org.app.corporateinternetbanking.integration.paystack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PaystackResolveAccountResponse {

    private boolean status;
    private Data data;

    @Setter
    @Getter
    public static class Data {

        @JsonProperty("account_number")
        private String accountNumber;

        @JsonProperty("account_name")
        private String accountName;
    }
}

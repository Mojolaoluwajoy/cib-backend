package org.app.corporateinternetbanking.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PaystackWebhookRequest {
    private String event;
    private Data data;

    @Setter
    @Getter
    public static class Data {
        private String reference;
        private String status;
        private Long amount;
        @JsonProperty
        private Customer customer;

        public String getCustomerCode() {
            return customer != null ? customer.getCustomerCode() : null;
        }

        @Setter
        @Getter
        public static class Customer {
            @JsonProperty("customer_code")
            private String customerCode;
        }
    }
}

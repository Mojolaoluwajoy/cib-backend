package org.app.corporateinternetbanking.integration.paystack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PaystackDvaResponse {

    private boolean status;
    private Data data;


    @Setter
    @Getter
    public static class Data {

        @JsonProperty
        private String accountNumber;
        private Bank bank;

    }

    @Setter
    @Getter
    public static class Bank {
        private String name;

    }
}

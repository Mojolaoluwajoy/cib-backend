package org.app.corporateinternetbanking.integration.paystack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateDvaRequest {

    private String customer;

    // "test-bank" with test keys; "wema-bank" or "titan-paystack" with live keys
    @JsonProperty("preferred_bank")
    private String preferredBank;

    public CreateDvaRequest(String customerCode, String preferredBank) {
        this.customer = customerCode;
        this.preferredBank = preferredBank;
    }
}

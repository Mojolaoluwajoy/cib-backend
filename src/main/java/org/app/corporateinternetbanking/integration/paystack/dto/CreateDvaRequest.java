package org.app.corporateinternetbanking.integration.paystack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateDvaRequest {

    private String customer;

    @JsonProperty("preferred_bank")
    private String preferredBank;

    public CreateDvaRequest(String customerCode) {
        this.customer = customerCode;
        this.preferredBank = "titan-paystack";
    }
}
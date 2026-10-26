package org.app.corporateinternetbanking.integration.paystack.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PaystackBankListResponse {

    private boolean status;
    private List<Bank> data;
    private Meta meta;

    @Setter
    @Getter
    public static class Bank {
        private String name;
        private String code;
        private boolean active;
    }

    // Cursor pagination: "next" is the cursor for the following page, null on the last page.
    @Setter
    @Getter
    public static class Meta {
        private String next;
    }
}

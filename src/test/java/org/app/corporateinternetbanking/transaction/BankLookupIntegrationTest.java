package org.app.corporateinternetbanking.transaction;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import feign.FeignException;
import feign.Request;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackBankListResponse;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackResolveAccountResponse;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.support.IntegrationTestBase;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Bank list and account-name lookup used by the payout form; Paystack is mocked.
class BankLookupIntegrationTest extends IntegrationTestBase {

    User maker;
    User admin;

    @BeforeEach
    void staff() {
        Organization org = organization("Acme");
        maker = user("maker@acme.com", UserRole.MAKER, org);
        admin = user("admin@acme.com", UserRole.ADMIN, org);
    }

    @Test
    void listsActiveBanksFromEveryPageSortedByName() throws Exception {
        when(payStackClient.listBanks(eq("nigeria"), anyBoolean(), anyInt(), isNull()))
                .thenReturn(bankPage("cursor-2", bank("Zenith Bank", "057", true), bank("Old Bank", "999", false)));
        when(payStackClient.listBanks(eq("nigeria"), anyBoolean(), anyInt(), eq("cursor-2")))
                .thenReturn(bankPage(null, bank("Access Bank", "044", true)));

        mvc.perform(get("/external/transaction/banks").header("Authorization", bearer(maker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", contains("Access Bank", "Zenith Bank")))
                .andExpect(jsonPath("$.data[*].code", contains("044", "057")));
    }

    @Test
    void resolvesTheAccountHoldersName() throws Exception {
        PaystackResolveAccountResponse response = new PaystackResolveAccountResponse();
        PaystackResolveAccountResponse.Data data = new PaystackResolveAccountResponse.Data();
        data.setAccountNumber("0123456789");
        data.setAccountName("JANE DOE");
        response.setData(data);
        when(payStackClient.resolveAccount("0123456789", "044")).thenReturn(response);

        mvc.perform(get("/external/transaction/banks/resolve")
                        .param("accountNumber", "0123456789")
                        .param("bankCode", "044")
                        .header("Authorization", bearer(maker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountName").value("JANE DOE"));
    }

    @Test
    void anAccountPaystackCannotFindIsABadRequest() throws Exception {
        Request request =
                Request.create(Request.HttpMethod.GET, "/bank/resolve", Map.of(), null, StandardCharsets.UTF_8, null);
        when(payStackClient.resolveAccount(any(), any()))
                .thenThrow(new FeignException.UnprocessableEntity("Could not resolve", request, null, Map.of()));

        mvc.perform(get("/external/transaction/banks/resolve")
                        .param("accountNumber", "0123456789")
                        .param("bankCode", "044")
                        .header("Authorization", bearer(maker)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No account with that number was found at the selected bank"));
    }

    @Test
    void rejectsAnAccountNumberThatIsNotTenDigitsWithoutCallingPaystack() throws Exception {
        mvc.perform(get("/external/transaction/banks/resolve")
                        .param("accountNumber", "12345")
                        .param("bankCode", "044")
                        .header("Authorization", bearer(maker)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(payStackClient);
    }

    @Test
    void onlyRolesThatCanSendPayoutsMayLookUpAccounts() throws Exception {
        mvc.perform(get("/external/transaction/banks").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
    }

    private static PaystackBankListResponse.Bank bank(String name, String code, boolean active) {
        PaystackBankListResponse.Bank bank = new PaystackBankListResponse.Bank();
        bank.setName(name);
        bank.setCode(code);
        bank.setActive(active);
        return bank;
    }

    private static PaystackBankListResponse bankPage(String next, PaystackBankListResponse.Bank... banks) {
        PaystackBankListResponse page = new PaystackBankListResponse();
        page.setData(List.of(banks));
        PaystackBankListResponse.Meta meta = new PaystackBankListResponse.Meta();
        meta.setNext(next);
        page.setMeta(meta);
        return page;
    }
}

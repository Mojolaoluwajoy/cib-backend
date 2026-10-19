package org.app.corporateinternetbanking.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.enums.OrganizationStatus;
import org.app.corporateinternetbanking.support.IntegrationTestBase;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

// Two organizations, each with its own staff and account. Every test has one tenant reach for the other's data.
class TenantIsolationIntegrationTest extends IntegrationTestBase {

    Organization orgA;
    Organization orgB;
    User adminA;
    User makerA;
    User approverA;
    User adminB;
    User makerB;
    User approverB;
    Account accountA;
    Account accountB;

    @BeforeEach
    void twoTenants() {
        orgA = organization("OrgA");
        orgB = organization("OrgB");
        adminA = user("admin@a.com", UserRole.ADMIN, orgA);
        makerA = user("maker@a.com", UserRole.MAKER, orgA);
        approverA = user("approver@a.com", UserRole.APPROVER, orgA);
        adminB = user("admin@b.com", UserRole.ADMIN, orgB);
        makerB = user("maker@b.com", UserRole.MAKER, orgB);
        approverB = user("approver@b.com", UserRole.APPROVER, orgB);
        accountA = account(orgA, "5000000001", "1000");
        accountB = account(orgB, "5000000002", "1000");
    }

    @Test
    void accountListsOnlyShowTheCallersOwnOrganization() throws Exception {
        mvc.perform(get("/accounts/all").header("Authorization", bearer(makerB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].accountNumber", contains("5000000002")));
    }

    @Test
    void anotherOrganizationsAccountCannotBeViewed() throws Exception {
        mvc.perform(post("/accounts/find")
                        .header("Authorization", bearer(makerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(accountA.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/accounts/organization/" + orgA.getId()).header("Authorization", bearer(makerB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void makerCannotMoveMoneyOutOfAnotherOrganizationsAccount() throws Exception {
        mvc.perform(post("/transactions/initiate")
                        .header("Authorization", bearer(makerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transfer("5000000001", "5000000002", "k1")))
                .andExpect(status().isForbidden());

        assertThat(reload(accountA).getAvailableBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void makerCanSendMoneyToAnotherOrganization() throws Exception {
        mvc.perform(post("/transactions/initiate")
                        .header("Authorization", bearer(makerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transfer("5000000001", "5000000002", "k2")))
                .andExpect(status().isOk());
    }

    @Test
    void onlyTheSourceOrganizationsApproverCanApprove() throws Exception {
        Transaction transfer = awaitingApproval(accountA, accountB, "300", makerA);
        String approval = "{\"transactionId\":" + transfer.getId() + ",\"status\":\"SUCCESS\"}";

        mvc.perform(post("/transactions/approve")
                        .header("Authorization", bearer(approverB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approval))
                .andExpect(status().isForbidden());
        assertThat(reload(transfer).getStatus()).isEqualTo(TransactionStatus.PENDING_APPROVAL);

        mvc.perform(post("/transactions/approve")
                        .header("Authorization", bearer(approverA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approval))
                .andExpect(status().isOk());
        assertThat(reload(accountA).getTotalBalance()).isEqualByComparingTo("700");
        assertThat(reload(accountB).getTotalBalance()).isEqualByComparingTo("1300");
    }

    @Test
    void anotherOrganizationsTransactionHistoryIsForbidden() throws Exception {
        mvc.perform(get("/transactions/organization/" + orgA.getId())
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", bearer(makerB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotEditAnotherOrganizationOrChangeTheirOwnStatus() throws Exception {
        mvc.perform(put("/organizations/" + orgA.getId() + "/profile")
                        .header("Authorization", bearer(adminB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"hacked\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/organizations/" + orgA.getId() + "/profile")
                        .header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationStatus\":\"DISABLED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/organizations/" + orgA.getId() + "/profile")
                        .header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"OrgA Ltd\"}"))
                .andExpect(status().isOk());

        Organization after = organizationRepository.findById(orgA.getId()).orElseThrow();
        assertThat(after.getName()).isEqualTo("OrgA Ltd");
        assertThat(after.getOrganizationStatus()).isEqualTo(OrganizationStatus.APPROVED);
    }

    @Test
    void usersPageIsScopedAndNeverExposesPasswordHashes() throws Exception {
        mvc.perform(get("/users/users").param("page", "0").param("size", "50").header("Authorization", bearer(adminB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.content[*].email", containsInAnyOrder("admin@b.com", "maker@b.com", "approver@b.com")))
                .andExpect(jsonPath("$.data.content[*]", everyItem(not(hasKey("password")))));
    }

    @Test
    void adminCannotInviteASuperAdmin() throws Exception {
        mvc.perform(post("/users/invitation")
                        .header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"evil@x.com\",\"role\":\"SUPER_ADMIN\"}"))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findByEmail("evil@x.com")).isEmpty();
    }

    @Test
    void onlyAnAdminOfTheSameOrganizationCanEditAnotherUsersProfile() throws Exception {
        String emailChange = "{\"email\":\"takeover@x.com\"}";
        mvc.perform(put("/users/" + makerA.getId() + "/profile")
                        .header("Authorization", bearer(adminB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailChange))
                .andExpect(status().isForbidden());
        mvc.perform(put("/users/" + approverA.getId() + "/profile")
                        .header("Authorization", bearer(makerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailChange))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findByEmail("takeover@x.com")).isEmpty();
    }

    private static String transfer(String from, String to, String idempotencyKey) {
        return """
                {"sourceAccount":"%s","destinationAccount":"%s","amount":100,"type":"INTERNAL_TRANSFER",\
                "idempotencyKey":"%s"}"""
                .formatted(from, to, idempotencyKey);
    }
}

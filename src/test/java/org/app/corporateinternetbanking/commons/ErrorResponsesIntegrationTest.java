package org.app.corporateinternetbanking.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.support.IntegrationTestBase;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// Every failure must reach the client as a GenericResponse with a meaningful status, never as an empty 403.
class ErrorResponsesIntegrationTest extends IntegrationTestBase {

    @Test
    void wrongPasswordAndUnknownEmailAreTheSame401() throws Exception {
        Organization org = organization("Acme");
        user("admin@acme.com", UserRole.ADMIN, org);

        login("admin@acme.com", "not-the-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        login("nobody@acme.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void inactiveAccountCannotLogIn() throws Exception {
        User pending = user("pending@acme.com", UserRole.ADMIN, organization("Acme"));
        pending.setStatus(UserStatus.INACTIVE);
        userRepository.save(pending);

        login("pending@acme.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("This account is not active yet"));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(json(post("/auth/login"), "{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void unknownEnumValueNamesTheField() throws Exception {
        User maker = user("maker@acme.com", UserRole.MAKER, organization("Acme"));

        mvc.perform(
                        json(
                                post("/transactions/initiate"),
                                maker,
                                """
                        {"sourceAccount":"5000000001","amount":10,"type":"TELEPORT","idempotencyKey":"k"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for type"));
    }

    @Test
    void emptySignupListsEveryMissingFieldAndCreatesNothing() throws Exception {
        mvc.perform(json(post("/organizations/create"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").value("must not be blank"))
                .andExpect(jsonPath("$.data.email").value("must not be blank"))
                .andExpect(jsonPath("$.data.password").exists())
                .andExpect(jsonPath("$.data.nin").exists());

        assertThat(organizationRepository.count()).isZero();
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void signupRejectsBadEmailShortPasswordAndBadNin() throws Exception {
        mvc.perform(
                        json(
                                post("/organizations/create"),
                                """
                        {"name":"Acme","registrationNumber":"RC1","firstName":"Ada","lastName":"L",\
                        "email":"not-an-email","password":"short","nin":"123",\
                        "organizationEmail":"ops@acme.com"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.email").value("must be a well-formed email address"))
                .andExpect(jsonPath("$.data.password").value("must be 8 to 72 characters"))
                .andExpect(jsonPath("$.data.nin").value("must be 11 digits"));
    }

    @Test
    void negativePayoutIsRejectedBeforePaystackIsCalled() throws Exception {
        Organization org = organization("Acme");
        User maker = user("maker@acme.com", UserRole.MAKER, org);
        account(org, "5000000001", "1000");

        mvc.perform(
                        json(
                                post("/external/transaction/payout"),
                                maker,
                                """
                        {"sourceAccount":"5000000001","amount":-500,"accountNumber":"0123456789",\
                        "bankCode":"058","accountName":"Ada","idempotencyKey":"p1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.amount").value("must be greater than 0"));

        verifyNoInteractions(payStackClient);
    }

    @Test
    void amountsWithMoreThanTwoDecimalsAreRejected() throws Exception {
        Organization org = organization("Acme");
        User maker = user("maker@acme.com", UserRole.MAKER, org);

        mvc.perform(
                        json(
                                post("/transactions/initiate"),
                                maker,
                                """
                        {"sourceAccount":"5000000001","destinationAccount":"5000000002","amount":1.005,\
                        "type":"INTERNAL_TRANSFER","idempotencyKey":"k"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.amount").value("must have at most 2 decimal places"));
    }

    @Test
    void payoutWithoutRecipientIs400AndReservesNothing() throws Exception {
        Organization org = organization("Acme");
        User maker = user("maker@acme.com", UserRole.MAKER, org);
        account(org, "5000000001", "1000");

        mvc.perform(
                        json(
                                post("/transactions/initiate"),
                                maker,
                                """
                        {"sourceAccount":"5000000001","amount":100,"type":"EXTERNAL_PAYOUT","idempotencyKey":"k"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("payoutRecipientId is required for a payout"));

        assertThat(accountRepository
                        .findByAccountNumber("5000000001")
                        .orElseThrow()
                        .getAvailableBalance())
                .isEqualByComparingTo("1000");
    }

    @Test
    void depositsCannotBeInitiatedManually() throws Exception {
        User maker = user("maker@acme.com", UserRole.MAKER, organization("Acme"));

        mvc.perform(
                        json(
                                post("/transactions/initiate"),
                                maker,
                                """
                        {"destinationAccount":"5000000001","amount":100,"type":"EXTERNAL_DEPOSIT","idempotencyKey":"k"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void passwordResetTokenAcceptsJsonAndPlainEmail() throws Exception {
        user("admin@acme.com", UserRole.ADMIN, organization("Acme"));

        mvc.perform(json(post("/auth/password/token"), "{\"email\":\"admin@acme.com\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/auth/password/token")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("admin@acme.com"))
                .andExpect(status().isCreated());
        mvc.perform(json(post("/auth/password/token"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email is required"));

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void missingTokenIs401AndWrongRoleIs403WithAMessage() throws Exception {
        mvc.perform(get("/accounts/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        User maker = user("maker@acme.com", UserRole.MAKER, organization("Acme"));
        mvc.perform(json(post("/currencies/status"), maker, "{\"code\":\"USD\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));
    }

    @Test
    void unknownRouteIs404() throws Exception {
        User maker = user("maker@acme.com", UserRole.MAKER, organization("Acme"));

        mvc.perform(get("/does-not-exist").header("Authorization", bearer(maker)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(json(
                post("/auth/login"), """
                {"email":"%s","password":"%s"}""".formatted(email, password)));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, User user, String body) {
        return json(request, body).header("Authorization", bearer(user));
    }
}

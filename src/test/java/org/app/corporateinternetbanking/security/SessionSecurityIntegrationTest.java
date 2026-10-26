package org.app.corporateinternetbanking.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import feign.FeignException;
import feign.Request;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.security.session.RefreshTokenCookie;
import org.app.corporateinternetbanking.support.IntegrationTestBase;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class SessionSecurityIntegrationTest extends IntegrationTestBase {

    User admin;

    @BeforeEach
    void setUp() {
        Organization org = organization("Acme");
        admin = user("admin@acme.com", UserRole.ADMIN, org);
    }

    @Test
    void loginReturnsShortLivedAccessTokenAndRefreshToken() throws Exception {
        login("admin@acme.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(1800));
    }

    @Test
    void theRefreshTokenOnlyTravelsInAnHttpOnlyCookie() throws Exception {
        login("admin@acme.com", PASSWORD)
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string(
                                HttpHeaders.SET_COOKIE,
                                allOf(
                                        containsString(RefreshTokenCookie.NAME + "="),
                                        containsString("HttpOnly"),
                                        containsString("Path=/auth"),
                                        containsString("SameSite=Strict"),
                                        containsString("Max-Age=604800"))));
    }

    @Test
    void clientsWithoutCookiesCanStillSendTheRefreshTokenInTheBody() throws Exception {
        Tokens tokens = loginTokens();

        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.refresh + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void refreshingWithoutAnyRefreshTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokensAndTheNewAccessTokenWorks() throws Exception {
        Tokens first = loginTokens();

        Tokens second = tokens(refresh(first.refresh).andExpect(status().isOk()));
        assertThat(second.refresh).isNotEqualTo(first.refresh);
        dashboard(second.access).andExpect(status().isOk());

        // The used refresh token is dead; the newest one keeps working
        refresh(first.refresh).andExpect(status().isUnauthorized());
        refresh(second.refresh).andExpect(status().isOk());
    }

    @Test
    void replayingAnOldRefreshTokenEndsTheWholeSession() throws Exception {
        Tokens first = loginTokens();
        Tokens second = tokens(refresh(first.refresh));
        // Outside the grace period for two tabs refreshing at once
        jdbc.update("UPDATE auth_session SET last_refreshed_at = now() - interval '1 minute'");

        refresh(first.refresh).andExpect(status().isUnauthorized());

        refresh(second.refresh).andExpect(status().isUnauthorized());
        dashboard(second.access)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your session has ended. Please log in again"));
    }

    @Test
    void logoutEndsTheAccessTokenAndTheRefreshToken() throws Exception {
        Tokens tokens = loginTokens();
        dashboard(tokens.access).andExpect(status().isOk());

        mvc.perform(post("/auth/logout").cookie(refreshCookie(tokens.refresh)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        dashboard(tokens.access).andExpect(status().isUnauthorized());
        refresh(tokens.refresh).andExpect(status().isUnauthorized());
    }

    @Test
    void changingThePasswordSignsOutEveryDevice() throws Exception {
        Tokens laptop = loginTokens();
        Tokens phone = loginTokens();

        mvc.perform(post("/auth/password/reset")
                        .header("Authorization", "Bearer " + laptop.access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"" + PASSWORD + "\",\"newPassword\":\"Brand-N3w-Pass\"}"))
                .andExpect(status().isCreated());

        dashboard(laptop.access).andExpect(status().isUnauthorized());
        dashboard(phone.access).andExpect(status().isUnauthorized());
        refresh(phone.refresh).andExpect(status().isUnauthorized());
        login("admin@acme.com", "Brand-N3w-Pass").andExpect(status().isOk());
    }

    @Test
    void tokensWithoutASessionAreRejected() throws Exception {
        String legacy = jwtService.generateToken(admin, null);

        dashboard(legacy).andExpect(status().isUnauthorized());
    }

    @Test
    void fiveWrongPasswordsLockTheEmailEvenForTheRightPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("admin@acme.com", "wrong-password").andExpect(status().isUnauthorized());
        }

        login("admin@acme.com", PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").value("Too many failed login attempts. Try again in 15 minute(s)"));
        // Other accounts are unaffected
        user("other@acme.com", UserRole.MAKER, admin.getOrganization());
        login("other@acme.com", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void unknownEmailsLockTheSameWaySoLockoutsRevealNothing() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("ghost@acme.com", "whatever-pass").andExpect(status().isUnauthorized());
        }
        login("ghost@acme.com", "whatever-pass").andExpect(status().isTooManyRequests());
    }

    @Test
    void anIpSprayingManyEmailsGetsBlocked() throws Exception {
        for (int i = 0; i < 20; i++) {
            login("user" + i + "@acme.com", "guess-pass").andExpect(status().isUnauthorized());
        }
        login("admin@acme.com", PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void corsAllowsOnlyTheConfiguredFrontend() throws Exception {
        mvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mvc.perform(options("/auth/login")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void paystackErrorsAreNotPassedToTheClient() throws Exception {
        User maker = user("maker@acme.com", UserRole.MAKER, admin.getOrganization());
        account(admin.getOrganization(), "5000000001", "1000");
        Request request = Request.create(
                Request.HttpMethod.POST, "https://api.paystack.co/transferrecipient", Map.of(), null, null, null);
        when(payStackClient.createRecipient(any()))
                .thenThrow(new FeignException.BadRequest(
                        "Bad Request",
                        request,
                        "{\"message\":\"internal paystack detail\"}".getBytes(StandardCharsets.UTF_8),
                        Map.of()));

        mvc.perform(
                        post("/external/transaction/payout")
                                .header("Authorization", bearer(maker))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"sourceAccount":"5000000001","amount":100,"accountNumber":"0123456789",\
                                "bankCode":"058","accountName":"Ada","idempotencyKey":"p1"}"""))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message")
                        .value("An external provider could not process the request. Please try again later."));
    }

    private record Tokens(String access, String refresh) {}

    private Tokens loginTokens() throws Exception {
        return tokens(login("admin@acme.com", PASSWORD).andExpect(status().isOk()));
    }

    private static Tokens tokens(ResultActions result) throws Exception {
        var response = result.andReturn().getResponse();
        String access = JsonPath.read(response.getContentAsString(), "$.data.token");
        return new Tokens(access, response.getCookie(RefreshTokenCookie.NAME).getValue());
    }

    private static Cookie refreshCookie(String refreshToken) {
        return new Cookie(RefreshTokenCookie.NAME, refreshToken);
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/auth/refresh").cookie(refreshCookie(refreshToken)));
    }

    private ResultActions dashboard(String accessToken) throws Exception {
        return mvc.perform(get("/dashboard/stats").header("Authorization", "Bearer " + accessToken));
    }
}

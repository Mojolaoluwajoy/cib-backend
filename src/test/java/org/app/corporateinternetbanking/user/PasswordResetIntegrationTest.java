package org.app.corporateinternetbanking.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.support.IntegrationTestBase;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.ResultActions;

class PasswordResetIntegrationTest extends IntegrationTestBase {

    User alice;
    User victim;

    @BeforeEach
    void users() {
        Organization org = organization("Acme");
        alice = user("alice@acme.com", UserRole.MAKER, org);
        victim = user("boss@acme.com", UserRole.ADMIN, org);
    }

    @Test
    void ownResetTokenCannotResetSomeoneElsesPassword() throws Exception {
        String aliceToken = requestResetToken("alice@acme.com");

        resetPassword("boss@acme.com", aliceToken, "Pwned123!").andExpect(status().isBadRequest());

        login("boss@acme.com", PASSWORD).andExpect(status().isOk());
        login("boss@acme.com", "Pwned123!").andExpect(status().is4xxClientError());
    }

    @Test
    void resetStoresAHashedPasswordThatWorksForLogin() throws Exception {
        String token = requestResetToken("alice@acme.com");

        resetPassword("alice@acme.com", token, "NewPass#1").andExpect(status().isCreated());

        String stored =
                userRepository.findByEmail("alice@acme.com").orElseThrow().getPassword();
        assertThat(stored).startsWith("$2").isNotEqualTo("NewPass#1");
        login("alice@acme.com", "NewPass#1").andExpect(status().isOk());
        login("alice@acme.com", PASSWORD).andExpect(status().is4xxClientError());
    }

    @Test
    void resetTokenWorksOnlyOnce() throws Exception {
        String token = requestResetToken("alice@acme.com");

        resetPassword("alice@acme.com", token, "NewPass#1").andExpect(status().isCreated());
        resetPassword("alice@acme.com", token, "Again#2").andExpect(status().isBadRequest());

        login("alice@acme.com", "NewPass#1").andExpect(status().isOk());
    }

    @Test
    void resetTokenCannotBeUsedToCallTheApi() throws Exception {
        String token = requestResetToken("alice@acme.com");

        mvc.perform(get("/dashboard/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/dashboard/stats").header("Authorization", bearer(alice)))
                .andExpect(status().isOk());
    }

    @Test
    void malformedBearerTokenGetsUnauthorizedRatherThanAServerError() throws Exception {
        mvc.perform(get("/dashboard/stats").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    // Requests a reset email and pulls the token out of it, just as a user would.
    private String requestResetToken(String email) throws Exception {
        mvc.perform(post("/auth/password/token")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(email))
                .andExpect(status().isCreated());
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeastOnce()).send(sent.capture());
        List<SimpleMailMessage> messages = sent.getAllValues();
        SimpleMailMessage latest = messages.get(messages.size() - 1);
        assertThat(latest.getTo()).containsExactly(email);
        return latest.getText().split("token is:")[1].trim();
    }

    private ResultActions resetPassword(String email, String token, String newPassword) throws Exception {
        return mvc.perform(post("/auth/forgotten/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","token":"%s","newPassword":"%s"}"""
                        .formatted(email, token, newPassword)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password)));
    }
}

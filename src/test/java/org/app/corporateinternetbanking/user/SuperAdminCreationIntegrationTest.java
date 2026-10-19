package org.app.corporateinternetbanking.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.support.IntegrationTestBase;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.app.corporateinternetbanking.user.exceptions.UserAlreadyRegistered;
import org.app.corporateinternetbanking.user.superAdminConfiguration.SuperAdminBootstrap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;

class SuperAdminCreationIntegrationTest extends IntegrationTestBase {

    @Autowired
    SuperAdminBootstrap bootstrap;

    @Test
    void commandCreatesAnActiveSuperAdminWhoCanLogIn() throws Exception {
        User created = bootstrap.create("owner@cib.test", "Ada", "Lovelace", "Sup3r-Secret!".toCharArray());

        assertThat(created.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.getOrganization()).isNull();
        assertThat(created.getPassword()).startsWith("$2");
        login("owner@cib.test", "Sup3r-Secret!").andExpect(status().isOk());
    }

    @Test
    void commandRejectsWeakPasswordsBadEmailsAndDuplicates() throws Exception {
        assertThatThrownBy(() -> bootstrap.create("owner@cib.test", null, null, "short".toCharArray()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bootstrap.create("not-an-email", null, null, "Sup3r-Secret!".toCharArray()))
                .isInstanceOf(IllegalArgumentException.class);

        bootstrap.create("owner@cib.test", null, null, "Sup3r-Secret!".toCharArray());
        assertThatThrownBy(() -> bootstrap.create("owner@cib.test", null, null, "An0ther-Secret!".toCharArray()))
                .isInstanceOf(UserAlreadyRegistered.class);
    }

    @Test
    void superAdminCanInviteAnotherSuperAdminWhoCompletesRegistration() throws Exception {
        User owner = bootstrap.create("owner@cib.test", null, null, "Sup3r-Secret!".toCharArray());

        mvc.perform(post("/users/invitation")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"second@cib.test\",\"role\":\"SUPER_ADMIN\"}"))
                .andExpect(status().isCreated());

        User invited = userRepository.findByEmail("second@cib.test").orElseThrow();
        assertThat(invited.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(invited.getOrganization()).isNull();
        assertThat(invited.getStatus()).isEqualTo(UserStatus.INACTIVE);

        mvc.perform(post("/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"email":"second@cib.test","token":"%s","firstName":"Grace","lastName":"Hopper",\
                                "password":"An0ther-Secret!","nin":"12345678901"}"""
                                        .formatted(invitationToken())))
                .andExpect(status().isCreated());

        login("second@cib.test", "An0ther-Secret!").andExpect(status().isOk());
    }

    @Test
    void organizationAdminCannotInviteASuperAdmin() throws Exception {
        Organization org = organization("Acme");
        User admin = user("admin@acme.com", UserRole.ADMIN, org);

        mvc.perform(post("/users/invitation")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"evil@x.com\",\"role\":\"SUPER_ADMIN\"}"))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findByEmail("evil@x.com")).isEmpty();
    }

    // The invitation email contains a registration link with the URL-encoded token.
    private String invitationToken() {
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        String text = sent.getValue().getText();
        String encoded =
                text.substring(text.indexOf("token=") + "token=".length()).split("\\s")[0];
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password)));
    }
}

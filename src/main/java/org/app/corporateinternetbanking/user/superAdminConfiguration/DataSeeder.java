package org.app.corporateinternetbanking.user.superAdminConfiguration;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
public class DataSeeder {
    // Credentials for the first super admin come from the environment, never from source code.
    @Value("${SUPER_ADMIN_EMAIL:}")
    private String superAdminEmail;

    @Value("${SUPER_ADMIN_PASSWORD:}")
    private String superAdminPassword;

    @Bean
    CommandLineRunner seed(UserRepository repository, PasswordEncoder encoder) {
        return args -> {
            if (!repository.existsByRole(UserRole.SUPER_ADMIN)) {
                if (superAdminEmail.isBlank() || superAdminPassword.isBlank()) {
                    log.warn(
                            "No SUPER_ADMIN exists and SUPER_ADMIN_EMAIL/SUPER_ADMIN_PASSWORD are not set; skipping seed");
                    return;
                }
                User superAdmin = new User();
                superAdmin.setFirstName("Moyinoluwa");
                superAdmin.setLastName("Michael");
                superAdmin.setEmail(superAdminEmail);
                superAdmin.setRole(UserRole.SUPER_ADMIN);
                superAdmin.setStatus(UserStatus.ACTIVE);
                superAdmin.setCreatedAt(LocalDateTime.now());
                superAdmin.setPassword(encoder.encode(superAdminPassword));
                superAdmin.setNin("12345678432");

                repository.save(superAdmin);
            }
        };
    }
}

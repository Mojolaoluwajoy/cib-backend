package org.app.corporateinternetbanking.user.superAdminConfiguration;

import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seed(UserRepository repository, PasswordEncoder encoder) {
        return args -> {
            if (!repository.existsByRole(UserRole.SUPER_ADMIN)) {
                User superAdmin = new User();
                superAdmin.setFirstName("Moyinoluwa");
                superAdmin.setLastName("Michael");
                superAdmin.setEmail("moyinoluwamichael@gmail.com");
                superAdmin.setRole(UserRole.SUPER_ADMIN);
                superAdmin.setStatus(UserStatus.ACTIVE);
                superAdmin.setCreatedAt(LocalDateTime.now());
                superAdmin.setPassword(encoder.encode("moyin1234"));
                superAdmin.setNin("12345678432");

                repository.save(superAdmin);
            }
        };
    }
}
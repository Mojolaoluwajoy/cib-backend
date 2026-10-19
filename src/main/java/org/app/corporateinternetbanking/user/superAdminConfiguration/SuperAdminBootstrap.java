package org.app.corporateinternetbanking.user.superAdminConfiguration;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.app.corporateinternetbanking.user.exceptions.UserAlreadyRegistered;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Creates platform super admins from the create-super-admin command. Super admins never come from
// configuration: the first is created by an operator with this command, later ones by invitation.
@Service
@RequiredArgsConstructor
public class SuperAdminBootstrap {

    static final int MIN_PASSWORD_LENGTH = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean superAdminExists() {
        return userRepository.existsByRole(UserRole.SUPER_ADMIN);
    }

    @Transactional(rollbackFor = Exception.class)
    public User create(String email, String firstName, String lastName, char[] password) throws UserAlreadyRegistered {
        if (email == null || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("A valid --email is required");
        }
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyRegistered("A user with this email already exists");
        }

        User superAdmin = new User();
        superAdmin.setEmail(email);
        superAdmin.setFirstName(firstName);
        superAdmin.setLastName(lastName);
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setStatus(UserStatus.ACTIVE);
        superAdmin.setCreatedAt(LocalDateTime.now());
        superAdmin.setPassword(passwordEncoder.encode(new String(password)));
        return userRepository.save(superAdmin);
    }
}

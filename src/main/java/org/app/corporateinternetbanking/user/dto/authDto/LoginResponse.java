package org.app.corporateinternetbanking.user.dto.authDto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;

@Setter
@Getter
@RequiredArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private UserRole role;
    private String firstName;
    private String lastName;

    public LoginResponse(User user, String token) {
        this.token = token;
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }
}

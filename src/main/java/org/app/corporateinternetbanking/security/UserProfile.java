package org.app.corporateinternetbanking.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class UserProfile implements UserDetails {
    private String firstName;
    private String lastName;
    private String nin;
    private String password;
    private String email;
    private UserRole role;
    private LocalDateTime createdAt;
    private UserStatus status;

    public static UserProfile parse(User user) {
        UserProfile userProfile = new UserProfile();

        userProfile.email = user.getEmail();
        userProfile.firstName = user.getFirstName();
        userProfile.lastName = user.getLastName();
        userProfile.nin = user.getNin();
        userProfile.password = user.getPassword();
        userProfile.createdAt = user.getCreatedAt();
        userProfile.role = user.getRole();
        userProfile.status = user.getStatus();
        return userProfile;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}

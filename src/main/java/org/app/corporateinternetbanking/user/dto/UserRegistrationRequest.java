package org.app.corporateinternetbanking.user.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserRegistrationRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String nin;
    private String adminKey;
    private String token;

}

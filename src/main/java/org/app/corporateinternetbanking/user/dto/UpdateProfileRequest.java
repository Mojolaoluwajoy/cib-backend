package org.app.corporateinternetbanking.user.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;

    @Email
    private String email;
}

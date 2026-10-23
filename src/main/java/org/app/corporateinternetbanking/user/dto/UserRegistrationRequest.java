package org.app.corporateinternetbanking.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserRegistrationRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "must be 8 to 72 characters")
    private String password;

    @NotBlank
    @Pattern(regexp = "\\d{11}", message = "must be 11 digits")
    private String nin;

    @NotBlank
    private String token;
}

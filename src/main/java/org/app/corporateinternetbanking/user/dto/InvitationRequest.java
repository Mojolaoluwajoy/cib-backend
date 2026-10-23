package org.app.corporateinternetbanking.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.user.enums.UserRole;

@Setter
@Getter
public class InvitationRequest {

    @NotBlank
    @Email
    private String userEmail;

    @NotNull
    private UserRole role;
}

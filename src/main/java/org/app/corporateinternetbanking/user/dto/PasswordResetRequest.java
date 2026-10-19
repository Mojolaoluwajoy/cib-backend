package org.app.corporateinternetbanking.user.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PasswordResetRequest {
    private String email;
    private String oldPassword;
    private String newPassword;
}

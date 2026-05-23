package org.app.corporateinternetbanking.user.dto;

import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.user.enums.UserRole;

@Setter
@Getter
public class InvitationRequest {

    private String userEmail;
    private UserRole role;
}
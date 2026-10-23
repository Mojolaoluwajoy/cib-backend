package org.app.corporateinternetbanking.organization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.organization.enums.OrganizationStatus;
import org.app.corporateinternetbanking.user.enums.UserStatus;

@Setter
@Getter
public class OrganizationApprovalRequest {
    @NotNull
    private Long organizationId;

    @NotNull
    private Long adminId;

    @NotNull
    private OrganizationStatus organizationStatus;

    @NotNull
    private UserStatus userStatus;
}

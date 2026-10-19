package org.app.corporateinternetbanking.organization.dto;

import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.organization.enums.OrganizationStatus;
import org.app.corporateinternetbanking.user.enums.UserStatus;

@Setter
@Getter
public class OrganizationApprovalRequest {
    private Long organizationId;
    private Long adminId;
    private OrganizationStatus organizationStatus;
    private UserStatus userStatus;
}

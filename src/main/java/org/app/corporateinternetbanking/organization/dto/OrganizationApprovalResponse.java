package org.app.corporateinternetbanking.organization.dto;

import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.organization.enums.OrganizationStatus;
import org.app.corporateinternetbanking.user.enums.UserStatus;

@Setter
@Getter
public class OrganizationApprovalResponse {
    private String organizationName;
    private String adminName;
    private OrganizationStatus organizationStatus;
    private UserStatus userStatus;
}

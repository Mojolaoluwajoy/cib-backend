package org.app.corporateinternetbanking.organization.utils.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.dto.OrganizationApprovalRequest;
import org.app.corporateinternetbanking.organization.dto.OrganizationApprovalResponse;
import org.app.corporateinternetbanking.user.domain.entity.User;

@Setter
@Getter
@AllArgsConstructor
public class ApprovalMap {
    private Organization organization;
    private User user;


    public static ApprovalMap mapApprovalRequest(OrganizationApprovalRequest request) {
        Organization organization = new Organization();
        User user = new User();
        organization.setId(request.getOrganizationId());
        organization.setOrganizationStatus(request.getOrganizationStatus());

        user.setId(request.getAdminId());
        user.setStatus(request.getUserStatus());

        return new ApprovalMap(organization, user);
    }

    public static OrganizationApprovalResponse mapApprovalResponse(Organization organization, User user) {
        OrganizationApprovalResponse approvalResponse = new OrganizationApprovalResponse();
        approvalResponse.setOrganizationName(organization.getName());
        approvalResponse.setAdminName(user.getFirstName() + " " + user.getLastName());
        approvalResponse.setOrganizationStatus(organization.getOrganizationStatus());
        approvalResponse.setUserStatus(user.getStatus());
        approvalResponse.setAdminId(user.getId());
        return approvalResponse;
    }
}

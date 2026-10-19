package org.app.corporateinternetbanking.organization.service;

import java.util.List;
import org.app.corporateinternetbanking.organization.dto.*;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationAlreadyExist;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationAlreadyProcessed;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserAlreadyRegistered;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;

public interface OrganizationService {

    OrganizationRegistrationResponse registerOrganization(OrganizationRequest request)
            throws UserAlreadyRegistered, OrganizationAlreadyExist;

    List<OrganizationOnlyResponse> viewAll() throws UserNotFound;

    OrganizationOnlyResponse viewById(long id) throws OrganizationDoesNotExist, UserNotFound, UnauthorizedAccess;

    OrganizationApprovalResponse processOrganizationRegistration(OrganizationApprovalRequest approvalRequest)
            throws OrganizationDoesNotExist, OrganizationAlreadyProcessed, UserNotFound;

    OrganizationOnlyResponse updateOrganization(Long orgId, UpdateOrganizationRequest request)
            throws OrganizationDoesNotExist, UserNotFound, UnauthorizedAccess;
}

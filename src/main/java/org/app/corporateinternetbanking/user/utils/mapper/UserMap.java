package org.app.corporateinternetbanking.user.utils.mapper;

import org.app.corporateinternetbanking.organization.dto.OrganizationId;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.dto.UserResponse;

public class UserMap {


    public static UserResponse mapResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setNin(user.getNin());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());


        if (user.getOrganization() != null) {
            response.setOrganizationId(new OrganizationId(user.getOrganization().getId()));
        }

        return response;
    }
}

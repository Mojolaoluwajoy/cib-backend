package org.app.corporateinternetbanking.user.utils.mapper;

import org.app.corporateinternetbanking.organization.dto.OrganizationId;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.dto.UserRegistrationRequest;
import org.app.corporateinternetbanking.user.dto.UserResponse;
import org.app.corporateinternetbanking.user.enums.UserStatus;

import java.util.UUID;

public class UserMap {


    public static User userMapRequest(UserRegistrationRequest registrationRequest) {
        User user = new User();
        user.setStatus(UserStatus.INACTIVE);
        user.setUserId(UUID.randomUUID().toString());
        user.setFirstName(registrationRequest.getFirstName());
        user.setLastName(registrationRequest.getLastName());
        user.setEmail(registrationRequest.getEmail());
        user.setNin(registrationRequest.getNin());
        user.setNin(registrationRequest.getNin());
        return user;
    }

    public static UserResponse mapResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setNin(user.getNin());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());

        // Only set organizationId if organization is not null
        if (user.getOrganization() != null) {
            response.setOrganizationId(new OrganizationId(user.getOrganization().getId()));
        }

        return response;
    }
}

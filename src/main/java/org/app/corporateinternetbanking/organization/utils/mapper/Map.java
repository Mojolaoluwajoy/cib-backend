package org.app.corporateinternetbanking.organization.utils.mapper;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.dto.OrganizationId;
import org.app.corporateinternetbanking.organization.dto.OrganizationOnlyResponse;
import org.app.corporateinternetbanking.organization.dto.OrganizationRegistrationResponse;
import org.app.corporateinternetbanking.organization.dto.OrganizationRequest;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Map {
    private final UserRepository userRepository;

    public static Organization mapRequest(OrganizationRequest request) {
        Organization organization = new Organization();

        organization.setName(request.getName());
        organization.setRegistrationNumber(request.getRegistrationNumber());
        organization.setOrganizationEmail(request.getOrganizationEmail());
        organization.setPhoneNumber(request.getPhoneNumber());
        organization.setOrganizationEmail(request.getOrganizationEmail());
        return organization;
    }

    public static OrganizationRegistrationResponse mapRegistrationResponse(Organization organization, User user) {
        OrganizationRegistrationResponse response = new OrganizationRegistrationResponse();

        response.setId(organization.getId());
        response.setName(organization.getName());
        response.setRegistrationNumber(organization.getRegistrationNumber());
        response.setOrganizationStatus(organization.getOrganizationStatus());
        response.setOrganizationEmail(organization.getOrganizationEmail());

        response.setUserId(user.getUserId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setNin(user.getNin());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setOrganizationId(new OrganizationId(user.getOrganization().getId()));
        return response;
    }

    public static User mapAdminRequest(OrganizationRequest registrationRequest) {
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setFirstName(registrationRequest.getFirstName());
        user.setLastName(registrationRequest.getLastName());
        user.setEmail(registrationRequest.getEmail());
        user.setNin(registrationRequest.getNin());
        user.setPassword(registrationRequest.getPassword());

        return user;
    }

    public OrganizationOnlyResponse mapResponse(Organization organization) {
        OrganizationOnlyResponse response = new OrganizationOnlyResponse();

        Optional<User> adminOpt = userRepository.findByOrganizationIdAndRole(organization.getId(), UserRole.ADMIN);
        response.setAdminId(adminOpt.map(User::getId).orElse(null));
        response.setId(organization.getId());
        response.setName(organization.getName());
        response.setRegistrationNumber(organization.getRegistrationNumber());
        response.setOrganizationStatus(organization.getOrganizationStatus());
        response.setDvaAccountNumber(organization.getDvaAccountNumber());
        response.setDvaBankName(organization.getDvaBankName());
        return response;
    }
}

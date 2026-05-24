package org.app.corporateinternetbanking.organization.service;

import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.app.corporateinternetbanking.organization.dto.*;
import org.app.corporateinternetbanking.organization.enums.OrganizationStatus;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationAlreadyExist;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationAlreadyProcessed;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.organization.utils.mapper.ApprovalMap;
import org.app.corporateinternetbanking.organization.utils.mapper.Map;
import org.app.corporateinternetbanking.transaction.service.PaystackService;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.app.corporateinternetbanking.user.exceptions.UserAlreadyRegistered;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final PaystackService paystackService;
    @Autowired
    OrganizationRepository repository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public OrganizationRegistrationResponse registerOrganization(OrganizationRequest request) throws UserAlreadyRegistered, OrganizationAlreadyExist {

        Organization organization = Map.mapRequest(request);
        Organization savedOrganization = repository.save(organization);
        repository.findByRegistrationNumber(request.getRegistrationNumber())
                .orElseThrow(() -> new OrganizationAlreadyExist("This organization already exist"));
        String nin = request.getNin();
        String email = request.getEmail();
        if (userRepository.findByNin(nin).isPresent() || userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyRegistered("This user already exists");
        }
        User user = Map.mapAdminRequest(request);
        user.setOrganization(organization);
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);
        user.setRole(UserRole.ADMIN);
        User savedUser = userRepository.save(user);
        return Map.mapRegistrationResponse(savedOrganization, savedUser);
    }

    @Override
    public OrganizationApprovalResponse processOrganizationRegistration(OrganizationApprovalRequest approvalRequest) throws OrganizationDoesNotExist, OrganizationAlreadyProcessed, UserNotFound {
        Organization organization = repository.findById(approvalRequest.getOrganizationId())
                .orElseThrow(() -> new OrganizationDoesNotExist("Organization does not exist"));

        User user = userRepository.findById(approvalRequest.getAdminId())
                .orElseThrow(() -> new UserNotFound("Organization does not exist"));
        if (!organization.getOrganizationStatus().equals(OrganizationStatus.PENDING)) {
            throw new OrganizationAlreadyProcessed("This organization has been processed");
        }
        if (approvalRequest.getOrganizationStatus().equals(OrganizationStatus.APPROVED))
            organization.setOrganizationStatus(OrganizationStatus.APPROVED);

        if (approvalRequest.getOrganizationStatus().equals(OrganizationStatus.REJECTED))
            organization.setOrganizationStatus(OrganizationStatus.REJECTED);

        if (approvalRequest.getUserStatus().equals(UserStatus.ACTIVE))
            user.setStatus(UserStatus.ACTIVE);

        Organization savedOrganization = repository.save(organization);
        User savedUser = userRepository.save(user);

        String customerCode = paystackService.createPaystackCustomer(organization);
        organization.setPayStackCustomerCode(customerCode);
        paystackService.createVirtualAccount(customerCode, organization);
        repository.save(organization);
        return ApprovalMap.mapApprovalResponse(savedOrganization, savedUser);
    }

    @Override
    public List<OrganizationOnlyResponse> viewAll() {
        List<Organization> organization = repository.findAll();
        List<OrganizationOnlyResponse> responseList = new ArrayList<>();
        for (Organization savedOrganization : organization) {
            OrganizationOnlyResponse response = new OrganizationOnlyResponse();
            response.setId(savedOrganization.getId());
            response.setName(savedOrganization.getName());
            response.setRegistrationNumber(savedOrganization.getRegistrationNumber());
            responseList.add(response);
        }
        return responseList;
    }


    @Override
    public OrganizationOnlyResponse viewById(long id) throws OrganizationDoesNotExist {
        Optional<Organization> organization = repository.findById(id);
        if (organization.isEmpty()) {
            throw new OrganizationDoesNotExist("There's no organization with the specified id");
        }
        return Map.mapResponse(organization.get());
    }


}

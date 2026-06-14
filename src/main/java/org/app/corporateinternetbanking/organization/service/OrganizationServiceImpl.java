package org.app.corporateinternetbanking.organization.service;

import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.integration.paystack.FeignConfiguration;
import org.app.corporateinternetbanking.integration.paystack.PayStackClient;
import org.app.corporateinternetbanking.integration.paystack.dto.CreateCustomerRequest;
import org.app.corporateinternetbanking.integration.paystack.dto.CreateDvaRequest;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackCustomerResponse;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackDvaResponse;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.app.corporateinternetbanking.organization.dto.*;
import org.app.corporateinternetbanking.organization.enums.OrganizationStatus;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationAlreadyExist;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationAlreadyProcessed;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.organization.utils.mapper.ApprovalMap;
import org.app.corporateinternetbanking.organization.utils.mapper.Map;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.app.corporateinternetbanking.user.exceptions.UserAlreadyRegistered;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final PayStackClient payStackClient;
    private final Map map;
    @Autowired
    OrganizationRepository repository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    FeignConfiguration feignConfiguration;

    @Override
    @Transactional
    public OrganizationRegistrationResponse registerOrganization(OrganizationRequest request) throws UserAlreadyRegistered, OrganizationAlreadyExist {

        if (repository.findByRegistrationNumber(request.getRegistrationNumber()).isPresent() ||
                repository.findByName(request.getName()).isPresent()) {
            throw new OrganizationAlreadyExist("This organization already exists");
        }

        Organization organization = Map.mapRequest(request);
        organization.setRegisteredAt(LocalDateTime.now());
        Organization savedOrganization = repository.save(organization);

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

        organization.setApprovedAt(LocalDateTime.now());
        Organization savedOrganization = repository.save(organization);
        User savedUser = userRepository.save(user);

        if (approvalRequest.getOrganizationStatus().equals(OrganizationStatus.APPROVED)) {

            CreateCustomerRequest createCustomerRequest = new CreateCustomerRequest(organization.getOrganizationEmail(),
                    organization.getName(), "Organization", organization.getPhoneNumber());

            PaystackCustomerResponse customerResponse = payStackClient.createCustomer(
                    feignConfiguration.getAuthHeader(),
                    createCustomerRequest
            );
            String customerCode = customerResponse.getData().getCustomerCode();

            organization.setPaystackCustomerCode(customerCode);
            repository.save(organization);

            CreateDvaRequest dvaRequest = new CreateDvaRequest(customerCode);

            PaystackDvaResponse dvaResponse = payStackClient.createDedicatedAccount(
                    feignConfiguration.getAuthHeader(),
                    dvaRequest
            );
            System.out.println("DEBUG: Customer response: " + customerResponse);
            System.out.println("DEBUG: Customer code: " + customerResponse.getData().getCustomerCode());
            organization.setDvaAccountNumber(dvaResponse.getData().getAccountNumber());

            organization.setDvaBankName(dvaResponse.getData().getBank().getName());

            repository.save(organization);
        }

        return ApprovalMap.mapApprovalResponse(savedOrganization, savedUser);
    }

    @Override
    public List<OrganizationOnlyResponse> viewAll() {
        return repository.findAll()
                .stream()
                .map(map::mapResponse)
                .collect(Collectors.toList());
    }


    @Override
    public OrganizationOnlyResponse viewById(long id) throws OrganizationDoesNotExist {
        Optional<Organization> organization = repository.findById(id);
        if (organization.isEmpty()) {
            throw new OrganizationDoesNotExist("There's no organization with the specified id");
        }
        return map.mapResponse(organization.get());
    }

    @Override
    public OrganizationOnlyResponse updateOrganization(Long orgId, UpdateOrganizationRequest request) throws OrganizationDoesNotExist {
        Organization org = repository.findById(orgId)
                .orElseThrow(() -> new OrganizationDoesNotExist("Organization not found"));


        if (request.getName() != null) org.setName(request.getName());
        if (request.getOrganizationEmail() != null) org.setOrganizationEmail(request.getOrganizationEmail());
        if (request.getRegistrationNumber() != null) org.setRegistrationNumber(request.getRegistrationNumber());
        if (request.getPhoneNumber() != null) org.setPhoneNumber(request.getPhoneNumber());
        if (request.getOrganizationStatus() != null) {
            org.setOrganizationStatus(request.getOrganizationStatus());
            if (request.getOrganizationStatus() == OrganizationStatus.DISABLED) {
                List<User> users = userRepository.findAllByOrganization(org);
                users.forEach(u -> u.setStatus(UserStatus.INACTIVE));
                userRepository.saveAll(users);
            }
        }

        return map.mapResponse(repository.save(org));
    }

}

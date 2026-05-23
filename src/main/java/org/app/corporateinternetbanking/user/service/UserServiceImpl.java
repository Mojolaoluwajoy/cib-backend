package org.app.corporateinternetbanking.user.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.email.EmailSenderService;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.security.JwtService;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.dto.*;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.app.corporateinternetbanking.user.exceptions.*;
import org.app.corporateinternetbanking.user.utils.mapper.PasswordResetResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.app.corporateinternetbanking.user.utils.mapper.UserMap.mapResponse;

@Slf4j
@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    // private final User user;
    PasswordEncoder passwordEncoder;
    @Autowired
    EmailSenderService senderService;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private JwtService jwtService;

    @Override
    public String sendInvitationTokenToUser(InvitationRequest invitationRequest) throws UserNotFound, UserAlreadyRegistered {
        User admin = getCurrentUser();
        org.app.corporateinternetbanking.organization.domain.entity.Organization organization
                = admin.getOrganization();

        if (repository.existsByEmail(invitationRequest.getUserEmail())) {
            throw new UserAlreadyRegistered("User already exists");
        }
        User user = new User();
        user.setEmail(invitationRequest.getUserEmail());
        user.setRole(invitationRequest.getRole());
        user.setOrganization(organization);
        user.setStatus(UserStatus.INACTIVE);
        repository.save(user);
        String token = jwtService.generateEmailToken(invitationRequest.getUserEmail());
        sendMail(invitationRequest, token);
        return invitationRequest.getUserEmail();
    }

    @Override
    public UserResponse createUserWithToken(UserRegistrationRequest request) throws UserAlreadyRegistered, OrganizationDoesNotExist, TokenExpiredOrInvalid, SuperAdminAlreadyExists, UserNotFound {

        if (!jwtService.isEmailTokenValid(request.getToken())) {
            throw new TokenExpiredOrInvalid("Token expired or its invalid");
        }
        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFound("This user has not been invited"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new UserAlreadyRegistered("This user has already completed registration");
        }

        if (repository.findByNin(request.getNin()).isPresent()) {
            throw new UserAlreadyRegistered("This NIN is already registered");
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNin(request.getNin());
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = repository.save(user);
        return mapResponse(savedUser);
    }

    @Override
    public List<UserResponse> ViewAllUsers() {
        List<User> users = repository.findAll();
        List<UserResponse> userList = new ArrayList<>();
        for (User savedUser : users) {
            UserResponse userResponse = new UserResponse();
            userResponse.setUserId(savedUser.getUserId());
            userResponse.setFirstName(savedUser.getFirstName());
            userResponse.setLastName(savedUser.getLastName());
            userResponse.setNin(savedUser.getNin());
            userResponse.setEmail(savedUser.getEmail());
            userResponse.setRole(savedUser.getRole());
            userResponse.setStatus(savedUser.getStatus());
            userList.add(userResponse);

        }
        return userList;
    }

    @Override
    public Page<User> viewByStatus(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null) {
            return repository.findByStatus(status, pageable);
        }
        return repository.findAll(pageable);
    }

    @Override
    public PasswordResetResponse resetPassword(PasswordResetRequest passwordResetRequest) throws IncorrectPassword, InvalidEmail {
        User user = repository.findByEmail(passwordResetRequest.getEmail())
                .orElseThrow(() -> new InvalidEmail("Email not found"));
        if (passwordEncoder.matches(passwordResetRequest.getOldPassword(), user.getPassword())) {
            String newPassword = passwordEncoder.encode(passwordResetRequest.getNewPassword());
            user.setPassword(newPassword);
            repository.save(user);
        } else {
            throw new IncorrectPassword("The old password you entered is incorrect");
        }
        return PasswordResetResponseMap.resetResponseMap(user);
    }

    @Override
    public String resetForgottenPassword(ForgotPasswordRequest forgotPasswordRequest) throws InvalidEmail, TokenExpiredOrInvalid {
        User user = repository.findByEmail(forgotPasswordRequest.getEmail())
                .orElseThrow(() -> new InvalidEmail("Email not found"));
        if (!jwtService.isEmailTokenValid(forgotPasswordRequest.getToken())) {
            throw new TokenExpiredOrInvalid("Token expired or its invalid");
        }
        user.setPassword(forgotPasswordRequest.getNewPassword());
        repository.save(user);
        return "Password reset successful";
    }


    @Override
    public PasswordResetResponse sendForgotPasswordToken(String email) throws InvalidEmail {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new InvalidEmail("Email not found"));
        String token = jwtService.generateEmailToken(email);
        senderService.sendEmail(user.getEmail(), "Password reset token", "Your password reset token is: \n" + token);
        return PasswordResetResponseMap.resetResponseMap(user);
    }


    public void sendMail(InvitationRequest invitationRequest, String token) {

        senderService.sendEmail(invitationRequest.getUserEmail(), "Account Creation Token", "Your verification token is: \n" + token);


    }

    private User getCurrentUser() throws UserNotFound {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return repository.findByEmail(email)
                .orElseThrow(() -> new UserNotFound("User not found"));
    }
}

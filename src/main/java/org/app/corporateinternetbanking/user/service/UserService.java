package org.app.corporateinternetbanking.user.service;

import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.dto.*;
import org.app.corporateinternetbanking.user.exceptions.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {


    UserResponse createUserWithToken(UserRegistrationRequest request) throws UserAlreadyRegistered, TokenExpiredOrInvalid, OrganizationDoesNotExist, SuperAdminAlreadyExists, UserNotFound;

    List<UserResponse> ViewAllUsers();

    String sendInvitationTokenToUser(InvitationRequest request) throws UserNotFound, UserAlreadyRegistered;


    Page<User> viewByStatus(int page, int size, String status);

    PasswordResetResponse resetPassword(PasswordResetRequest passwordResetRequest) throws IncorrectPassword, InvalidEmail;

    String resetForgottenPassword(ForgotPasswordRequest forgotPasswordRequest) throws InvalidEmail, TokenExpiredOrInvalid;

    PasswordResetResponse sendForgotPasswordToken(String email) throws InvalidEmail;

    UserResponse updateUserProfile(Long userId, UpdateProfileRequest request) throws UserNotFound, UnauthorizedAccess, UserAlreadyRegistered;

    UserResponse updateProfile(UpdateProfileRequest request) throws UserNotFound, UserAlreadyRegistered;

}

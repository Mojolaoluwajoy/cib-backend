package org.app.corporateinternetbanking.user.service;

import java.util.List;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.user.dto.*;
import org.app.corporateinternetbanking.user.exceptions.*;
import org.springframework.data.domain.Page;

public interface UserService {

    UserResponse createUserWithToken(UserRegistrationRequest request)
            throws UserAlreadyRegistered, TokenExpiredOrInvalid, OrganizationDoesNotExist, SuperAdminAlreadyExists,
                    UserNotFound;

    List<UserResponse> ViewAll();

    String sendInvitationTokenToUser(InvitationRequest request)
            throws UserNotFound, UserAlreadyRegistered, UnauthorizedAccess;

    Page<UserResponse> viewByStatus(int page, int size, String status) throws UserNotFound;

    PasswordResetResponse resetPassword(PasswordResetRequest passwordResetRequest)
            throws IncorrectPassword, InvalidEmail;

    String resetForgottenPassword(ForgotPasswordRequest forgotPasswordRequest)
            throws InvalidEmail, TokenExpiredOrInvalid;

    void sendForgotPasswordToken(String email);

    UserResponse updateUserProfile(Long userId, UpdateProfileRequest request)
            throws UserNotFound, UnauthorizedAccess, UserAlreadyRegistered;

    UserResponse updateProfile(UpdateProfileRequest request) throws UserNotFound, UserAlreadyRegistered;
}

package org.app.corporateinternetbanking.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.app.corporateinternetbanking.commons.response.GenericResponse;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.user.dto.InvitationRequest;
import org.app.corporateinternetbanking.user.dto.UpdateProfileRequest;
import org.app.corporateinternetbanking.user.dto.UserRegistrationRequest;
import org.app.corporateinternetbanking.user.dto.UserResponse;
import org.app.corporateinternetbanking.user.exceptions.*;
import org.app.corporateinternetbanking.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Users API", description = "Handles users")
public class UserController {
    @Autowired
    UserService service;

    @Operation(summary = "Send user creation token invitation to email")
    @PostMapping("/invitation")
    public ResponseEntity<GenericResponse> sendInvitation(@RequestBody InvitationRequest request) throws UserNotFound, UserAlreadyRegistered {
        String email = service.sendInvitationTokenToUser(request);
        return new ResponseEntity<>(GenericResponse.success(email, "token successfully sent"), HttpStatus.CREATED);
    }

    @Operation(summary = "Create a user with token from admin")
    @PostMapping("/create")
    public ResponseEntity<GenericResponse> createUser(@RequestBody UserRegistrationRequest request) throws UserAlreadyRegistered, UnauthorizedAccess, OrganizationDoesNotExist, TokenExpiredOrInvalid, SuperAdminAlreadyExists, UserNotFound {
        UserResponse response = service.createUserWithToken(request);
        return new ResponseEntity<>(GenericResponse.success(response, "user registration successful"), HttpStatus.CREATED);
    }

    @Operation(summary = "view all users")
    @GetMapping("/viewAll")
    public ResponseEntity<GenericResponse> viewAlUsers() {
        List<UserResponse> users = service.ViewAllUsers();
        return new ResponseEntity<>(GenericResponse.success(users, "Users found"), HttpStatus.OK);
    }

    @Operation(summary = "Pageable view of users")
    @GetMapping("/users")
    public ResponseEntity<GenericResponse> getUsers(@RequestParam int page, @RequestParam int size, @RequestParam(required = false) String status) {
        return new ResponseEntity<>(GenericResponse.success(service.viewByStatus(page, size, status), "Users found"), HttpStatus.OK);

    }

    @Operation(summary = "To update one's profile")
    @PutMapping("/profile")
    public ResponseEntity<GenericResponse> updateOwnProfile(
            @RequestBody UpdateProfileRequest request) throws UserNotFound, UserAlreadyRegistered {
        return ResponseEntity.ok(GenericResponse.success(
                service.updateProfile(request), "Profile updated"));
    }

    @Operation(summary = "To update other users profile")
    @PutMapping("/{userId}/profile")
    public ResponseEntity<GenericResponse> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody UpdateProfileRequest request) throws UserNotFound, UserAlreadyRegistered, UnauthorizedAccess {
        return ResponseEntity.ok(GenericResponse.success(
                service.updateUserProfile(userId, request), "Profile updated"));
    }


}

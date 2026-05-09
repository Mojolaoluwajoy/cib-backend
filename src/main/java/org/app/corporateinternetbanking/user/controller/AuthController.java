package org.app.corporateinternetbanking.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.app.corporateinternetbanking.commons.response.GenericResponse;
import org.app.corporateinternetbanking.security.AuthenticationService;
import org.app.corporateinternetbanking.user.dto.ForgotPasswordRequest;
import org.app.corporateinternetbanking.user.dto.PasswordResetRequest;
import org.app.corporateinternetbanking.user.dto.authDto.LoginRequest;
import org.app.corporateinternetbanking.user.dto.authDto.LoginResponse;
import org.app.corporateinternetbanking.user.exceptions.IncorrectPassword;
import org.app.corporateinternetbanking.user.exceptions.InvalidEmail;
import org.app.corporateinternetbanking.user.exceptions.TokenExpiredOrInvalid;
import org.app.corporateinternetbanking.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication API", description = "Handles endpoints that doesn't require authentication")
public class AuthController {
    @Autowired
    UserService service;
    @Autowired
    AuthenticationService authenticationService;

    @Operation(summary = "Users login")
    @PostMapping("/login")
    public ResponseEntity<GenericResponse> authenticate(@RequestBody LoginRequest request) {
        LoginResponse response = authenticationService.Authenticate(request);
        return new ResponseEntity<>(GenericResponse.success(response, "Authentication successful"), HttpStatus.OK);
    }

    @Operation(summary = "change password")
    @PostMapping("/password/reset")
    public ResponseEntity<GenericResponse> resetPassword(@RequestBody PasswordResetRequest resetRequest) throws IncorrectPassword, InvalidEmail {
        return new ResponseEntity<>(GenericResponse.success(service.resetPassword(resetRequest), "Your password has been reset"), HttpStatus.CREATED);
    }

    @Operation(summary = "password reset token")
    @PostMapping("/password/token")
    public ResponseEntity<GenericResponse> resetToken(@RequestBody String email) throws InvalidEmail {
        return new ResponseEntity<>(GenericResponse.success(service.sendForgotPasswordToken(email), "A token has bee sent to your email"), HttpStatus.CREATED);
    }

    @Operation(summary = "Reset forgotten password")
    @PostMapping("/forgotten/password")
    public ResponseEntity<GenericResponse> resetForgottenPassword(@RequestBody ForgotPasswordRequest resetRequest) throws InvalidEmail, TokenExpiredOrInvalid {
        return new ResponseEntity<>(GenericResponse.success(service.resetForgottenPassword(resetRequest), "Your password has been reset"), HttpStatus.CREATED);
    }


}

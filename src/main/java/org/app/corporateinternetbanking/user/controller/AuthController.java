package org.app.corporateinternetbanking.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @Autowired
    ObjectMapper objectMapper;

    @SecurityRequirements
    @Operation(summary = "Users login")
    @PostMapping("/login")
    public ResponseEntity<GenericResponse> authenticate(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authenticationService.Authenticate(request);
        return new ResponseEntity<>(GenericResponse.success(response, "Authentication successful"), HttpStatus.OK);
    }

    @Operation(summary = "change password")
    @PostMapping("/password/reset")
    public ResponseEntity<GenericResponse> resetPassword(@Valid @RequestBody PasswordResetRequest resetRequest)
            throws IncorrectPassword, InvalidEmail {
        return new ResponseEntity<>(
                GenericResponse.success(service.resetPassword(resetRequest), "Your password has been reset"),
                HttpStatus.CREATED);
    }

    @SecurityRequirements
    @Operation(summary = "password reset token")
    @PostMapping("/password/token")
    public ResponseEntity<GenericResponse> resetToken(@RequestBody String body) throws InvalidEmail {
        service.sendForgotPasswordToken(emailFrom(body));
        return new ResponseEntity<>(
                GenericResponse.success(null, "If an account exists for that email, a reset token has been sent to it"),
                HttpStatus.CREATED);
    }

    @SecurityRequirements
    @Operation(summary = "Reset forgotten password")
    @PostMapping("/forgotten/password")
    public ResponseEntity<GenericResponse> resetForgottenPassword(
            @Valid @RequestBody ForgotPasswordRequest resetRequest) throws InvalidEmail, TokenExpiredOrInvalid {
        return new ResponseEntity<>(
                GenericResponse.success(service.resetForgottenPassword(resetRequest), "Your password has been reset"),
                HttpStatus.CREATED);
    }

    // Accepts {"email": "..."} as well as the bare address (or JSON string) this endpoint originally took.
    private String emailFrom(String body) throws InvalidEmail {
        String email = body.trim();
        try {
            if (email.startsWith("{")) {
                JsonNode field = objectMapper.readTree(email).get("email");
                email = field == null || field.isNull() ? "" : field.asText().trim();
            } else if (email.startsWith("\"")) {
                email = objectMapper.readValue(email, String.class).trim();
            }
        } catch (JsonProcessingException e) {
            throw new InvalidEmail("Send the email as {\"email\": \"you@example.com\"}");
        }
        if (email.isEmpty()) {
            throw new InvalidEmail("email is required");
        }
        return email;
    }
}

package org.app.corporateinternetbanking.commons.exception.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import feign.FeignException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.commons.response.GenericResponse;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotActive;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.notification.exceptions.NoPendingNotifications;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationAlreadyExist;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationAlreadyProcessed;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.transaction.exceptions.*;
import org.app.corporateinternetbanking.user.exceptions.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Every error leaves the API as a GenericResponse. Spring MVC's own exceptions (malformed JSON, failed
// validation, unknown route, wrong method, ...) keep their status codes via ResponseEntityExceptionHandler;
// anything nobody anticipated becomes a 500 whose details stay in the log.
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        String message = errors.entrySet().stream()
                .map(error -> error.getKey() + " " + error.getValue())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);

        GenericResponse body = GenericResponse.failed("Validation failed: " + message);
        body.setData(errors);
        return new ResponseEntity<>(body, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = "Malformed request body";
        // e.g. an unknown enum constant or text where a number is expected: name the offending field
        if (exception.getCause() instanceof InvalidFormatException invalid
                && !invalid.getPath().isEmpty()) {
            String field = invalid.getPath().get(invalid.getPath().size() - 1).getFieldName();
            message = "Invalid value for " + field;
        }
        log.warn("{}: {}", message, exception.getMostSpecificCause().getMessage());
        return new ResponseEntity<>(GenericResponse.failed(message), headers, status);
    }

    // Wraps the remaining Spring MVC exceptions (404, 405, 415, missing parameters, ...) in a GenericResponse
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = body instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail()
                : exception.getMessage();
        log.warn("Request rejected ({}): {}", status.value(), message);
        return new ResponseEntity<>(GenericResponse.failed(message), headers, status);
    }

    // Wrong email or password, or an account that has not been activated yet
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GenericResponse> handleAuthentication(AuthenticationException exception) {
        log.warn("Authentication failed: {}", exception.getMessage());
        String message =
                exception instanceof DisabledException ? "This account is not active yet" : "Invalid email or password";
        return new ResponseEntity<>(GenericResponse.failed(message), HttpStatus.UNAUTHORIZED);
    }

    // Thrown by @PreAuthorize checks inside controllers
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GenericResponse> handleAccessDenied(AccessDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        return new ResponseEntity<>(
                GenericResponse.failed("You do not have permission to perform this action"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return new ResponseEntity<>(
                GenericResponse.failed("Something went wrong. Please try again later."),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UserAlreadyRegistered.class)
    public ResponseEntity<GenericResponse> handleUserAlreadyRegistered(UserAlreadyRegistered registered) {
        log.error("Error registering user: {}", registered.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(registered.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedAccess.class)
    public ResponseEntity<GenericResponse> handleNotAnAdminException(UnauthorizedAccess exception) {
        log.error("Unauthorized access: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccountDoesNotExist.class)
    public ResponseEntity<GenericResponse> handleAccountDoesNotExist(AccountDoesNotExist exception) {
        log.error("Account not found: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrganizationDoesNotExist.class)
    public ResponseEntity<GenericResponse> handleOrganizationDoesNotExist(OrganizationDoesNotExist exception) {
        log.error("Organization not found: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidAmount.class)
    public ResponseEntity<GenericResponse> handleInvalidAmount(InvalidAmount exception) {
        log.error("Invalid amount entered: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidStatus.class)
    public ResponseEntity<GenericResponse> handleInvalidStatus(InvalidStatus exception) {
        log.error("Invalid status entered: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TransactionAlreadyProcessed.class)
    public ResponseEntity<GenericResponse> handleTransactionAlreadyProcessed(TransactionAlreadyProcessed exception) {
        log.error("Transaction already processed: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TransactionDoesNotExist.class)
    public ResponseEntity<GenericResponse> handleTransactionDoesNotExist(TransactionDoesNotExist exception) {
        log.error("Transaction does not exist: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnsupportedTransactionType.class)
    public ResponseEntity<GenericResponse> handleUnsupportedTransactionType(UnsupportedTransactionType exception) {
        log.error("Unsupported transaction type: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotFound.class)
    public ResponseEntity<GenericResponse> handleUserNotFound(UserNotFound exception) {
        log.error("user not found: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenExpiredOrInvalid.class)
    public ResponseEntity<GenericResponse> handleTokenExpiredOrInvalid(TokenExpiredOrInvalid exception) {
        log.error("Invalid token: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrganizationAlreadyProcessed.class)
    public ResponseEntity<GenericResponse> handleOrganizationAlreadyProcessed(OrganizationAlreadyProcessed exception) {
        log.error("This organization has been processed: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CurrencyNotFound.class)
    public ResponseEntity<GenericResponse> handleCurrencyNotFound(CurrencyNotFound exception) {
        log.error("Currency not found: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoPendingTransactionFound.class)
    public ResponseEntity<GenericResponse> handleNoPendingTransactionFound(NoPendingTransactionFound exception) {
        log.error("No pending Transaction(s): {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateTransaction.class)
    public ResponseEntity<GenericResponse> handleDuplicateTransaction(DuplicateTransaction exception) {
        log.error("Duplicate Transaction: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CurrencyNotActive.class)
    public ResponseEntity<GenericResponse> handleCurrencyNotActive(CurrencyNotActive exception) {
        log.error("Inactive currency: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InsufficientBalance.class)
    public ResponseEntity<GenericResponse> handleInsufficientBalance(InsufficientBalance exception) {
        log.error("Insufficient balance: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoPendingNotifications.class)
    public ResponseEntity<GenericResponse> handleNoPendingNotifications(NoPendingNotifications exception) {
        log.error("No pending notifications: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IncorrectPassword.class)
    public ResponseEntity<GenericResponse> handleIncorrectPassword(IncorrectPassword exception) {
        log.error("Incorrect password: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidEmail.class)
    public ResponseEntity<GenericResponse> handleInvalidEmail(InvalidEmail exception) {
        log.error("Invalid Email: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrganizationAlreadyExist.class)
    public ResponseEntity<GenericResponse> handleOrganizationAlreadyExist(OrganizationAlreadyExist exception) {
        log.error("Organization already exist: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SuperAdminAlreadyExists.class)
    public ResponseEntity<GenericResponse> handleSuperAdminAlreadyExists(SuperAdminAlreadyExists exception) {
        log.error("Not allowed: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidSignature.class)
    public ResponseEntity<GenericResponse> handleInvalidSignature(InvalidSignature exception) {
        log.error("Invalid signature: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidAccount.class)
    public ResponseEntity<GenericResponse> handleInvalidAccount(InvalidAccount exception) {
        log.error("Invalid account: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IsNull.class)
    public ResponseEntity<GenericResponse> handleIsNull(IsNull exception) {
        log.error("Is null: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotApproved.class)
    public ResponseEntity<GenericResponse> handleNotApproved(NotApproved exception) {
        log.error("Not approved: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    // A concurrent request with the same idempotency key lost the race on the unique constraint.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.error(
                "Data integrity violation: {}", exception.getMostSpecificCause().getMessage());
        return new ResponseEntity<>(
                GenericResponse.failed("Conflicting request: this transaction may already exist"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<GenericResponse> handleFeignException(FeignException exception) {
        log.error(
                "External provider error ({} {}): {}",
                exception.status(),
                exception.request() != null ? exception.request().url() : "",
                exception.contentUTF8());
        return new ResponseEntity<>(
                GenericResponse.failed("Payment provider error: " + exception.contentUTF8()), HttpStatus.BAD_GATEWAY);
    }
}

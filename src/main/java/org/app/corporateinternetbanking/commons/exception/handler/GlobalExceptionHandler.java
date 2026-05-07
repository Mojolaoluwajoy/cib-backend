package org.app.corporateinternetbanking.commons.exception.handler;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(UserAlreadyRegistered.class)
    public ResponseEntity<GenericResponse> handleUserAlreadyRegistered(UserAlreadyRegistered registered) {
        log.error("Error registering user: {}", registered.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(registered.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedAccess.class)
    public ResponseEntity<GenericResponse> handleNotAnAdminException(UnauthorizedAccess exception) {
        log.error("Not a super admin: {}", exception.getMessage());
        return new ResponseEntity<>(GenericResponse.failed(exception.getMessage()), HttpStatus.BAD_REQUEST);
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


}

package org.app.corporateinternetbanking.transaction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.commons.response.GenericResponse;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalRequest;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalResponse;
import org.app.corporateinternetbanking.transaction.dto.TransactionResponse;
import org.app.corporateinternetbanking.transaction.dto.TransferRequest;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.transaction.exceptions.*;
import org.app.corporateinternetbanking.transaction.service.ApprovalService;
import org.app.corporateinternetbanking.transaction.service.TransactionServiceImpl;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactions API", description = "Handles transactions")
public class TransactionController {
    private final ApprovalService approvalService;

    @Autowired
    TransactionServiceImpl service;

    @Operation(summary = "Initiate transaction")
    @PostMapping("/initiate")
    public ResponseEntity<GenericResponse> initiateTransaction(
            @Valid @RequestBody TransferRequest request, HttpServletRequest servletRequest)
            throws InvalidAmount, AccountDoesNotExist, UserNotFound, UnauthorizedAccess, DuplicateTransaction,
                    InsufficientBalance, InvalidAccount, IsNull, UnsupportedTransactionType {
        log.info("URI: " + servletRequest.getRequestURI());
        if (request.getType() == TransactionType.EXTERNAL_DEPOSIT) {
            throw new UnsupportedTransactionType(
                    "External deposits cannot be initiated manually; they arrive through the Paystack webhook");
        }
        TransactionResponse response = service.initiateTransaction(request);
        return new ResponseEntity<>(
                GenericResponse.success(response, "Transaction successfully initiated...waiting for approval"),
                HttpStatus.OK);
    }

    @Operation(summary = "Approve transaction")
    @PostMapping("/approve")
    public ResponseEntity<GenericResponse> grantApproval(@Valid @RequestBody TransactionApprovalRequest request)
            throws TransactionAlreadyProcessed, TransactionDoesNotExist, InvalidStatus, UnsupportedTransactionType,
                    UserNotFound, UnauthorizedAccess, InvalidAmount, AccountDoesNotExist, CurrencyNotFound,
                    InsufficientBalance, IsNull {
        TransactionApprovalResponse response = approvalService.approveInternalTransaction(request);
        return new ResponseEntity<>(GenericResponse.success(response, "Transaction Processed"), HttpStatus.OK);
    }

    @Operation(summary = "View all pending transactions")
    @GetMapping("pending")
    public ResponseEntity<GenericResponse> viewPendingTransactions() throws UserNotFound {
        List<TransactionResponse> response = service.viewPendingTransactions();
        return new ResponseEntity<>(GenericResponse.success(response, "Pending transactions found"), HttpStatus.OK);
    }

    @Operation(summary = "View all transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MAKER', 'APPROVER')")
    @GetMapping("/transactions")
    public ResponseEntity<GenericResponse> getTransactions(
            @RequestParam int page, @RequestParam int size, @RequestParam(required = false) String status)
            throws UserNotFound {
        return new ResponseEntity<>(
                GenericResponse.success(service.getTransactions(page, size, status), "Transactions found"),
                HttpStatus.OK);
    }

    @Operation(summary = "get all transaction by an organization")
    @GetMapping("/organization/{orgId}")
    public ResponseEntity<GenericResponse> getOrgTransactions(
            @PathVariable Long orgId, @RequestParam int page, @RequestParam int size)
            throws OrganizationDoesNotExist, UserNotFound, UnauthorizedAccess {
        return ResponseEntity.ok(GenericResponse.success(
                service.getTransactionsByOrganization(orgId, page, size), "Transactions found"));
    }
}

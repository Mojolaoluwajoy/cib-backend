package org.app.corporateinternetbanking.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.app.corporateinternetbanking.account.dto.AccountRequest;
import org.app.corporateinternetbanking.account.dto.AccountResponse;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.service.AccountServiceImpl;
import org.app.corporateinternetbanking.commons.response.GenericResponse;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotActive;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Account API", description = "Handles accounts")
public class AccountController {
    @Autowired
    AccountServiceImpl service;

    @Operation(summary = "Create account")
    @PostMapping("/create")
    public ResponseEntity<GenericResponse> createAccount(@RequestBody AccountRequest request) throws OrganizationDoesNotExist, UserNotFound, CurrencyNotFound, CurrencyNotActive {
        AccountResponse response = service.createAccount(request);
        return new ResponseEntity<>(GenericResponse.success(response, "Account successfully registered"), HttpStatus.OK);
    }

    @Operation(summary = "Find account by id")
    @PostMapping("/find")
    public ResponseEntity<GenericResponse> findById(@RequestBody Long id) throws AccountDoesNotExist {
        AccountResponse response = null;
        response = service.findById(id);
        return new ResponseEntity<>(GenericResponse.success(response, "Account with the specified id found"), HttpStatus.OK);
    }

    @Operation(summary = "View all accounts")
    @GetMapping("/all")
    public ResponseEntity<GenericResponse> viewAll() {
        List<AccountResponse> response = service.viewAll();
        return new ResponseEntity<>(GenericResponse.success(response, "Accounts found!"), HttpStatus.OK);
    }

    // In AccountController
    @GetMapping("/organization/{orgId}")
    public ResponseEntity<GenericResponse> getOrgAccounts(@PathVariable Long orgId) throws OrganizationDoesNotExist {
        return ResponseEntity.ok(GenericResponse.success(
                service.getAccountsByOrganization(orgId), "Accounts found"));
    }
}

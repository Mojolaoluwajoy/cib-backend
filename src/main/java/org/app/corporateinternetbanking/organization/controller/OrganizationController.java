package org.app.corporateinternetbanking.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.app.corporateinternetbanking.commons.response.GenericResponse;
import org.app.corporateinternetbanking.organization.dto.*;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationAlreadyExist;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationAlreadyProcessed;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.organization.service.OrganizationService;
import org.app.corporateinternetbanking.user.exceptions.UserAlreadyRegistered;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/organizations")
@Tag(name = "Organization API", description = "Handles organizations")
public class OrganizationController {
    @Autowired
    OrganizationService service;

    @Operation(summary = "Send organization and admin registration request")
    @PostMapping("/create")
    public ResponseEntity<GenericResponse> createOrganization(@RequestBody OrganizationRequest request) throws UserAlreadyRegistered, OrganizationAlreadyExist {
        OrganizationRegistrationResponse response = service.registerOrganization(request);
        return new ResponseEntity<>(GenericResponse.success(response, "Organization registration complete,awaiting approval"), HttpStatus.OK);
    }

    @Operation(summary = "Approve organization and admin registration request")
    @PostMapping("/approve")
    public ResponseEntity<GenericResponse> approveOrganization(@RequestBody OrganizationApprovalRequest request) throws UserAlreadyRegistered, UserNotFound, OrganizationAlreadyProcessed, OrganizationDoesNotExist {
        OrganizationApprovalResponse response = service.processOrganizationRegistration(request);
        return new ResponseEntity<>(GenericResponse.success(response, "Organization successfully processed"), HttpStatus.OK);
    }

    @Operation(summary = "Find an organization by id")
    @PostMapping("/findBy")
    public ResponseEntity<GenericResponse> findById(@RequestBody Long id) throws OrganizationDoesNotExist {
        OrganizationOnlyResponse response;
        response = service.viewById(id);
        return new ResponseEntity<>(GenericResponse.success(response, "Organization with the specified id found"), HttpStatus.OK);
    }

    @Operation(summary = "View all organizations")
    @GetMapping("/viewAll")
    public ResponseEntity<GenericResponse> viewAll() {
        List<OrganizationOnlyResponse> response = service.viewAll();
        return new ResponseEntity<>(GenericResponse.success(response, "Organization found!"), HttpStatus.OK);
    }
}

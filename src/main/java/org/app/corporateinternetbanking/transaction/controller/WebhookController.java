package org.app.corporateinternetbanking.transaction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.commons.response.GenericResponse;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.transaction.dto.PaystackWebhookRequest;
import org.app.corporateinternetbanking.transaction.exceptions.InsufficientBalance;
import org.app.corporateinternetbanking.transaction.exceptions.InvalidSignature;
import org.app.corporateinternetbanking.transaction.exceptions.NotApproved;
import org.app.corporateinternetbanking.transaction.exceptions.TransactionDoesNotExist;
import org.app.corporateinternetbanking.transaction.service.WebhookHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhook", description = "Handles webhook")
public class WebhookController {
    private final WebhookHandler webhookHandler;

    @Operation(summary = "It handles webhook")
    @PostMapping("/paystack")
    public ResponseEntity<GenericResponse> handleWebhook(@RequestBody PaystackWebhookRequest webhookRequest) throws TransactionDoesNotExist, AccountDoesNotExist, InvalidSignature, NotApproved, InsufficientBalance, OrganizationDoesNotExist {
        return new ResponseEntity<>(GenericResponse.success(webhookHandler.handleWebhook(webhookRequest), "Received"), HttpStatus.OK);
    }

}

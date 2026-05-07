package org.app.corporateinternetbanking.transaction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.commons.response.GenericResponse;
import org.app.corporateinternetbanking.transaction.dto.FundRequest;
import org.app.corporateinternetbanking.transaction.dto.PayoutRequest;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.transaction.exceptions.DuplicateTransaction;
import org.app.corporateinternetbanking.transaction.exceptions.InsufficientBalance;
import org.app.corporateinternetbanking.transaction.exceptions.InvalidAmount;
import org.app.corporateinternetbanking.transaction.exceptions.IsNull;
import org.app.corporateinternetbanking.transaction.service.PaymentService;
import org.app.corporateinternetbanking.transaction.service.RecipientService;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/external/transaction")
@RequiredArgsConstructor
@Tag(name = "External Transactions", description = "To carryout external payout and funding")
public class ExternalTransactionController {
    private final PaymentService paymentService;
    private final RecipientService recipientService;

    @Operation(summary = "It fund sthe system account from an external account(paystack")
    @PostMapping("/fund")
    public ResponseEntity<GenericResponse> fund(@RequestBody FundRequest fundRequest) throws UserNotFound, InvalidAccount, InvalidAmount, InsufficientBalance, UnauthorizedAccess, IsNull, DuplicateTransaction, AccountDoesNotExist {
        fundRequest.setType(TransactionType.EXTERNAL_FUNDING);
        Map<String, Object> map = paymentService.initializeFunding(fundRequest);

        return new ResponseEntity<>(GenericResponse.success(map, "funding initiated and awaiting response"), HttpStatus.OK);
    }

    @Operation(summary = "It initiates a payout from the system account to an external one")
    @PostMapping("/payout")
    public ResponseEntity<GenericResponse> payout(@RequestBody PayoutRequest payoutRequest) throws UserNotFound, InvalidAccount, InvalidAmount, InsufficientBalance, UnauthorizedAccess, IsNull, DuplicateTransaction, AccountDoesNotExist {
        payoutRequest.setType(TransactionType.EXTERNAL_PAYOUT);
        return new ResponseEntity<>(GenericResponse.success(recipientService.requestPayOut(payoutRequest), "payment request initialized and awaiting approval"), HttpStatus.OK);
    }
}

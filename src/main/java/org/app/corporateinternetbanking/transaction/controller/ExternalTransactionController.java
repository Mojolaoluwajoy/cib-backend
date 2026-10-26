package org.app.corporateinternetbanking.transaction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.commons.response.GenericResponse;
import org.app.corporateinternetbanking.transaction.dto.AccountNameResponse;
import org.app.corporateinternetbanking.transaction.dto.BankResponse;
import org.app.corporateinternetbanking.transaction.dto.FundRequest;
import org.app.corporateinternetbanking.transaction.dto.PayoutRequest;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.transaction.exceptions.DuplicateTransaction;
import org.app.corporateinternetbanking.transaction.exceptions.InsufficientBalance;
import org.app.corporateinternetbanking.transaction.exceptions.InvalidAmount;
import org.app.corporateinternetbanking.transaction.exceptions.IsNull;
import org.app.corporateinternetbanking.transaction.service.BankLookupService;
import org.app.corporateinternetbanking.transaction.service.PaymentService;
import org.app.corporateinternetbanking.transaction.service.RecipientService;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/external/transaction")
@RequiredArgsConstructor
@Tag(name = "External Transactions", description = "To carryout external payout and funding")
public class ExternalTransactionController {
    private final PaymentService paymentService;
    private final RecipientService recipientService;
    private final BankLookupService bankLookupService;

    @Operation(summary = "It fund the system account from an external account(paystack")
    @PostMapping("/fund")
    public ResponseEntity<GenericResponse> fund(@Valid @RequestBody FundRequest fundRequest)
            throws UserNotFound, InvalidAccount, InvalidAmount, InsufficientBalance, UnauthorizedAccess, IsNull,
                    DuplicateTransaction, AccountDoesNotExist {
        fundRequest.setType(TransactionType.EXTERNAL_FUNDING);
        Map<String, Object> map = paymentService.initializeFunding(fundRequest);

        return new ResponseEntity<>(
                GenericResponse.success(map, "funding initiated and awaiting response"), HttpStatus.OK);
    }

    @Operation(summary = "It initiates a payout from the system account to an external one")
    @PostMapping("/payout")
    public ResponseEntity<GenericResponse> payout(@Valid @RequestBody PayoutRequest payoutRequest)
            throws UserNotFound, InvalidAccount, InvalidAmount, InsufficientBalance, UnauthorizedAccess, IsNull,
                    DuplicateTransaction, AccountDoesNotExist {
        payoutRequest.setType(TransactionType.EXTERNAL_PAYOUT);
        return new ResponseEntity<>(
                GenericResponse.success(
                        recipientService.requestPayOut(payoutRequest),
                        "payment request initialized and awaiting approval"),
                HttpStatus.OK);
    }

    @Operation(summary = "Lists the banks a payout can be sent to")
    @GetMapping("/banks")
    public ResponseEntity<GenericResponse> banks() {
        List<BankResponse> banks = bankLookupService.listBanks();
        return new ResponseEntity<>(GenericResponse.success(banks, "banks retrieved"), HttpStatus.OK);
    }

    @Operation(summary = "Looks up the account holder's name for a payout recipient")
    @GetMapping("/banks/resolve")
    public ResponseEntity<GenericResponse> resolveAccount(
            @RequestParam String accountNumber, @RequestParam String bankCode) throws InvalidAccount {
        AccountNameResponse response = bankLookupService.resolveAccountName(accountNumber, bankCode);
        return new ResponseEntity<>(GenericResponse.success(response, "account resolved"), HttpStatus.OK);
    }
}

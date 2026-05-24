package org.app.corporateinternetbanking.transaction.service;

import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.account.service.AccountService;
import org.app.corporateinternetbanking.integration.paystack.PayStackClient;
import org.app.corporateinternetbanking.transaction.dto.FundRequest;
import org.app.corporateinternetbanking.transaction.dto.TransactionResponse;
import org.app.corporateinternetbanking.transaction.dto.TransferRequest;
import org.app.corporateinternetbanking.transaction.exceptions.DuplicateTransaction;
import org.app.corporateinternetbanking.transaction.exceptions.InsufficientBalance;
import org.app.corporateinternetbanking.transaction.exceptions.InvalidAmount;
import org.app.corporateinternetbanking.transaction.exceptions.IsNull;
import org.app.corporateinternetbanking.transaction.utils.mapper.FundingMap;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final AccountService accountService;

    private final TransactionService transactionService;
    private final PayStackClient payStackClient;
    private final FundingMap fundingMap;

    @Override
    public Map<String, Object> initializeFunding(FundRequest fundRequest) throws IsNull, AccountDoesNotExist, UserNotFound, InvalidAccount, InvalidAmount, InsufficientBalance, UnauthorizedAccess, DuplicateTransaction {

        TransferRequest transferRequest = fundingMap.mapToFundingRequest(fundRequest);

        TransactionResponse response = transactionService.initiateTransaction(transferRequest);

        Map<String, Object> body = new HashMap<>();

        body.put("email", fundRequest.getEmail());
        body.put("amount", fundRequest.getAmount().multiply(BigDecimal.valueOf(100)));

        body.put("reference", response.getTransactionReference());
        return payStackClient.initializeTransaction(body);


    }
}

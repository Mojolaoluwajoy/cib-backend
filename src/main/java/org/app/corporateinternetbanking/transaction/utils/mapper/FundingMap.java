package org.app.corporateinternetbanking.transaction.utils.mapper;

import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.transaction.dto.FundRequest;
import org.app.corporateinternetbanking.transaction.dto.TransferRequest;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.app.corporateinternetbanking.user.service.UserServiceImpl;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FundingMap {
    private final UserServiceImpl userService;

    public TransferRequest mapToFundingRequest(FundRequest fundRequest) throws UserNotFound {
        TransferRequest transferRequest = new TransferRequest();

        transferRequest.setDestinationAccount(fundRequest.getAccountNumber());
        transferRequest.setAmount(fundRequest.getAmount());
        transferRequest.setType(TransactionType.EXTERNAL_FUNDING);
        transferRequest.setIdempotencyKey(fundRequest.getIdempotencyKey());
        return transferRequest;

    }
}

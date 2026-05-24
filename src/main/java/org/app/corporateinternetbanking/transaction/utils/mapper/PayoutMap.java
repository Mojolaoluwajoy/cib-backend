package org.app.corporateinternetbanking.transaction.utils.mapper;

import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.transaction.domain.entity.PayoutRecipient;
import org.app.corporateinternetbanking.transaction.dto.PayoutRequest;
import org.app.corporateinternetbanking.transaction.dto.TransferRequest;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.app.corporateinternetbanking.user.service.UserServiceImpl;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PayoutMap {
    private final UserServiceImpl userService;

    public static PayoutRecipient getPayoutRecipient(PayoutRequest payoutRequest, String code) {
        PayoutRecipient payoutRecipient = new PayoutRecipient();
        payoutRecipient.setAccountNumber(payoutRequest.getAccountNumber());
        payoutRecipient.setRecipientName(payoutRequest.getAccountName());
        payoutRecipient.setBankCode(payoutRequest.getBankCode());
        payoutRecipient.setRecipientCode(code);
        payoutRecipient.setBankName(payoutRequest.getBankName());
        payoutRecipient.setCreatedAt(LocalDateTime.now());
        return payoutRecipient;
    }

    public TransferRequest mapToPayoutRequest(PayoutRequest payoutRequest) throws UserNotFound {
        TransferRequest transferRequest = new TransferRequest();

        transferRequest.setSourceAccount(payoutRequest.getSourceAccount());
        transferRequest.setAmount(payoutRequest.getAmount());
        transferRequest.setType(TransactionType.EXTERNAL_PAYOUT);
        transferRequest.setIdempotencyKey(payoutRequest.getIdempotencyKey());
        return transferRequest;

    }
}

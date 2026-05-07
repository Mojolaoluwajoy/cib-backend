package org.app.corporateinternetbanking.transaction.utils.mapper;

import org.app.corporateinternetbanking.transaction.domain.entity.PayoutRecipient;
import org.app.corporateinternetbanking.transaction.dto.PayoutRequest;
import org.app.corporateinternetbanking.transaction.dto.TransferRequest;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;

import java.time.LocalDateTime;

public class PayoutMap {


    public static TransferRequest mapToPayoutRequest(PayoutRequest payoutRequest) {
        TransferRequest transferRequest = new TransferRequest();

        transferRequest.setSourceAccount(payoutRequest.getSourceAccount());
        transferRequest.setAmount(payoutRequest.getAmount());
        transferRequest.setCreatorId(payoutRequest.getMakerId());
        transferRequest.setType(TransactionType.EXTERNAL_PAYOUT);
        transferRequest.setIdempotencyKey(payoutRequest.getIdempotencyKey());
        return transferRequest;

    }
    

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
}

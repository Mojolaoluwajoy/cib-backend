package org.app.corporateinternetbanking.transaction.utils.mapper;

import org.app.corporateinternetbanking.account.dto.AccountNumberDto;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.dto.TransactionResponse;
import org.app.corporateinternetbanking.transaction.dto.TransferRequest;
import org.app.corporateinternetbanking.user.dto.UserIdDto;

import java.util.UUID;

public class TransactionMap {

    public static Transaction mapRequest(TransferRequest request) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setIdempotencyKey(request.getIdempotencyKey());
        transaction.setReference(UUID.randomUUID().toString());

        return transaction;
    }

    public static TransactionResponse mapResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        if (transaction.getSourceAccount() != null) {
            response.setSourceAccount(new AccountNumberDto(transaction.getSourceAccount().getAccountNumber()));
        }
        if (transaction.getDestinationAccount() != null) {
            response.setDestinationAccount(new AccountNumberDto(transaction.getDestinationAccount().getAccountNumber()));
        }
        response.setAmount(transaction.getAmount());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setTransactionReference(transaction.getIdempotencyKey());
        response.setCreator(new UserIdDto(transaction.getCreatedBy().getId()));
        return response;
    }
}

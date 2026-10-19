package org.app.corporateinternetbanking.transaction.utils.mapper;

import org.app.corporateinternetbanking.account.dto.AccountNumberDto;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalRequest;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalResponse;
import org.app.corporateinternetbanking.user.dto.UserIdDto;

public class ApprovalMap {
    public static Transaction mapApprovalRequest(TransactionApprovalRequest request) {
        Transaction transaction = new Transaction();
        transaction.setId(request.getTransactionId());
        transaction.setStatus(request.getStatus());
        return transaction;
    }

    public static TransactionApprovalResponse mapApprovalResponse(Transaction transaction) {
        TransactionApprovalResponse approvalResponse = new TransactionApprovalResponse();
        approvalResponse.setSourceAccount(
                new AccountNumberDto(transaction.getSourceAccount().getAccountNumber()));
        approvalResponse.setAmount(transaction.getAmount());
        approvalResponse.setExchangeRate(transaction.getExchangeRate());
        approvalResponse.setConvertedAmount(transaction.getConvertedAmount());
        approvalResponse.setType(transaction.getType());
        approvalResponse.setStatus(transaction.getStatus());
        approvalResponse.setApprover(new UserIdDto(transaction.getProcessedBy().getId()));
        approvalResponse.setApprovedAt(transaction.getProcessedAt());
        return approvalResponse;
    }
}

package org.app.corporateinternetbanking.transaction.service;

import static org.app.corporateinternetbanking.transaction.utils.mapper.ApprovalMap.mapApprovalResponse;

import feign.FeignException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.integration.paystack.PayStackClient;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalRequest;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalResponse;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.transaction.exceptions.*;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.stereotype.Service;

// Entry point for approvals. The balance changes happen in ApprovalProcessor's database transaction;
// the Paystack transfer is requested afterwards, outside it, so no row locks are held during the HTTP call.
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalProcessor approvalProcessor;
    private final PayStackClient payStackClient;

    public TransactionApprovalResponse approveInternalTransaction(TransactionApprovalRequest request)
            throws TransactionAlreadyProcessed, TransactionDoesNotExist, InvalidStatus, UnsupportedTransactionType,
                    UserNotFound, UnauthorizedAccess, InvalidAmount, AccountDoesNotExist, CurrencyNotFound,
                    InsufficientBalance, IsNull {
        Transaction transaction = approvalProcessor.decide(request);

        if (transaction.getType() == TransactionType.EXTERNAL_PAYOUT
                && transaction.getStatus() == TransactionStatus.APPROVED) {
            sendPayout(transaction);
        }
        return mapApprovalResponse(transaction);
    }

    private void sendPayout(Transaction transaction) {
        Map<String, Object> body = new HashMap<>();
        body.put("source", "balance");
        body.put("amount", PayStackClient.toKobo(transaction.getAmount()));
        body.put("recipient", transaction.getPayoutRecipient().getRecipientCode());
        body.put("reference", transaction.getReference());
        body.put("reason", "CIB payout " + transaction.getReference());
        log.info("Initiating Paystack transfer {} for {} kobo", transaction.getReference(), body.get("amount"));
        try {
            Map<String, Object> response = payStackClient.initiateTransfer(body);
            log.info("Paystack transfer response: {}", response.get("message"));
        } catch (FeignException e) {
            approvalProcessor.revertPayoutApproval(transaction.getId());
            throw e;
        }
    }
}

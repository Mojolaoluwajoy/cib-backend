package org.app.corporateinternetbanking.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.integration.paystack.PayStackClient;
import org.app.corporateinternetbanking.transaction.domain.entity.PayoutRecipient;
import org.app.corporateinternetbanking.transaction.domain.repository.PayoutRecipientRepository;
import org.app.corporateinternetbanking.transaction.dto.PayoutRequest;
import org.app.corporateinternetbanking.transaction.dto.TransactionResponse;
import org.app.corporateinternetbanking.transaction.dto.TransferRequest;
import org.app.corporateinternetbanking.transaction.exceptions.DuplicateTransaction;
import org.app.corporateinternetbanking.transaction.exceptions.InsufficientBalance;
import org.app.corporateinternetbanking.transaction.exceptions.InvalidAmount;
import org.app.corporateinternetbanking.transaction.exceptions.IsNull;
import org.app.corporateinternetbanking.transaction.utils.mapper.PayoutMap;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.app.corporateinternetbanking.transaction.utils.mapper.PayoutMap.getPayoutRecipient;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipientService {

    private final PayoutRecipientRepository payoutRecipientRepository;
    private final PayStackClient payStackClient;
    private final TransactionServiceImpl transactionService;
    private final PayoutMap payoutMap;

    public TransactionResponse requestPayOut(PayoutRequest payoutRequest) throws UserNotFound, InvalidAccount, InvalidAmount, InsufficientBalance, UnauthorizedAccess, IsNull, DuplicateTransaction, AccountDoesNotExist {

        PayoutRecipient recipient = createOrFetch(payoutRequest);
        TransferRequest transferRequest = payoutMap.mapToPayoutRequest(payoutRequest);
        transferRequest.setPayoutRecipientId(recipient.getId());
        return transactionService.initiateTransaction(transferRequest);

    }


    public PayoutRecipient createOrFetch(PayoutRequest payoutRequest) {

        Optional<PayoutRecipient> existing = payoutRecipientRepository.
                findByAccountNumberAndBankCode(payoutRequest.getAccountNumber(), payoutRequest.getBankCode());
        if (existing.isPresent()) {
            return existing.get();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("type", "nuban");
        body.put("account_number", payoutRequest.getAccountNumber());
        body.put("bank_code", payoutRequest.getBankCode());
        body.put("name", payoutRequest.getAccountName());
        body.put("currency", "NGN");

        log.info("Paystack body:{} ", body);

        Map<String, Object> response = payStackClient.createRecipient(body);
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        String code = (String) data.get("recipient_code");

        PayoutRecipient payoutRecipient = getPayoutRecipient(payoutRequest, code);
        return payoutRecipientRepository.save(payoutRecipient);

    }


}

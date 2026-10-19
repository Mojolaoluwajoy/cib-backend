package org.app.corporateinternetbanking.transaction.service;

import java.math.BigDecimal;
import java.util.List;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.dto.TransactionResponse;
import org.app.corporateinternetbanking.transaction.dto.TransferRequest;
import org.app.corporateinternetbanking.transaction.exceptions.*;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.data.domain.Page;

public interface TransactionService {

    TransactionResponse initiateTransaction(TransferRequest request)
            throws InvalidAmount, AccountDoesNotExist, UserNotFound, UnauthorizedAccess, DuplicateTransaction,
                    InsufficientBalance, InvalidAccount, IsNull;

    List<TransactionResponse> viewPendingTransactions() throws UserNotFound;

    Page<TransactionResponse> getTransactions(int page, int size, String status) throws UserNotFound;

    void expirePendingTransactions();

    BigDecimal calculateTransactionVolume();

    void markSuccess(String reference) throws TransactionDoesNotExist;

    void markFailed(String reference) throws TransactionDoesNotExist;

    Transaction findByTransactionReference(String transactionReference) throws TransactionDoesNotExist;

    Page<TransactionResponse> getTransactionsByOrganization(Long orgId, int page, int size)
            throws OrganizationDoesNotExist, UserNotFound, UnauthorizedAccess;
}

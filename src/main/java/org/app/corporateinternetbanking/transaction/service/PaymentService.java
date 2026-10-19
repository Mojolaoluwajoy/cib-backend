package org.app.corporateinternetbanking.transaction.service;

import java.util.Map;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.transaction.dto.FundRequest;
import org.app.corporateinternetbanking.transaction.exceptions.DuplicateTransaction;
import org.app.corporateinternetbanking.transaction.exceptions.InsufficientBalance;
import org.app.corporateinternetbanking.transaction.exceptions.InvalidAmount;
import org.app.corporateinternetbanking.transaction.exceptions.IsNull;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;

public interface PaymentService {

    Map<String, Object> initializeFunding(FundRequest fundRequest)
            throws IsNull, AccountDoesNotExist, UserNotFound, InvalidAccount, InvalidAmount, InsufficientBalance,
                    UnauthorizedAccess, DuplicateTransaction;
}

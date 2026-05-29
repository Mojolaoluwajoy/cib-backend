package org.app.corporateinternetbanking.account.service;

import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.dto.AccountRequest;
import org.app.corporateinternetbanking.account.dto.AccountResponse;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotActive;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.transaction.exceptions.InsufficientBalance;
import org.app.corporateinternetbanking.transaction.exceptions.IsNull;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountResponse createAccount(AccountRequest request) throws OrganizationDoesNotExist, UserNotFound, CurrencyNotFound, CurrencyNotActive;

    List<AccountResponse> viewAll();

    AccountResponse findById(Long id) throws AccountDoesNotExist;


    void credit(Long accountId, BigDecimal amount) throws AccountDoesNotExist;

    void debit(Long accountId, BigDecimal amount) throws AccountDoesNotExist, InsufficientBalance;

    Account getValidAccount(String accountNumber) throws AccountDoesNotExist, IsNull;

    AccountResponse createAccountForOrg(Long orgId, AccountRequest request)
            throws OrganizationDoesNotExist, UserNotFound, CurrencyNotFound, CurrencyNotActive;
}

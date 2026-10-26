package org.app.corporateinternetbanking.transaction.service;

import java.util.List;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.transaction.dto.AccountNameResponse;
import org.app.corporateinternetbanking.transaction.dto.BankResponse;

// Bank list and account-name lookup for payouts, backed by Paystack.
public interface BankLookupService {

    List<BankResponse> listBanks();

    AccountNameResponse resolveAccountName(String accountNumber, String bankCode) throws InvalidAccount;
}

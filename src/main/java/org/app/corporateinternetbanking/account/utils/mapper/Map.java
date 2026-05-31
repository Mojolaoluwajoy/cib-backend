package org.app.corporateinternetbanking.account.utils.mapper;

import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.dto.AccountRequest;
import org.app.corporateinternetbanking.account.dto.AccountResponse;
import org.app.corporateinternetbanking.currency.dto.CurrencyCodeDto;
import org.app.corporateinternetbanking.organization.dto.OrganizationId;
import org.app.corporateinternetbanking.user.dto.UserIdDto;

import java.security.SecureRandom;

public class Map {

    private static String generateAccountNumber() {
        SecureRandom random = new SecureRandom();
        int number = 1000000000 + random.nextInt(900000000);
        return String.valueOf(number);

    }

    public static Account requestMap(AccountRequest request) {
        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setType(request.getType());
        return account;
    }

    public static AccountResponse responseMap(Account account) {
        AccountResponse response = new AccountResponse();
        response.setAccountNumber(account.getAccountNumber());
        response.setType(account.getType());
        response.setTotalBalance(account.getTotalBalance());
        response.setAvailableBalance(account.getAvailableBalance());
        response.setCreatedAt(account.getCreatedAt());

        if (account.getCurrency() != null) {
            response.setCurrencyCode(
                    new CurrencyCodeDto(account.getCurrency().getCode())
            );
        }
        if (account.getOrganization() != null) {
            response.setOrganizationId(
                    new OrganizationId(account.getOrganization().getId())
            );
            response.setOrganizationName(account.getOrganization().getName());
        }
        if (account.getCreatedBy() != null) {
            response.setCreatedBy(
                    new UserIdDto(account.getCreatedBy().getId())
            );
        }
        return response;
    }
}

package org.app.corporateinternetbanking.transaction.service;

import feign.FeignException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.account.exception.InvalidAccount;
import org.app.corporateinternetbanking.integration.paystack.PayStackClient;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackBankListResponse;
import org.app.corporateinternetbanking.integration.paystack.dto.PaystackResolveAccountResponse;
import org.app.corporateinternetbanking.transaction.dto.AccountNameResponse;
import org.app.corporateinternetbanking.transaction.dto.BankResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankLookupServiceImpl implements BankLookupService {

    // The bank list rarely changes, so it is fetched from Paystack at most once a day.
    private static final Duration BANK_LIST_TTL = Duration.ofHours(24);
    private static final int PAGE_SIZE = 100;
    // Guards against a cursor that never ends; Nigeria has a few hundred banks.
    private static final int MAX_PAGES = 20;

    private final PayStackClient payStackClient;

    private volatile List<BankResponse> cachedBanks;
    private volatile Instant cachedAt = Instant.EPOCH;

    @Override
    public List<BankResponse> listBanks() {
        if (cachedBanks != null && Instant.now().isBefore(cachedAt.plus(BANK_LIST_TTL))) {
            return cachedBanks;
        }
        List<BankResponse> banks = new ArrayList<>();
        String next = null;
        for (int page = 0; page < MAX_PAGES; page++) {
            PaystackBankListResponse response = payStackClient.listBanks("nigeria", true, PAGE_SIZE, next);
            if (response == null || response.getData() == null) break;
            response.getData().stream()
                    .filter(PaystackBankListResponse.Bank::isActive)
                    .map(bank -> new BankResponse(bank.getName(), bank.getCode()))
                    .forEach(banks::add);
            next = response.getMeta() == null ? null : response.getMeta().getNext();
            if (next == null) break;
        }
        banks.sort(Comparator.comparing(BankResponse::getName, String.CASE_INSENSITIVE_ORDER));
        cachedBanks = List.copyOf(banks);
        cachedAt = Instant.now();
        return cachedBanks;
    }

    @Override
    public AccountNameResponse resolveAccountName(String accountNumber, String bankCode) throws InvalidAccount {
        if (accountNumber == null || !accountNumber.matches("\\d{10}")) {
            throw new InvalidAccount("Account number must be exactly 10 digits");
        }
        if (bankCode == null || bankCode.isBlank()) {
            throw new InvalidAccount("Select the recipient's bank");
        }
        PaystackResolveAccountResponse response;
        try {
            response = payStackClient.resolveAccount(accountNumber, bankCode);
        } catch (FeignException.FeignClientException e) {
            // Paystack answers 4xx when the number does not exist at that bank
            log.warn("Paystack could not resolve account at bank {}: {}", bankCode, e.status());
            throw new InvalidAccount("No account with that number was found at the selected bank");
        }
        if (response == null || response.getData() == null || response.getData().getAccountName() == null) {
            throw new InvalidAccount("No account with that number was found at the selected bank");
        }
        return new AccountNameResponse(
                accountNumber, bankCode, response.getData().getAccountName());
    }
}

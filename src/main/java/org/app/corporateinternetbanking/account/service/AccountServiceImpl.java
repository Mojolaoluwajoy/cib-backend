package org.app.corporateinternetbanking.account.service;

import lombok.AllArgsConstructor;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.domain.repository.AccountRepository;
import org.app.corporateinternetbanking.account.dto.AccountRequest;
import org.app.corporateinternetbanking.account.dto.AccountResponse;
import org.app.corporateinternetbanking.account.exception.AccountDoesNotExist;
import org.app.corporateinternetbanking.account.utils.mapper.Map;
import org.app.corporateinternetbanking.currency.domain.entity.Currency;
import org.app.corporateinternetbanking.currency.domain.repository.CurrencyRepository;
import org.app.corporateinternetbanking.currency.enums.CurrencyStatus;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotActive;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.email.EmailSenderService;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.app.corporateinternetbanking.organization.exceptions.OrganizationDoesNotExist;
import org.app.corporateinternetbanking.transaction.exceptions.InsufficientBalance;
import org.app.corporateinternetbanking.transaction.exceptions.IsNull;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.dto.UserIdDto;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.app.corporateinternetbanking.user.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.app.corporateinternetbanking.account.utils.mapper.Map.requestMap;
import static org.app.corporateinternetbanking.account.utils.mapper.Map.responseMap;

@org.springframework.stereotype.Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final UserRepository userRepository;
    private final UserServiceImpl userService;
    @Autowired
    AccountRepository repository;
    @Autowired
    OrganizationRepository organizationRepository;
    @Autowired
    CurrencyRepository currencyRepository;
    @Autowired
    private EmailSenderService senderService;

    @Override
    public AccountResponse createAccount(AccountRequest request) throws OrganizationDoesNotExist, UserNotFound, CurrencyNotFound, CurrencyNotActive {
        Account account = requestMap(request);
        User creator = userService.getCurrentUser();
        Organization organization = creator.getOrganization();
        Currency currency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new CurrencyNotFound("This currency does not exist"));
        account.setCurrency(currency);
        if (currency.getStatus().equals(CurrencyStatus.INACTIVE)) {
            throw new CurrencyNotActive("This currency is not available for use right now");

        }
        account.setOrganization(organization);
        account.setCreatedBy(creator);

        Account savedAccount = repository.save(account);
        return responseMap(savedAccount);
    }

    @Override
    public AccountResponse createAccountForOrg(Long orgId, AccountRequest request)
            throws OrganizationDoesNotExist, UserNotFound, CurrencyNotFound, CurrencyNotActive {

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new OrganizationDoesNotExist("Organization not found"));

        User creator = userService.getCurrentUser();

        Currency currency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new CurrencyNotFound("This currency does not exist"));

        if (currency.getStatus().equals(CurrencyStatus.INACTIVE)) {
            throw new CurrencyNotActive("This currency is not available for use right now");
        }

        Account account = requestMap(request);
        account.setCurrency(currency);
        account.setOrganization(organization);
        account.setCreatedBy(creator);

        return responseMap(repository.save(account));
    }

    @Override
    public List<AccountResponse> viewAll() {
        List<Account> accounts = repository.findAll();
        List<AccountResponse> accountList = new ArrayList<>();
        for (Account savedAccount : accounts) {
            AccountResponse response = new AccountResponse();
            response.setTotalBalance(savedAccount.getTotalBalance());
            response.setAvailableBalance(savedAccount.getAvailableBalance());
            response.setAccountNumber(savedAccount.getAccountNumber());
            response.setType(savedAccount.getType());
            response.setCreatedBy(new UserIdDto(savedAccount.getCreatedBy().getId()));
            response.setCreatedAt(savedAccount.getCreatedAt());
        }
        return accountList;
    }

    @Override
    public AccountResponse findById(Long id) throws AccountDoesNotExist {
        Optional<Account> account = repository.findById(id);
        if (account.isEmpty()) {
            throw new AccountDoesNotExist("There's no account with the id entered");
        }
        return responseMap(account.get());

    }

    @Override
    public void credit(Long accountId, BigDecimal amount) throws AccountDoesNotExist {
        Account account = repository.findById(accountId).orElseThrow(() -> new AccountDoesNotExist("Account not found"));

        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setTotalBalance(account.getTotalBalance().add(amount));
        repository.save(account);
    }

    @Override
    public void debit(Long accountId, BigDecimal amount) throws AccountDoesNotExist, InsufficientBalance {
        Account account = repository.findById(accountId).orElseThrow(() -> new AccountDoesNotExist("Account not found"));

        if (account.getTotalBalance().compareTo(amount) < 0) {
            throw new InsufficientBalance("Insufficient funds");
        }
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setTotalBalance(account.getTotalBalance().subtract(amount));
        repository.save(account);
    }

    @Override
    public Account getValidAccount(String accountNumber) throws AccountDoesNotExist, IsNull {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IsNull("Account number is required");
        }
        return repository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountDoesNotExist("Account not found"));
    }

    public List<AccountResponse> getAccountsByOrganization(Long orgId) throws OrganizationDoesNotExist {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new OrganizationDoesNotExist("Organization not found"));

        List<Account> accounts = repository.findAllByOrganization(org);
        return accounts.stream()
                .map(Map::responseMap) // use your existing account mapper
                .collect(Collectors.toList());
    }
}

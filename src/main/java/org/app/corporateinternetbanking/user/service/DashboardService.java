package org.app.corporateinternetbanking.user.service;

import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.domain.repository.AccountRepository;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.app.corporateinternetbanking.organization.enums.OrganizationStatus;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.domain.repository.TransactionRepository;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.dto.DashboardStats;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountRepository accountRepository;
    private final OrganizationRepository organizationRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final UserServiceImpl userService;

    public DashboardStats getStats() throws UserNotFound {

        User currentUser = userService.getCurrentUser();
        UserRole role = currentUser.getRole();

        if (role == UserRole.SUPER_ADMIN) {
            List<Organization> allOrgs = organizationRepository.findAll();

            int pending = (int) allOrgs.stream()
                    .filter(o -> o.getOrganizationStatus() == OrganizationStatus.PENDING).count();
            int approved = (int) allOrgs.stream()
                    .filter(o -> o.getOrganizationStatus() == OrganizationStatus.APPROVED).count();
            int rejected = (int) allOrgs.stream()
                    .filter(o -> o.getOrganizationStatus() == OrganizationStatus.REJECTED).count();
            int disabled = (int) allOrgs.stream()
                    .filter(o -> o.getOrganizationStatus() == OrganizationStatus.DISABLED).count();

            return DashboardStats.builder()
                    .totalOrganizations(allOrgs.size())
                    .pendingOnboarding(pending)
                    .approvedOrganizations(approved)
                    .rejectedOrganizations(rejected)
                    .disabledOrganizations(disabled)
                    .build();
        }

        Organization org = currentUser.getOrganization();

        List<Account> accounts = accountRepository.findAllByOrganization(org);

        List<Transaction> pendingTxns = transactionRepository
                .findAllByStatus(TransactionStatus.PENDING);

        List<Transaction> allTxns = transactionRepository.findAll();

        BigDecimal transferVolume = allTxns.stream()
                .filter(t -> t.getType() == TransactionType.INTERNAL_TRANSFER)
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal payoutVolume = allTxns.stream()
                .filter(t -> t.getType() == TransactionType.EXTERNAL_PAYOUT)
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardStats.builder()
                .totalAccounts(accounts.size())
                .pendingTransactions(pendingTxns.size())
                .transferVolume(transferVolume)
                .payoutVolume(payoutVolume)
                .build();
    }
}
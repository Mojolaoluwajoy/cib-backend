package org.app.corporateinternetbanking.transaction.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.notification.service.NotificationService;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.domain.repository.TransactionRepository;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.service.TransactionService;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionJobsTest {

    @Mock
    TransactionService transactionService;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    OrganizationRepository organizationRepository;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    TransactionJobs jobs;

    Organization orgA;
    Organization orgB;

    @BeforeEach
    void setUp() {
        orgA = organization(1L, "OrgA");
        orgB = organization(2L, "OrgB");
    }

    @Test
    void dailyReportGoesToEachOrganizationsOwnAdminsAndPlatformReportToSuperAdmins() {
        Transaction a1 = transaction(orgA, TransactionStatus.SUCCESS, "100");
        Transaction a2 = transaction(orgA, TransactionStatus.SUCCESS, "50");
        Transaction a3 = transaction(orgA, TransactionStatus.REJECTED, "999");
        Transaction b1 = transaction(orgB, TransactionStatus.PENDING_APPROVAL, "70");

        when(organizationRepository.findAll()).thenReturn(List.of(orgA, orgB));
        when(transactionRepository.findAllForOrganizationCreatedBetween(eq(orgA), any(), any()))
                .thenReturn(List.of(a1, a2, a3));
        when(transactionRepository.findAllForOrganizationCreatedBetween(eq(orgB), any(), any()))
                .thenReturn(List.of(b1));
        when(transactionRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(a1, a2, a3, b1));
        // Two admins in OrgA: the old code crashed as soon as more than one ADMIN existed.
        when(userRepository.findAllByOrganizationAndRoleAndStatus(orgA, UserRole.ADMIN, UserStatus.ACTIVE))
                .thenReturn(List.of(user("adminA1@x.com"), user("adminA2@x.com")));
        when(userRepository.findAllByOrganizationAndRoleAndStatus(orgB, UserRole.ADMIN, UserStatus.ACTIVE))
                .thenReturn(List.of(user("adminB@x.com")));
        when(userRepository.findAllByRoleAndStatus(UserRole.SUPER_ADMIN, UserStatus.ACTIVE))
                .thenReturn(List.of(user("super@x.com")));

        jobs.buildDailyReports();

        ArgumentCaptor<String> orgAReport = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(eq("adminA1@x.com"), orgAReport.capture());
        verify(notificationService).createNotification(eq("adminA2@x.com"), anyString());
        assertThat(orgAReport.getValue())
                .contains("Daily summary report: OrgA")
                .contains("Total transactions: 3")
                .contains("Successful transaction volume: 150")
                .contains("Total rejected transactions: 1");

        ArgumentCaptor<String> orgBReport = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(eq("adminB@x.com"), orgBReport.capture());
        assertThat(orgBReport.getValue()).contains("OrgB").contains("Total awaiting approval: 1");
        assertThat(orgBReport.getValue()).doesNotContain("OrgA");

        ArgumentCaptor<String> platformReport = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(eq("super@x.com"), platformReport.capture());
        assertThat(platformReport.getValue()).contains("All organizations").contains("Total transactions: 4");
    }

    @Test
    void pendingAlertCoversOnlyTheHourThatCrossedTheThresholdAndGoesToTheRightOrganization() {
        Transaction stuckA = transaction(orgA, TransactionStatus.PENDING_APPROVAL, "500");
        when(transactionRepository.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        eq(TransactionStatus.PENDING_APPROVAL), any(), any()))
                .thenReturn(List.of(stuckA));
        when(userRepository.findAllByOrganizationAndRoleAndStatus(orgA, UserRole.ADMIN, UserStatus.ACTIVE))
                .thenReturn(List.of(user("adminA@x.com")));

        jobs.checkPendingTransactions();

        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(transactionRepository)
                .findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        eq(TransactionStatus.PENDING_APPROVAL), start.capture(), end.capture());
        assertThat(end.getValue())
                .isCloseTo(
                        LocalDateTime.now().minusHours(TransactionJobs.PENDING_ALERT_AFTER_HOURS),
                        within(5, ChronoUnit.SECONDS));
        assertThat(start.getValue()).isEqualTo(end.getValue().minusHours(1));

        verify(notificationService).createNotification(eq("adminA@x.com"), anyString());
        verify(userRepository, never()).findAllByOrganizationAndRoleAndStatus(orgB, UserRole.ADMIN, UserStatus.ACTIVE);
    }

    @Test
    void nothingIsSentWhenNoTransactionsAreStuck() {
        when(transactionRepository.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        eq(TransactionStatus.PENDING_APPROVAL), any(), any()))
                .thenReturn(List.of());

        jobs.checkPendingTransactions();

        verify(notificationService, never()).createNotification(anyString(), anyString());
    }

    private static Organization organization(Long id, String name) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName(name);
        return organization;
    }

    private static Transaction transaction(Organization organization, TransactionStatus status, String amount) {
        Account account = new Account();
        account.setOrganization(organization);
        Transaction transaction = new Transaction();
        transaction.setSourceAccount(account);
        transaction.setStatus(status);
        transaction.setAmount(new BigDecimal(amount));
        return transaction;
    }

    private static User user(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }
}

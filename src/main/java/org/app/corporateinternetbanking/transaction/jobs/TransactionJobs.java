package org.app.corporateinternetbanking.transaction.jobs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionJobs {

    static final long PENDING_ALERT_AFTER_HOURS = 10;

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *")
    public void runExpirationJob() {
        transactionService.expirePendingTransactions();
    }

    // Alerts each organization's admins about transfers/payouts that have waited too long for an approver.
    // Each hourly run only looks at the one-hour slice that crossed the threshold since the previous run,
    // so a stuck transaction is reported once instead of every hour.
    @Scheduled(cron = "0 0 * * * *")
    public void checkPendingTransactions() {
        log.info("checking pending transactions");
        LocalDateTime windowEnd = LocalDateTime.now().minusHours(PENDING_ALERT_AFTER_HOURS);
        LocalDateTime windowStart = windowEnd.minusHours(1);

        List<Transaction> pending = transactionRepository.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                TransactionStatus.PENDING_APPROVAL, windowStart, windowEnd);

        Map<Long, List<Transaction>> pendingByOrganization = new LinkedHashMap<>();
        Map<Long, Organization> organizations = new LinkedHashMap<>();
        for (Transaction transaction : pending) {
            if (transaction.getSourceAccount() == null
                    || transaction.getSourceAccount().getOrganization() == null) {
                continue;
            }
            Organization organization = transaction.getSourceAccount().getOrganization();
            organizations.putIfAbsent(organization.getId(), organization);
            pendingByOrganization
                    .computeIfAbsent(organization.getId(), id -> new ArrayList<>())
                    .add(transaction);
        }

        pendingByOrganization.forEach((organizationId, transactions) ->
                notifyAll(organizationAdminEmails(organizations.get(organizationId)), buildMessage(transactions)));
    }

    // Each organization's admins get a report on their own organization only; super admins get the platform view.
    @Scheduled(cron = "0 0 0 * * *")
    public void buildDailyReports() {
        log.info("building daily reports");
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(24);

        for (Organization organization : organizationRepository.findAll()) {
            List<Transaction> transactions =
                    transactionRepository.findAllForOrganizationCreatedBetween(organization, start, end);
            if (!transactions.isEmpty()) {
                notifyAll(
                        organizationAdminEmails(organization),
                        buildSummaryReportMessage(organization.getName(), transactions));
            }
        }

        List<Transaction> allTransactions = transactionRepository.findByCreatedAtBetween(start, end);
        if (!allTransactions.isEmpty()) {
            notifyAll(superAdminEmails(), buildSummaryReportMessage("All organizations", allTransactions));
        }
    }

    private List<String> organizationAdminEmails(Organization organization) {
        return userRepository
                .findAllByOrganizationAndRoleAndStatus(organization, UserRole.ADMIN, UserStatus.ACTIVE)
                .stream()
                .map(User::getEmail)
                .toList();
    }

    private List<String> superAdminEmails() {
        return userRepository.findAllByRoleAndStatus(UserRole.SUPER_ADMIN, UserStatus.ACTIVE).stream()
                .map(User::getEmail)
                .toList();
    }

    private void notifyAll(List<String> recipients, String message) {
        if (recipients.isEmpty()) {
            log.warn("No active recipients for notification, skipping");
            return;
        }
        recipients.forEach(email -> notificationService.createNotification(email, message));
    }

    private String buildMessage(List<Transaction> transactions) {

        StringBuilder message = new StringBuilder();
        message.append("Pending transaction alert\n\n");
        message.append("The following transactions have been awaiting approval for over ")
                .append(PENDING_ALERT_AFTER_HOURS)
                .append(" hours.\n\n");
        message.append("Total: ").append(transactions.size()).append("\n\n");

        for (Transaction transaction : transactions) {
            message.append("ID: ")
                    .append(transaction.getId())
                    .append(" | Amount: ")
                    .append(transaction.getAmount())
                    .append(" | Created: ")
                    .append(transaction.getCreatedAt())
                    .append("\n");
        }
        return message.toString();
    }

    private String buildSummaryReportMessage(String scope, List<Transaction> transactions) {
        BigDecimal volume = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder message = new StringBuilder();
        message.append("Daily summary report: ").append(scope).append("\n\n");
        message.append("Total transactions: ").append(transactions.size()).append("\n\n");
        message.append("Successful transaction volume: ").append(volume).append("\n\n");
        message.append("Total pending transactions: ")
                .append(count(transactions, TransactionStatus.PENDING))
                .append("\n\n");
        message.append("Total awaiting approval: ")
                .append(count(transactions, TransactionStatus.PENDING_APPROVAL))
                .append("\n\n");
        message.append("Total successful transactions: ")
                .append(count(transactions, TransactionStatus.SUCCESS))
                .append("\n\n");
        message.append("Total failed transactions: ")
                .append(count(transactions, TransactionStatus.FAILED))
                .append("\n\n");
        message.append("Total expired transactions: ")
                .append(count(transactions, TransactionStatus.EXPIRED))
                .append("\n\n");
        message.append("Total rejected transactions: ")
                .append(count(transactions, TransactionStatus.REJECTED))
                .append("\n\n");
        return message.toString();
    }

    private static long count(List<Transaction> transactions, TransactionStatus status) {
        return transactions.stream().filter(t -> t.getStatus() == status).count();
    }
}

package org.app.corporateinternetbanking.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.service.AccountService;
import org.app.corporateinternetbanking.notification.domain.repository.NotificationRepository;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.support.IntegrationTestBase;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.dto.TransactionApprovalRequest;
import org.app.corporateinternetbanking.transaction.dto.TransferRequest;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.transaction.exceptions.InsufficientBalance;
import org.app.corporateinternetbanking.transaction.exceptions.TransactionAlreadyProcessed;
import org.app.corporateinternetbanking.transaction.service.ApprovalService;
import org.app.corporateinternetbanking.transaction.service.TransactionService;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConcurrencyIntegrationTest extends IntegrationTestBase {

    @Autowired
    AccountService accountService;

    @Autowired
    ApprovalService approvalService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    NotificationRepository notificationRepository;

    @Test
    void concurrentCreditsAreNeverLost() throws Exception {
        Organization org = organization("Acme");
        Account account = account(org, "3000000001", "0");

        List<Outcome> outcomes = runConcurrently(20, i -> {
            accountService.credit(account.getId(), new BigDecimal("10"));
            return null;
        });

        assertThat(outcomes).allMatch(Outcome::succeeded);
        Account after = reload(account);
        assertThat(after.getTotalBalance()).isEqualByComparingTo("200");
        assertThat(after.getAvailableBalance()).isEqualByComparingTo("200");
    }

    @Test
    void twoApproversApprovingTheSameTransferMoveTheMoneyOnce() throws Exception {
        Organization org = organization("Acme");
        User maker = user("maker@acme.com", UserRole.MAKER, org);
        User approver1 = user("approver1@acme.com", UserRole.APPROVER, org);
        User approver2 = user("approver2@acme.com", UserRole.APPROVER, org);
        Account source = account(org, "3000000001", "1000");
        Account destination = account(org, "3000000002", "0");
        Transaction transfer = awaitingApproval(source, destination, "400", maker);

        List<User> approvers = List.of(approver1, approver2);
        List<Outcome> outcomes = runConcurrently(2, i -> {
            actAs(approvers.get(i));
            TransactionApprovalRequest request = new TransactionApprovalRequest();
            request.setTransactionId(transfer.getId());
            request.setStatus(TransactionStatus.SUCCESS);
            return approvalService.approveInternalTransaction(request);
        });

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(1);
        assertThat(outcomes).filteredOn(o -> !o.succeeded()).singleElement().satisfies(o -> assertThat(o.error())
                .isInstanceOf(TransactionAlreadyProcessed.class));

        assertThat(reload(transfer).getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        Account sourceAfter = reload(source);
        assertThat(sourceAfter.getTotalBalance()).isEqualByComparingTo("600");
        assertThat(sourceAfter.getAvailableBalance()).isEqualByComparingTo("600");
        assertThat(sourceAfter.getReservedBalance()).isEqualByComparingTo("0");
        assertThat(reload(destination).getTotalBalance()).isEqualByComparingTo("400");
    }

    @Test
    void concurrentTransfersCannotOverdrawTheSourceAccount() throws Exception {
        Organization org = organization("Acme");
        User maker = user("maker@acme.com", UserRole.MAKER, org);
        Account source = account(org, "3000000001", "100");
        account(org, "3000000002", "0");

        // Ten requests of 30 against a balance of 100: exactly three can be reserved.
        List<Outcome> outcomes = runConcurrently(10, i -> {
            actAs(maker);
            TransferRequest request = new TransferRequest();
            request.setType(TransactionType.INTERNAL_TRANSFER);
            request.setSourceAccount("3000000001");
            request.setDestinationAccount("3000000002");
            request.setAmount(new BigDecimal("30"));
            request.setIdempotencyKey("transfer-" + i);
            return transactionService.initiateTransaction(request);
        });

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(3);
        assertThat(outcomes).filteredOn(o -> !o.succeeded()).allSatisfy(o -> assertThat(o.error())
                .isInstanceOf(InsufficientBalance.class));
        Account after = reload(source);
        assertThat(after.getAvailableBalance()).isEqualByComparingTo("10");
        assertThat(after.getReservedBalance()).isEqualByComparingTo("90");
        assertThat(after.getTotalBalance()).isEqualByComparingTo("100");
    }

    @Test
    void unapprovedTransfersExpireReleaseFundsAndNotifyTheMaker() {
        Organization org = organization("Acme");
        User maker = user("maker@acme.com", UserRole.MAKER, org);
        Account source = account(org, "3000000001", "1000");
        Account destination = account(org, "3000000002", "0");
        Transaction stale = awaitingApproval(source, destination, "250", maker);
        stale.setCreatedAt(LocalDateTime.now().minusHours(49));
        transactionRepository.save(stale);
        Transaction recent = awaitingApproval(source, destination, "100", maker);

        transactionService.expireUnapprovedTransactions();

        assertThat(reload(stale).getStatus()).isEqualTo(TransactionStatus.EXPIRED);
        assertThat(reload(recent).getStatus()).isEqualTo(TransactionStatus.PENDING_APPROVAL);
        Account after = reload(source);
        assertThat(after.getAvailableBalance()).isEqualByComparingTo("900");
        assertThat(after.getReservedBalance()).isEqualByComparingTo("100");
        assertThat(notificationRepository.findAll()).singleElement().satisfies(n -> assertThat(n.getRecipient())
                .isEqualTo("maker@acme.com"));
    }

    // Starts all tasks at the same instant and collects each one's result or exception.
    private List<Outcome> runConcurrently(int threads, Task task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int index = i;
            Callable<Outcome> callable = () -> {
                start.await();
                try {
                    task.run(index);
                    return new Outcome(null);
                } catch (Exception e) {
                    return new Outcome(e);
                }
            };
            futures.add(pool.submit(callable));
        }
        start.countDown();
        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> future : futures) {
            outcomes.add(future.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return outcomes;
    }

    @FunctionalInterface
    interface Task {
        Object run(int index) throws Exception;
    }

    record Outcome(Exception error) {
        boolean succeeded() {
            return error == null;
        }
    }
}

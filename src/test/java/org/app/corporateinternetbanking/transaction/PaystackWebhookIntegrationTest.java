package org.app.corporateinternetbanking.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.ledger.domain.repository.LedgerRepository;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.support.IntegrationTestBase;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.transaction.service.WebhookHandler;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PaystackWebhookIntegrationTest extends IntegrationTestBase {

    @Autowired
    LedgerRepository ledgerRepository;

    @Autowired
    WebhookHandler webhookHandler;

    @Test
    void forgedWebhookIsRejectedAndMovesNoMoney() throws Exception {
        Organization org = organization("Acme");
        Account account = account(org, "4000000001", "0");
        String payload = chargeSuccess("T_forged", 99_999_900L, org.getPaystackCustomerCode());

        mvc.perform(post("/webhook/paystack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-paystack-signature", "deadbeef")
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(reload(account).getTotalBalance()).isEqualByComparingTo("0");
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void fundingIsCreditedOnceEvenWhenPaystackRetries() throws Exception {
        Organization org = organization("Acme");
        Account account = account(org, "4000000001", "0");
        Transaction funding = funding(account, "5000", "ref-funding-1");
        // Paystack always includes a customer code, even for ordinary card payments.
        String payload = chargeSuccess("ref-funding-1", 500_000L, "CUS_someone_else");

        signedWebhook(payload).andExpect(status().isOk());
        signedWebhook(payload).andExpect(status().isOk());

        assertThat(reload(funding).getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        Account after = reload(account);
        assertThat(after.getTotalBalance()).isEqualByComparingTo("5000");
        assertThat(after.getAvailableBalance()).isEqualByComparingTo("5000");
        assertThat(ledgerRepository.findByAccountId(account.getId())).hasSize(1);
    }

    @Test
    void fundingWithWrongAmountIsMarkedFailedAndNotCredited() throws Exception {
        Organization org = organization("Acme");
        Account account = account(org, "4000000001", "0");
        Transaction funding = funding(account, "5000", "ref-funding-1");

        signedWebhook(chargeSuccess("ref-funding-1", 100L, "CUS_x")).andExpect(status().isOk());

        assertThat(reload(funding).getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(reload(account).getTotalBalance()).isEqualByComparingTo("0");
    }

    @Test
    void dvaDepositIsRecordedOnceEvenWhenDeliveredTwice() throws Exception {
        Organization org = organization("Acme");
        Account account = account(org, "4000000001", "0");
        String payload = chargeSuccess("T_dva_1", 150_000L, org.getPaystackCustomerCode());

        signedWebhook(payload).andExpect(status().isOk());
        signedWebhook(payload).andExpect(status().isOk());

        assertThat(reload(account).getTotalBalance()).isEqualByComparingTo("1500");
        assertThat(transactionRepository.findAll()).singleElement().satisfies(t -> {
            assertThat(t.getType()).isEqualTo(TransactionType.EXTERNAL_DEPOSIT);
            assertThat(t.getReference()).isEqualTo("T_dva_1");
        });
    }

    @Test
    void simultaneousDuplicateDvaDeliveriesAreRecordedOnce() throws Exception {
        Organization org = organization("Acme");
        Account account = account(org, "4000000001", "0");
        String payload = chargeSuccess("T_dva_race", 150_000L, org.getPaystackCustomerCode());
        String signature = sign(payload);

        ExecutorService pool = Executors.newFixedThreadPool(5);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> deliveries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            deliveries.add(pool.submit(() -> {
                start.await();
                return webhookHandler.handleWebhook(payload, signature);
            }));
        }
        start.countDown();
        for (Future<?> delivery : deliveries) {
            delivery.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(reload(account).getTotalBalance()).isEqualByComparingTo("1500");
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    void successfulPayoutDebitsTheReservedFundsOnce() throws Exception {
        Organization org = organization("Acme");
        User maker = user("maker@acme.com", UserRole.MAKER, org);
        Account account = account(org, "4000000001", "1000");
        Transaction payout = approvedPayout(account, "400", maker);
        String payload = transferEvent("transfer.success", payout.getReference());

        signedWebhook(payload).andExpect(status().isOk());
        signedWebhook(payload).andExpect(status().isOk());

        assertThat(reload(payout).getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        Account after = reload(account);
        assertThat(after.getTotalBalance()).isEqualByComparingTo("600");
        assertThat(after.getAvailableBalance()).isEqualByComparingTo("600");
        assertThat(after.getReservedBalance()).isEqualByComparingTo("0");
    }

    @Test
    void failedPayoutReleasesTheReservedFunds() throws Exception {
        Organization org = organization("Acme");
        User maker = user("maker@acme.com", UserRole.MAKER, org);
        Account account = account(org, "4000000001", "1000");
        Transaction payout = approvedPayout(account, "400", maker);

        signedWebhook(transferEvent("transfer.failed", payout.getReference())).andExpect(status().isOk());

        assertThat(reload(payout).getStatus()).isEqualTo(TransactionStatus.FAILED);
        Account after = reload(account);
        assertThat(after.getTotalBalance()).isEqualByComparingTo("1000");
        assertThat(after.getAvailableBalance()).isEqualByComparingTo("1000");
        assertThat(after.getReservedBalance()).isEqualByComparingTo("0");
    }

    @Test
    void transferEventForAPayoutStillAwaitingApprovalAsksPaystackToRetry() throws Exception {
        Organization org = organization("Acme");
        User maker = user("maker@acme.com", UserRole.MAKER, org);
        Account account = account(org, "4000000001", "1000");
        Transaction payout = approvedPayout(account, "400", maker);
        payout.setStatus(TransactionStatus.PENDING_APPROVAL);
        transactionRepository.save(payout);

        // A non-2xx response makes Paystack deliver the event again later.
        signedWebhook(transferEvent("transfer.success", payout.getReference())).andExpect(status().is4xxClientError());
        assertThat(reload(account).getTotalBalance()).isEqualByComparingTo("1000");
    }

    private ResultActions signedWebhook(String payload) throws Exception {
        return mvc.perform(post("/webhook/paystack")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-paystack-signature", sign(payload))
                .content(payload));
    }

    private static String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(PAYSTACK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private static String chargeSuccess(String reference, long amountInKobo, String customerCode) {
        return """
                {"event":"charge.success","data":{"reference":"%s","amount":%d,"status":"success",\
                "customer":{"customer_code":"%s"}}}"""
                .formatted(reference, amountInKobo, customerCode);
    }

    private static String transferEvent(String event, String reference) {
        return """
                {"event":"%s","data":{"reference":"%s","amount":40000}}"""
                .formatted(event, reference);
    }

    private Transaction funding(Account destination, String amount, String reference) {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.EXTERNAL_FUNDING);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setDestinationAccount(destination);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setReference(reference);
        transaction.setIdempotencyKey("idem-" + reference);
        transaction.setCreatedAt(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    private Transaction approvedPayout(Account source, String amount, User maker) {
        Transaction payout = awaitingApproval(source, null, amount, maker);
        payout.setType(TransactionType.EXTERNAL_PAYOUT);
        payout.setStatus(TransactionStatus.APPROVED);
        return transactionRepository.save(payout);
    }
}

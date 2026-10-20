package org.app.corporateinternetbanking.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.account.domain.repository.AccountRepository;
import org.app.corporateinternetbanking.currency.domain.repository.CurrencyRepository;
import org.app.corporateinternetbanking.integration.paystack.PayStackClient;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.app.corporateinternetbanking.organization.enums.OrganizationStatus;
import org.app.corporateinternetbanking.security.JwtService;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.domain.repository.TransactionRepository;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

// Base for tests that run the whole application against a real, throwaway Postgres started in Docker.
// Every test starts from empty tables, so tests never depend on each other or on a developer's database.
@SpringBootTest(properties = {"paystack.secret.key=" + IntegrationTestBase.PAYSTACK_SECRET})
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    public static final String PAYSTACK_SECRET = "sk_test_integration_secret";
    public static final String PASSWORD = "Passw0rd!";

    // One container for the whole test run, shared by every test class (and the cached Spring context).
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    // External systems are replaced so tests are fast, deterministic and never send real email or money.
    @MockBean
    protected JavaMailSender mailSender;

    @MockBean
    protected PayStackClient payStackClient;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected OrganizationRepository organizationRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected TransactionRepository transactionRepository;

    @Autowired
    protected CurrencyRepository currencyRepository;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("TRUNCATE ledger_entry, notification, \"transaction\", payout_recipient, account, users,"
                + " organization RESTART IDENTITY CASCADE");
        jdbc.update("UPDATE currency SET status = 'ACTIVE' WHERE code = 'NGN'");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    protected Organization organization(String name) {
        Organization organization = new Organization();
        organization.setName(name);
        organization.setOrganizationStatus(OrganizationStatus.APPROVED);
        organization.setPaystackCustomerCode("CUS_" + name.toLowerCase());
        return organizationRepository.save(organization);
    }

    protected User user(String email, UserRole role, Organization organization) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(email.substring(0, email.indexOf('@')));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setOrganization(organization);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        return userRepository.save(user);
    }

    protected Account account(Organization organization, String accountNumber, String balance) {
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setOrganization(organization);
        account.setCurrency(currencyRepository.findByCode("NGN").orElseThrow());
        account.setTotalBalance(new BigDecimal(balance));
        account.setAvailableBalance(new BigDecimal(balance));
        account.setReservedBalance(BigDecimal.ZERO);
        return accountRepository.save(account);
    }

    // A transfer or payout that a maker initiated: the amount is already moved from available to reserved.
    protected Transaction awaitingApproval(Account source, Account destination, String amount, User maker) {
        BigDecimal value = new BigDecimal(amount);
        source.setAvailableBalance(source.getAvailableBalance().subtract(value));
        source.setReservedBalance(source.getReservedBalance().add(value));
        accountRepository.save(source);

        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.INTERNAL_TRANSFER);
        transaction.setStatus(TransactionStatus.PENDING_APPROVAL);
        transaction.setSourceAccount(source);
        transaction.setDestinationAccount(destination);
        transaction.setAmount(value);
        transaction.setCreatedBy(maker);
        transaction.setIdempotencyKey(UUID.randomUUID().toString());
        transaction.setReference(UUID.randomUUID().toString());
        transaction.setCreatedAt(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    protected String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    // For calling services directly (e.g. from worker threads): the services read the current user from here.
    protected static void actAs(User user) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(), null, java.util.List.of()));
    }

    protected Account reload(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow();
    }

    protected Transaction reload(Transaction transaction) {
        return transactionRepository.findById(transaction.getId()).orElseThrow();
    }
}

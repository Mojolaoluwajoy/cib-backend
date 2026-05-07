package org.app.corporateinternetbanking.account.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.app.corporateinternetbanking.account.enums.AccountType;
import org.app.corporateinternetbanking.currency.domain.entity.Currency;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.user.domain.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Setter
@Getter
@ToString
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String accountNumber;
    private BigDecimal totalBalance = BigDecimal.valueOf(10000);
    private BigDecimal availableBalance = totalBalance;
    private BigDecimal reservedBalance = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    private AccountType type;
    @OneToMany(mappedBy = "sourceAccount")
    private List<Transaction> sourceTransactions;
    @OneToMany(mappedBy = "destinationAccount")
    private List<Transaction> destinationTransactions;
    @ManyToOne
    @JoinColumn(name = "currency")
    private Currency currency;
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean flagged = false;


    @PrePersist
    public void prePersist() {
        this.flagged = false;
        if (availableBalance == null) {
            availableBalance = getTotalBalance();
        }
        if (totalBalance == null) {
            totalBalance = BigDecimal.valueOf(10000);
        }
    }
}
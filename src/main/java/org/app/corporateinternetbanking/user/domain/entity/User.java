package org.app.corporateinternetbanking.user.domain.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Setter
@Getter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String firstName;
    private String lastName;
    @Column(unique = true)
    private String email;
    private String password;
    private String nin;
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @OneToMany(mappedBy = "createdBy", fetch = FetchType.EAGER)
    private List<Account> createdAccounts;
    @OneToMany(mappedBy = "createdBy", fetch = FetchType.EAGER)
    private List<Transaction> createdTransactions;
    @OneToMany(mappedBy = "processedBy", fetch = FetchType.EAGER)
    private List<Transaction> createdApprovals;
    private LocalDateTime createdAt = LocalDateTime.now();


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", nin='" + nin + '\'' +
                ", role=" + role +
                ", status=" + status +
                ", organization=" + organization +
                ", createdAccounts=" + createdAccounts +
                ", createdTransactions=" + createdTransactions +
                ", createdApprovals=" + createdApprovals +
                ", createdAt=" + createdAt +
                '}';
    }

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = UserStatus.INACTIVE;
        }
        if (this.userId == null) {
            this.userId = java.util.UUID.randomUUID().toString();
        }
    }
}

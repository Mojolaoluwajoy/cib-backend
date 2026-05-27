package org.app.corporateinternetbanking.organization.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.organization.enums.OrganizationStatus;
import org.app.corporateinternetbanking.user.domain.entity.User;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Setter
@Getter

public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String registrationNumber;
    private String phoneNumber;
    @OneToMany(mappedBy = "organization")
    private List<Account> accounts;
    @OneToMany(mappedBy = "organization")
    private List<User> users;
    @Enumerated(EnumType.STRING)
    private OrganizationStatus organizationStatus = OrganizationStatus.PENDING;
    private String organizationEmail;
    private String paystackCustomerCode;
    private String dvaAccountNumber;
    private String dvaBankName;
    private LocalDateTime registeredAt;
    private LocalDateTime approvedAt;

}

package org.app.corporateinternetbanking.currency.domain.entity;

import jakarta.persistence.*;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.currency.enums.CurrencyStatus;

@Entity
@Setter
@Getter
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String code;

    private String symbol;

    @Enumerated(EnumType.STRING)
    private CurrencyStatus status;

    @OneToMany(mappedBy = "currency")
    private List<Account> accounts;
}

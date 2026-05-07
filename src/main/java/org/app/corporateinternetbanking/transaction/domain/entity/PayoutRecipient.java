package org.app.corporateinternetbanking.transaction.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class PayoutRecipient {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 100)
    private String bankCode;

    @Column(length = 100)
    private String bankName;

    @Column(nullable = false, unique = true)
    private String recipientCode;
    @Column(nullable = false, length = 100)
    private String recipientName;

    private LocalDateTime createdAt = LocalDateTime.now();

}



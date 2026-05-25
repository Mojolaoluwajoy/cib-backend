package org.app.corporateinternetbanking.transaction.domain.repository;

import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {


    List<Transaction> findByStatus(TransactionStatus status);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);


    Page<Transaction> findByStatus(String status, Pageable pageable);

    List<Transaction> findByStatusAndCreatedAtBefore(TransactionStatus status, LocalDateTime time);

    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Transaction> findByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, TransactionStatus status);


    List<Transaction> findAllByStatus(TransactionStatus transactionStatus);

    Page<Transaction> findAllBySourceAccount_Organization(
            Organization organization, Pageable pageable);
}
package org.app.corporateinternetbanking.transaction.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.transaction.domain.entity.Transaction;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByStatus(TransactionStatus status);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByReference(String reference);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    List<Transaction> findByStatusAndCreatedAtBefore(TransactionStatus status, LocalDateTime time);

    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Transaction> findByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, TransactionStatus status);

    List<Transaction> findAllByStatus(TransactionStatus transactionStatus);

    // A transaction belongs to an organization if either side of it is one of the organization's accounts
    // (funding and DVA deposits only have a destination account).
    String ORGANIZATION_SCOPE =
            "select t from Transaction t left join t.sourceAccount s left join t.destinationAccount d"
                    + " where (s.organization = :org or d.organization = :org)";

    @Query(ORGANIZATION_SCOPE)
    Page<Transaction> findAllForOrganization(@Param("org") Organization organization, Pageable pageable);

    @Query(ORGANIZATION_SCOPE)
    List<Transaction> findAllForOrganization(@Param("org") Organization organization);

    @Query(ORGANIZATION_SCOPE + " and t.status = :status")
    Page<Transaction> findAllForOrganizationByStatus(
            @Param("org") Organization organization, @Param("status") TransactionStatus status, Pageable pageable);

    @Query(ORGANIZATION_SCOPE + " and t.status = :status")
    List<Transaction> findAllForOrganizationByStatus(
            @Param("org") Organization organization, @Param("status") TransactionStatus status);
}

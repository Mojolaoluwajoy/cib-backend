package org.app.corporateinternetbanking.ledger.domain.repository;

import java.util.List;
import java.util.Optional;
import org.app.corporateinternetbanking.ledger.domain.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByAccountId(Long accountId);

    List<LedgerEntry> findByTransactionId(Long transactionId);

    Optional<LedgerEntry> findFirstByAccountIdOrderByIdDesc(Long accountId);
}

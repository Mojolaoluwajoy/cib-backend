package org.app.corporateinternetbanking.transaction.domain.repository;

import java.util.Optional;
import org.app.corporateinternetbanking.transaction.domain.entity.PayoutRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRecipientRepository extends JpaRepository<PayoutRecipient, Long> {

    Optional<PayoutRecipient> findByAccountNumberAndBankCode(String accountNumber, String bankCode);
}

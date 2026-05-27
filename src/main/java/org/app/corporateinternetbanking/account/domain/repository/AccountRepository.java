package org.app.corporateinternetbanking.account.domain.repository;

import org.app.corporateinternetbanking.account.domain.entity.Account;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {


    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findAllByOrganization(Organization org);

    Account findFirstByOrganizationId(Long id);

    
    Optional<Account> findFirstByOrganizationAndCurrencyCodeOrderByCreatedAtAsc(Organization organization, String currencyCode);

}

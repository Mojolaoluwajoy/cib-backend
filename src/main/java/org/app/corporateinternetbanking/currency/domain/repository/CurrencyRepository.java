package org.app.corporateinternetbanking.currency.domain.repository;

import org.app.corporateinternetbanking.currency.domain.entity.Currency;
import org.app.corporateinternetbanking.currency.enums.CurrencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {


    Optional<Currency> findByCode(String code);

    List<Currency> findByStatus(CurrencyStatus status);

    Collection<Currency> findAllByStatus(CurrencyStatus status);
}

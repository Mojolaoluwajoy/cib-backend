package org.app.corporateinternetbanking.organization.domain.repository;

import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, java.lang.Long> {


    Optional<Organization> findByRegistrationNumber(String registrationNumber);

    Optional<Organization> findByName(String name);
}

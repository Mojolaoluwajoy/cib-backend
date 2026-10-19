package org.app.corporateinternetbanking.user.domain.repository;

import java.util.List;
import java.util.Optional;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@org.springframework.stereotype.Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNin(String nin);

    Optional<User> findByEmail(String email);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findByOrganization(Organization organization, Pageable pageable);

    Page<User> findByOrganizationAndStatus(Organization organization, UserStatus status, Pageable pageable);

    Optional<User> findByRole(UserRole userRole);

    Optional<User> findByPassword(String password);

    boolean existsByRole(UserRole role);

    boolean existsByEmail(String userEmail);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.organization")
    List<User> findAllWithOrganization();

    Optional<User> findByOrganizationIdAndRole(Long organizationId, UserRole role);

    List<User> findAllByOrganization(Organization organization);
}

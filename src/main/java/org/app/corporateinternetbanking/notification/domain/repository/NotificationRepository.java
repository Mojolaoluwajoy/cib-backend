package org.app.corporateinternetbanking.notification.domain.repository;

import java.util.List;
import org.app.corporateinternetbanking.notification.domain.entity.Notification;
import org.app.corporateinternetbanking.notification.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStatus(NotificationStatus status);
}

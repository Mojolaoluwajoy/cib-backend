package org.app.corporateinternetbanking.notification.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.notification.enums.NotificationStatus;

@Entity
@Setter
@Getter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipient;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}

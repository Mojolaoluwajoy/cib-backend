package org.app.corporateinternetbanking.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.email.EmailSenderService;
import org.app.corporateinternetbanking.notification.domain.entity.Notification;
import org.app.corporateinternetbanking.notification.domain.repository.NotificationRepository;
import org.app.corporateinternetbanking.notification.enums.NotificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    EmailSenderService emailSenderService;

    @Override
    public void createNotification(String recipient, String message) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    @Override
    public void sendPendingTransactionsNotification(Notification notification) {
        try {
            emailSenderService.sendEmail(notification.getRecipient(), "pending transactions alert", notification.getMessage());
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Email failed", e);

        }
    }


}

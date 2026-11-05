package com.reliefgrid.notification;

import com.reliefgrid.user.User;
import com.reliefgrid.user.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every notification is recorded in the `notifications` table regardless of whether email
 * actually goes out — so the in-app "recent activity" view (and anyone debugging) has a
 * single source of truth. When spring.mail.host isn't set (local/dev by default), this logs
 * what it would have sent instead of failing — same resilience philosophy as EventPublisher
 * and the AI service's Kafka publisher: a notification failure must never affect the thing
 * that triggered it (a status change, a task assignment).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final UserRepository users;
    private final NotificationRepository notifications;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.notifications.from-address:no-reply@reliefgrid.local}")
    private String fromAddress;

    @Transactional
    public void notifyUser(UUID userId, String subject, String message) {
        User user = users.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Tried to notify unknown user {}", userId);
            return;
        }

        boolean delivered = sendEmail(user.getEmail(), subject, message);

        Notification n = new Notification();
        n.setUserId(userId);
        n.setChannel(NotificationChannel.EMAIL);
        n.setSubject(subject);
        n.setMessage(message);
        n.setDelivered(delivered);
        notifications.save(n);
    }

    private boolean sendEmail(String toAddress, String subject, String body) {
        if (mailHost == null || mailHost.isBlank()) {
            log.info("[DEV: no SMTP configured] Would email {} — subject: \"{}\" — {}", toAddress, subject, body);
            return false;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(toAddress);
            mail.setSubject(subject);
            mail.setText(body);
            mailSender.send(mail);
            return true;
        } catch (MailException e) {
            log.warn("Failed to email {}: {}", toAddress, e.getMessage());
            return false;
        }
    }
}

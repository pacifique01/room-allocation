package org.project.service;

import org.project.model.Notification;
import org.project.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final String mailUsername;
    private final String mailPassword;

    public NotificationService(NotificationRepository notificationRepository,
                               JavaMailSender mailSender,
                               @Value("${app.mail.from}") String senderEmail,
                               @Value("${spring.mail.username:}") String mailUsername,
                               @Value("${spring.mail.password:}") String mailPassword) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
    }

    public boolean notifyUser(String recipientEmail, String subject, String message) {
        notificationRepository.save(new Notification(recipientEmail, message));
        try {
            sendEmail(recipientEmail, subject, message);
            return true;
        } catch (MailConfigurationException ex) {
            LOGGER.warn("Email notification is not configured. Notification remains available in the web app. {}",
                    ex.getMessage());
        } catch (MailException ex) {
            LOGGER.warn("Could not send email notification to {}. Notification remains available in the web app.",
                    recipientEmail, ex);
        }
        return false;
    }

    public void sendTestEmail(String recipientEmail) {
        sendEmail(
                recipientEmail,
                "Room Allocation email test",
                "This is a test email from the Room Allocation System. Gmail SMTP is configured correctly."
        );
    }

    private void sendEmail(String recipientEmail, String subject, String message) {
        try {
            validateMailConfiguration();
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(senderEmail);
            email.setTo(recipientEmail);
            email.setSubject(subject);
            email.setText(message);
            mailSender.send(email);
            LOGGER.info("Email notification sent to {}", recipientEmail);
        } catch (MailException ex) {
            throw ex;
        }
    }

    private void validateMailConfiguration() {
        if (isBlank(mailUsername) || isBlank(mailPassword)) {
            throw new MailConfigurationException(
                    "Email is not configured. Copy application-local.example.properties to application-local.properties, "
                            + "then set MAIL_USERNAME and a Gmail App Password in MAIL_PASSWORD before restarting the app."
            );
        }
        if (senderEmail == null || senderEmail.isBlank() || senderEmail.endsWith("@roomallocation.local")) {
            throw new MailConfigurationException(
                    "Email sender is not configured. Set MAIL_FROM to the same Gmail address as MAIL_USERNAME."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

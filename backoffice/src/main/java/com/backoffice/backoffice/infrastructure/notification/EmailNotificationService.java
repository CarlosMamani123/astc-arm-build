package com.backoffice.backoffice.infrastructure.notification;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EmailNotificationService {

    private static final Logger LOG = Logger.getLogger(EmailNotificationService.class);

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "app.notification.email.enabled", defaultValue = "false")
    boolean enabled;

    public void send(String to, String subject, String body) {
        if (!enabled) {
            LOG.infof("[EMAIL DISABLED] Would send to=%s subject=%s", to, subject);
            return;
        }
        if (to == null || to.isBlank()) {
            LOG.warn("Email not sent: recipient is empty");
            return;
        }
        try {
            mailer.send(Mail.withText(to, subject, body));
            LOG.infof("Email sent to=%s subject=%s", to, subject);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send email to=%s: %s", to, e.getMessage());
        }
    }

    public void sendProjectAssigned(String userEmail, String projectName, String role) {
        send(userEmail,
            "[ASTC] Asignado a proyecto - " + projectName,
            "Has sido asignado al proyecto \"" + projectName + "\" como " + role + ".\n\n"
            + "Ya puedes registrar tu asistencia en este proyecto.\n\n"
            + "ASTC - Sistema de Asistencia");
    }
}

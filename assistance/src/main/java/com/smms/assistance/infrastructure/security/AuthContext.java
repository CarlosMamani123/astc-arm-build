
package com.smms.assistance.infrastructure.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;
import java.util.logging.Logger;

@RequestScoped
public class AuthContext {

    private static final Logger LOG =
        Logger.getLogger(AuthContext.class.getName());

    @Inject
    JsonWebToken jwt;

    private static final UUID DEV_USER =
        UUID.fromString("11111111-1111-1111-1111-111111111111");

    public UUID getUserId() {

        LOG.info("====================================");
        LOG.info("🔥 AuthContext.getUserId()");

        if (jwt == null) {
            LOG.warning("❌ JWT object is NULL");
            LOG.warning("⚠ Using DEV USER: " + DEV_USER);
            LOG.info("====================================");

            return DEV_USER;
        }

        LOG.info("✅ JWT detected");

        String subject = jwt.getSubject();

        LOG.info("📌 JWT SUBJECT: " + subject);
        LOG.info("📧 JWT EMAIL: " + jwt.getClaim("email"));
        LOG.info("🛡 JWT ROLE: " + jwt.getClaim("role"));
        LOG.info("👥 JWT GROUPS: " + jwt.getGroups());

        if (subject == null) {
            LOG.warning("❌ JWT subject is NULL");
            LOG.warning("⚠ Using DEV USER: " + DEV_USER);
            LOG.info("====================================");

            return DEV_USER;
        }

        UUID userId = UUID.fromString(subject);

        LOG.info("✅ AUTHENTICATED USER ID: " + userId);
        LOG.info("====================================");

        return userId;
    }

    public String getEmail() {

        String email = jwt != null
            ? jwt.getClaim("email")
            : null;

        LOG.info("📧 getEmail(): " + email);

        return email;
    }

    public String getRole() {

        if (jwt == null || jwt.getClaim("role") == null) {

            LOG.warning("⚠ No role found, using TEAM_MEMBER (DEV MODE)");

            return "TEAM_MEMBER";
        }

        String role = jwt.getClaim("role");

        LOG.info("🛡 getRole(): " + role);

        return role;
    }

    public void assertTeamMember() {

        LOG.info("🔍 Checking TEAM_MEMBER access...");

        // DEV MODE
        if (jwt == null || jwt.getSubject() == null) {

            LOG.warning("⚠ DEV MODE ENABLED - Access granted");

            return;
        }

        if (!"TEAM_MEMBER".equals(getRole())) {

            LOG.severe("❌ ACCESS DENIED");
            LOG.severe("Required role: TEAM_MEMBER");
            LOG.severe("Current role: " + getRole());

            throw new SecurityException(
                "Access denied: requires TEAM_MEMBER role"
            );
        }

        LOG.info("✅ TEAM_MEMBER access granted");
    }
}


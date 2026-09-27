package com.smms.assistance.infrastructure.client;

import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ProjectManagerClientWrapper {

    private static final Logger LOG = Logger.getLogger(ProjectManagerClientWrapper.class.getName());

    @Inject
    JsonWebToken jwt;

    @Inject
    @io.quarkus.cache.CacheName("my_projects")
    io.quarkus.cache.Cache myProjectsCache;

    public List<ProjectOutput> getMyProjects() {
        if (jwt == null || jwt.getSubject() == null) {
            return getMyProjectsUncached();
        }

        String userSubject = jwt.getSubject();
        long start = System.currentTimeMillis();
        java.util.concurrent.atomic.AtomicBoolean miss = new java.util.concurrent.atomic.AtomicBoolean(false);

        ProjectOutputListWrapper wrapper = myProjectsCache.get(userSubject, key -> {
            miss.set(true);
            return new ProjectOutputListWrapper(getMyProjectsUncached());
        }).await().indefinitely();

        long elapsed = System.currentTimeMillis() - start;
        LOG.info("[REDIS CACHE " + (miss.get() ? "MISS" : "HIT") + "] my_projects -> " + elapsed + "ms");

        return wrapper != null ? wrapper.projects : List.of();
    }

    private List<ProjectOutput> getMyProjectsUncached() {
        LOG.info("📡 Calling Project Manager: GetMyProjects (Uncached)...");
        long startTime = System.currentTimeMillis();

        try {
            var builder = TypesafeGraphQLClientBuilder.newBuilder()
                    .configKey("project-manager-service");

            if (jwt != null && jwt.getRawToken() != null) {
                builder = builder.header("Authorization", "Bearer " + jwt.getRawToken());
            }

            ProjectManagerClient client = builder.build(ProjectManagerClient.class);

            List<ProjectOutput> response = client.getMyProjects();

            LOG.info("✅ GetMyProjects OK (" +
                    (System.currentTimeMillis() - startTime) + "ms)");

            return response;

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "❌ GetMyProjects failed", e);
            throw e;
        }
    }

    public EffectiveScheduleOutput getEffectiveSchedule(UUID userId, UUID projectId, LocalDate date) {
        LOG.info("📡 Calling Project Manager: GetEffectiveSchedule...");
        long startTime = System.currentTimeMillis();
        try {
            var builder = TypesafeGraphQLClientBuilder.newBuilder()
                    .configKey("project-manager-service");
            if (jwt != null && jwt.getRawToken() != null) {
                builder = builder.header("Authorization", "Bearer " + jwt.getRawToken());
            }
            ProjectManagerClient client = builder.build(ProjectManagerClient.class);
            EffectiveScheduleOutput result = client.getEffectiveSchedule(userId, projectId, date.toString());
            LOG.info("✅ GetEffectiveSchedule OK (" +
                    (System.currentTimeMillis() - startTime) + "ms)");
            return result;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "❌ GetEffectiveSchedule failed", e);
            return null;
        }
    }

    public boolean isHoliday(UUID projectId, LocalDate date) {
        LOG.info("📡 Calling Project Manager: IsHoliday...");
        long startTime = System.currentTimeMillis();
        try {
            var builder = TypesafeGraphQLClientBuilder.newBuilder()
                    .configKey("project-manager-service");
            if (jwt != null && jwt.getRawToken() != null) {
                builder = builder.header("Authorization", "Bearer " + jwt.getRawToken());
            }
            ProjectManagerClient client = builder.build(ProjectManagerClient.class);
            boolean result = client.isHoliday(projectId, date);
            LOG.info("✅ IsHoliday=" + result + " (" +
                    (System.currentTimeMillis() - startTime) + "ms)");
            return result;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "❌ IsHoliday failed", e);
            return false;
        }
    }

    public boolean hasApprovedVacation(UUID userId, LocalDate date) {
        LOG.info("📡 Calling Project Manager: HasApprovedVacation...");
        long startTime = System.currentTimeMillis();
        try {
            var builder = TypesafeGraphQLClientBuilder.newBuilder()
                    .configKey("project-manager-service");
            if (jwt != null && jwt.getRawToken() != null) {
                builder = builder.header("Authorization", "Bearer " + jwt.getRawToken());
            }
            ProjectManagerClient client = builder.build(ProjectManagerClient.class);
            boolean result = client.hasApprovedVacation(userId, date);
            LOG.info("✅ HasApprovedVacation=" + result + " (" +
                    (System.currentTimeMillis() - startTime) + "ms)");
            return result;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "❌ HasApprovedVacation failed", e);
            return false;
        }
    }

    public void createAlert(
            UUID userId,
            UUID projectId,
            String type,
            String detail,
            Double latitude,
            Double longitude
    ) {
        String token = (jwt != null) ? jwt.getRawToken() : null;
        createAlert(userId, projectId, type, detail, latitude, longitude, token);
    }

    public void createAlert(
            UUID userId,
            UUID projectId,
            String type,
            String detail,
            Double latitude,
            Double longitude,
            String rawToken
    ) {
        LOG.info("📡 Calling Project Manager: createAlert (with explicit token)...");
        long startTime = System.currentTimeMillis();

        try {
            var builder = TypesafeGraphQLClientBuilder.newBuilder()
                    .configKey("project-manager-service");

            if (rawToken != null) {
                builder = builder.header("Authorization", "Bearer " + rawToken);
            }

            ProjectManagerClient client = builder.build(ProjectManagerClient.class);

            client.createAlert(userId, projectId, type, detail, latitude, longitude);

            LOG.info("✅ createAlert OK (" +
                    (System.currentTimeMillis() - startTime) + "ms)");

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "❌ createAlert failed", e);
            // no throw (check-in must NOT break)
        }
    }
}
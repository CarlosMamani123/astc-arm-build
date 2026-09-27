package com.smms.assistance.infrastructure.controller.rest;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/api/cache")
public class CacheInvalidationController {

    private static final Logger LOG = Logger.getLogger(CacheInvalidationController.class);

    @Inject
    @CacheName("my_projects")
    Cache myProjectsCache;

    @POST
    @Path("/invalidate-projects")
    @io.smallrye.common.annotation.Blocking
    public Response invalidateProjects(String body) {
        try {
            JsonObject json = new JsonObject(body);
            String eventType = json.getString("eventType");
            String projectId = json.getString("projectId");
            JsonArray userIds = json.getJsonArray("userIds");

            LOG.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            LOG.info("📥 [CACHE] Received invalidation request: " + eventType + " for project " + projectId);

            if (userIds != null && !userIds.isEmpty()) {
                // Invalidate only specific users
                LOG.info("🗑️  [CACHE] Invalidating cache for " + userIds.size() + " specific users...");
                for (Object userIdObj : userIds) {
                    String userId = userIdObj.toString();
                    myProjectsCache.invalidate(userId).await().indefinitely();
                    LOG.info("🗑️  [CACHE] Invalidated cache for user: " + userId);
                }
            } else {
                // Fallback: invalidate all if no user IDs provided
                LOG.info("🗑️  [CACHE] No user IDs provided, invalidating ALL cache entries...");
                myProjectsCache.invalidateAll().await().indefinitely();
            }

            LOG.info("✅ [CACHE] Cache invalidation completed successfully");
            LOG.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            return Response.ok("{\"status\": \"ok\", \"message\": \"Cache invalidated\"}").build();
        } catch (Exception e) {
            LOG.error("❌ [CACHE] Error processing invalidation request: " + e.getMessage(), e);
            return Response.serverError().entity("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}").build();
        }
    }
}

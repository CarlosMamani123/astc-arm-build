package com.smms.assistance.infrastructure.controller.graphql;

import io.smallrye.graphql.api.ErrorExtensionProvider;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Centralized GraphQL error handling.
 * Domain exceptions are mapped to appropriate GraphQL errors.
 */
@ApplicationScoped
public class GraphQLErrorHandler {

    private static final Logger LOG = Logger.getLogger(GraphQLErrorHandler.class);

    public static void handleException(Exception e) {
        LOG.error("GraphQL operation error", e);
        if (e instanceof SecurityException) {
            throw new RuntimeException("ACCESS_DENIED: " + e.getMessage());
        }
        if (e instanceof IllegalArgumentException) {
            throw new RuntimeException("NOT_FOUND: " + e.getMessage());
        }
        if (e instanceof IllegalStateException) {
            throw new RuntimeException("CONFLICT: " + e.getMessage());
        }
        throw new RuntimeException("INTERNAL_ERROR: " + e.getMessage());
    }
}

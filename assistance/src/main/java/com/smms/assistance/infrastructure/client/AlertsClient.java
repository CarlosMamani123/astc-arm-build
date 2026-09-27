package com.smms.assistance.infrastructure.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;

import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AlertsClient {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AlertsClient.class);
    private final Client client = ClientBuilder.newClient();

    @Inject
    @ConfigProperty(name = "app.alerts.url", defaultValue = "http://astc-svc-worker-alerts-service.astc-app.svc.cluster.local:8080/api/analyze-attendance")
    String alertsUrl;

    public AlertResponsePayload sendAlert(AlertRequestPayload payload) {
        LOG.info("📤 [Inter-service Call] Payload generated for Alerts Service: attendanceId={}, userId={}, projectId={}", 
            payload.attendanceId, payload.userId, payload.projectId);
        
        String targetUrl = alertsUrl;
        if (targetUrl == null || targetUrl.isBlank() || "http://alerts-service:8080/api/analyze-attendance".equals(targetUrl)) {
            targetUrl = "http://astc-svc-worker-alerts-service.astc-app.svc.cluster.local:8080/api/analyze-attendance";
        }

        long startTime = System.currentTimeMillis();
        try {
            LOG.info("📡 [Inter-service Call] Sending check-in payload to Alerts Service at: {}", targetUrl);
            
            var response = client.target(targetUrl)
                    .request()
                    .post(Entity.json(payload));

            long latency = System.currentTimeMillis() - startTime;

            if (response.getStatus() == 200) {
                AlertResponsePayload result = response.readEntity(AlertResponsePayload.class);
                LOG.info("✅ [Inter-service Call] Alerts Service responded successfully. Latency: {}ms. Result: alert={}, type={}, severity={}, message={}", 
                    latency, result.alert, result.type, result.severity, result.message);
                return result;
            } else {
                String errorBody = response.readEntity(String.class);
                LOG.error("❌ [Inter-service Call] Alerts Service returned error status: {}. Response: {}", response.getStatus(), errorBody);
                return null;
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            LOG.error("❌ [Inter-service Call] Alerts Service call failed. Latency: {}ms. Error: {}", latency, e.getMessage(), e);
            return null;
        }
    }
}
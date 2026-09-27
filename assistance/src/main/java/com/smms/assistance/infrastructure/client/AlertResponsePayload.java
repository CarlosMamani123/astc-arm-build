package com.smms.assistance.infrastructure.client;

public class AlertResponsePayload {
    public boolean alert;
    public String type;
    public String severity;
    public String message;

    public AlertResponsePayload() {}

    public AlertResponsePayload(boolean alert, String type, String severity, String message) {
        this.alert = alert;
        this.type = type;
        this.severity = severity;
        this.message = message;
    }
}

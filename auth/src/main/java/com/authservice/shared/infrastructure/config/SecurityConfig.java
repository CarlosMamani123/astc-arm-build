package com.authservice.shared.infrastructure.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.auth.LoginConfig;

@ApplicationPath("/")
@LoginConfig(authMethod = "MP-JWT")
public class SecurityConfig extends Application {
}
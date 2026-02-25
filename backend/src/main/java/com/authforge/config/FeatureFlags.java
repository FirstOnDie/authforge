package com.authforge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "authforge.features")
public class FeatureFlags {
    private boolean oauth2 = true;
    private boolean twoFactor = true;
    private boolean rateLimiting = true;
    private boolean emailVerification = true;
}

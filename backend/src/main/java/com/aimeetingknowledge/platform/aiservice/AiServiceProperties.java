package com.aimeetingknowledge.platform.aiservice;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai-service")
public record AiServiceProperties(
        String url,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}

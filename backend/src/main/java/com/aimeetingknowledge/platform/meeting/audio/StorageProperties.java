package com.aimeetingknowledge.platform.meeting.audio;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String audioDirectory,
        long maxFileSizeBytes,
        List<String> allowedContentTypes
) {
}

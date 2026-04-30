package com.xirr.calculator.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties(prefix = "app.access-request")
public record AppAccessRequestProperties(
        @NotNull Path storagePath,
        @NotNull String notifyEmail
) {
}

package com.xirr.calculator.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record AppRateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @Valid RateLimitRule login,
        @Valid RateLimitRule accessRequest,
        @Valid RateLimitRule xirr
) {

    public record RateLimitRule(
            @Min(1) int maxRequests,
            @Min(1) int windowSeconds
    ) {
    }
}

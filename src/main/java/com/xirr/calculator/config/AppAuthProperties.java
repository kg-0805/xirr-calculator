package com.xirr.calculator.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AppAuthProperties(
        @NotEmpty List<@Valid AuthUser> users
) {

    public AppAuthProperties {
        Set<String> normalizedUsernames = new HashSet<>();
        for (AuthUser user : users) {
            String normalizedUsername = user.username().trim().toLowerCase(Locale.ENGLISH);
            if (!normalizedUsernames.add(normalizedUsername)) {
                throw new IllegalArgumentException("Duplicate usernames are not allowed in app.auth.users.");
            }
        }
    }

    public boolean containsUsername(String username) {
        if (username == null) {
            return false;
        }

        String normalizedUsername = username.trim().toLowerCase(Locale.ENGLISH);
        return users.stream()
                .map(AuthUser::username)
                .map(value -> value.trim().toLowerCase(Locale.ENGLISH))
                .anyMatch(normalizedUsername::equals);
    }

    public record AuthUser(
            @NotBlank String username,
            @NotBlank String passwordHash
    ) {
    }
}

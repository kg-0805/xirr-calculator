package com.xirr.calculator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xirr.calculator.config.AppAccessRequestProperties;
import com.xirr.calculator.config.AppAuthProperties;
import com.xirr.calculator.exception.AccessRequestStorageException;
import com.xirr.calculator.model.AccessRequestForm;
import com.xirr.calculator.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AccessRequestService {

    private final AppAccessRequestProperties accessRequestProperties;
    private final AppAuthProperties authProperties;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ReentrantLock writeLock = new ReentrantLock();

    public AccessRequestService(AppAccessRequestProperties accessRequestProperties,
                                AppAuthProperties authProperties,
                                UserRepository userRepository,
                                ObjectMapper objectMapper) {
        this.accessRequestProperties = accessRequestProperties;
        this.authProperties = authProperties;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public boolean usernameAlreadyExists(String username) {
        return authProperties.containsUsername(username)
                || userRepository.existsByEmail(username.toLowerCase().trim());
    }

    public void submitRequest(AccessRequestForm form, String clientIp) {
        AccessRequestRecord record = new AccessRequestRecord(
                UUID.randomUUID().toString(),
                Instant.now(),
                safeTrim(clientIp),
                safeTrim(form.getFullName()),
                normalizeEmail(form.getEmail()),
                safeTrim(form.getPurpose())
        );

        Path storagePath = accessRequestProperties.storagePath();
        Path parent = storagePath.getParent();

        writeLock.lock();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String json = objectMapper.writeValueAsString(record) + System.lineSeparator();
            Files.writeString(
                    storagePath,
                    json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new AccessRequestStorageException("Unable to store the access request.", exception);
        } finally {
            writeLock.unlock();
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        return safeTrim(value).toLowerCase(Locale.ENGLISH);
    }

    private record AccessRequestRecord(
            String requestId,
            Instant submittedAt,
            String clientIp,
            String fullName,
            String email,
            String purpose
    ) {
    }
}

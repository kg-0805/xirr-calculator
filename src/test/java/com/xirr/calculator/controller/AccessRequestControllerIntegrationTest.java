package com.xirr.calculator.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class AccessRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final Path storagePath = Path.of("target", "test-data", "access-requests.jsonl").toAbsolutePath();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.access-request.storage-path", () -> storagePath.toString());
    }

    @BeforeEach
    void cleanStorage() throws Exception {
        Files.createDirectories(storagePath.getParent());
        Files.deleteIfExists(storagePath);
    }

    @Test
    void storesValidAccessRequestAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/access-request")
                        .param("fullName", "Kartik Gupta")
                        .param("email", "kartik@example.com")
                        .param("desiredUsername", "kartik.user")
                        .param("purpose", "Please create a login so I can securely review my mutual fund XIRR calculations.")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?requestSuccess"));

        assertThat(storagePath).exists();
        String content = Files.readString(storagePath);
        assertThat(content).contains("kartik@example.com");
        assertThat(content).contains("kartik.user");
        assertThat(content).contains("Kartik Gupta");
    }

    @Test
    void rejectsRequestWhenUsernameAlreadyExists() throws Exception {
        mockMvc.perform(post("/access-request")
                        .param("fullName", "Kartik Gupta")
                        .param("email", "kartik@example.com")
                        .param("desiredUsername", "investor")
                        .param("purpose", "Please create a login so I can securely review my mutual fund XIRR calculations.")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeHasFieldErrors("accessRequestForm", "desiredUsername"));

        assertThat(storagePath).doesNotExist();
    }
}

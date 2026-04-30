package com.xirr.calculator.config;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.rate-limit.login.max-requests=2",
        "app.rate-limit.login.window-seconds=60",
        "app.rate-limit.xirr.max-requests=1",
        "app.rate-limit.xirr.window-seconds=60"
})
@AutoConfigureMockMvc
class RateLimitingFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void blocksRepeatedLoginAttemptsFromSameIp() throws Exception {
        RequestPostProcessor ipAddress = remoteAddress("198.51.100.10");

                mockMvc.perform(post("/login")
                        .param("username", "investor")
                        .param("password", "wrong-password")
                        .with(csrf())
                        .with(ipAddress))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(post("/login")
                        .param("username", "investor")
                        .param("password", "wrong-password")
                        .with(csrf())
                        .with(ipAddress))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(post("/login")
                        .param("username", "investor")
                        .param("password", "wrong-password")
                        .with(csrf())
                        .with(ipAddress))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().string("X-RateLimit-Limit", "2"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }

    @Test
    @WithMockUser(username = "investor")
    void blocksRepeatedXirrUploadsFromSameIp() throws Exception {
        MockMultipartFile workbook = validWorkbook();
        RequestPostProcessor ipAddress = remoteAddress("198.51.100.20");

        mockMvc.perform(multipart("/api/xirr/calculate")
                        .file(workbook)
                        .with(csrf())
                        .with(ipAddress))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "1"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        mockMvc.perform(multipart("/api/xirr/calculate")
                        .file(validWorkbook())
                        .with(csrf())
                        .with(ipAddress))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.message", containsString("Too many xirr requests")))
                .andExpect(jsonPath("$.retryAfterSeconds").value(60));
    }

    private RequestPostProcessor remoteAddress(String ipAddress) {
        return request -> {
            request.setRemoteAddr(ipAddress);
            return request;
        };
    }

    private MockMultipartFile validWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("transactions");

            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Type");
            header.createCell(2).setCellValue("Amount");

            var row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("2024-01-10");
            row1.createCell(1).setCellValue("BUY");
            row1.createCell(2).setCellValue(10000);

            var row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("2025-01-15");
            row2.createCell(1).setCellValue("SELL");
            row2.createCell(2).setCellValue(12000);

            workbook.write(outputStream);

            return new MockMultipartFile(
                    "file",
                    "transactions.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }
}

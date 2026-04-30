package com.xirr.calculator.controller;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
class XirrControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "investor")
    void calculatesXirrForValidWorkbookUpload() throws Exception {
        MockMultipartFile workbook = validWorkbook();

        mockMvc.perform(multipart("/api/xirr/calculate").file(workbook).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(3))
                .andExpect(jsonPath("$.formattedXirr").value(org.hamcrest.Matchers.containsString("%")))
                .andExpect(jsonPath("$.transactions[0].type").value("BUY"))
                .andExpect(jsonPath("$.transactions[2].type").value("SELL"));
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
            row2.createCell(0).setCellValue("2024-04-05");
            row2.createCell(1).setCellValue("BUY");
            row2.createCell(2).setCellValue(7500);

            var row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("2025-01-15");
            row3.createCell(1).setCellValue("SELL");
            row3.createCell(2).setCellValue(22000);

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

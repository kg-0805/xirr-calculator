package com.xirr.calculator.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

@RestController
public class SampleDownloadController {

    @GetMapping("/api/sample-workbook")
    public ResponseEntity<byte[]> downloadSample() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-MM-dd"));

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Type");
            header.createCell(2).setCellValue("Amount");

            Object[][] data = {
                    {LocalDate.of(2024, 1, 10), "BUY", 10000},
                    {LocalDate.of(2024, 4, 5), "BUY", 7500},
                    {LocalDate.of(2024, 9, 20), "SELL", 5000},
                    {LocalDate.now(), "PRESENT", 22000}
            };

            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i + 1);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(java.sql.Date.valueOf((LocalDate) data[i][0]));
                dateCell.setCellStyle(dateStyle);
                row.createCell(1).setCellValue((String) data[i][1]);
                row.createCell(2).setCellValue(((Number) data[i][2]).doubleValue());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sample-xirr-workbook.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}

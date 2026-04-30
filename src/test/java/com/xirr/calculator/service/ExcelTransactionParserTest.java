package com.xirr.calculator.service;

import com.xirr.calculator.exception.InvalidWorkbookException;
import com.xirr.calculator.model.InvestmentTransaction;
import com.xirr.calculator.model.TransactionType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelTransactionParserTest {

    private final ExcelTransactionParser parser = new ExcelTransactionParser();

    @Test
    void parsesWorkbookWithValidTransactions() throws IOException {
        MockMultipartFile file = workbookOf(
                new Object[]{"Date", "Type", "Amount"},
                new Object[]{"2024-01-10", "BUY", 10_000},
                new Object[]{"2024-04-05", "BUY", 7_500},
                new Object[]{"2025-01-15", "SELL", 22_000}
        );

        List<InvestmentTransaction> transactions = parser.parse(file);

        assertThat(transactions).hasSize(3);
        assertThat(transactions.getFirst().type()).isEqualTo(TransactionType.BUY);
        assertThat(transactions.getLast().type()).isEqualTo(TransactionType.SELL);
        assertThat(transactions.get(1).amount()).isEqualByComparingTo(BigDecimal.valueOf(7500));
    }

    @Test
    void rejectsWorkbookWhenLastTransactionIsNotSell() throws IOException {
        MockMultipartFile file = workbookOf(
                new Object[]{"Date", "Type", "Amount"},
                new Object[]{"2024-01-10", "BUY", 10_000},
                new Object[]{"2024-04-05", "SELL", 7_500},
                new Object[]{"2024-05-01", "BUY", 2_000}
        );

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(InvalidWorkbookException.class)
                .hasMessageContaining("last transaction must be a SELL");
    }

    private MockMultipartFile workbookOf(Object[]... rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("transactions");
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                var row = sheet.createRow(rowIndex);
                Object[] values = rows[rowIndex];
                for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
                    var cell = row.createCell(columnIndex);
                    Object value = values[columnIndex];
                    if (value instanceof String stringValue) {
                        cell.setCellValue(stringValue);
                    } else if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    }
                }
            }

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

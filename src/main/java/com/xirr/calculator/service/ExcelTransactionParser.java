package com.xirr.calculator.service;

import com.xirr.calculator.exception.InvalidWorkbookException;
import com.xirr.calculator.model.InvestmentTransaction;
import com.xirr.calculator.model.TransactionType;
import org.apache.poi.EmptyFileException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ExcelTransactionParser {

    private static final List<DateTimeFormatter> SUPPORTED_DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)
    );

    public List<InvestmentTransaction> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidWorkbookException("Please upload an Excel workbook with your transactions.");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseSheet(workbook.getSheetAt(0));
        } catch (IOException | EmptyFileException | EncryptedDocumentException | NotOfficeXmlFileException exception) {
            throw new InvalidWorkbookException("The uploaded file is not a valid Excel workbook.");
        }
    }

    private List<InvestmentTransaction> parseSheet(Sheet sheet) {
        if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
            throw new InvalidWorkbookException("The workbook must have a header row and at least two transactions.");
        }

        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        Map<String, Integer> columnIndexes = resolveColumns(headerRow);
        List<InvestmentTransaction> transactions = new ArrayList<>();

        for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isBlank(row)) {
                continue;
            }

            LocalDate date = readDateCell(row.getCell(columnIndexes.get("date")), rowIndex);
            TransactionType type = readTypeCell(row.getCell(columnIndexes.get("type")), rowIndex);
            BigDecimal amount = readAmountCell(row.getCell(columnIndexes.get("amount")), rowIndex);

            if (amount.signum() <= 0) {
                throw new InvalidWorkbookException("Row " + (rowIndex + 1) + " has a non-positive amount.");
            }

            transactions.add(new InvestmentTransaction(date, type, amount));
        }

        validateTransactions(transactions);
        return transactions;
    }

    private Map<String, Integer> resolveColumns(Row headerRow) {
        if (headerRow == null) {
            throw new InvalidWorkbookException("The workbook header row is missing.");
        }

        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = readAsString(cell).trim().toLowerCase(Locale.ENGLISH);
            columns.put(header, cell.getColumnIndex());
        }

        if (!columns.containsKey("date") || !columns.containsKey("type") || !columns.containsKey("amount")) {
            throw new InvalidWorkbookException("Expected the first sheet to contain Date, Type, and Amount columns.");
        }

        return columns;
    }

    private boolean isBlank(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && !readAsString(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private LocalDate readDateCell(Cell cell, int rowIndex) {
        if (cell == null) {
            throw new InvalidWorkbookException("Row " + (rowIndex + 1) + " is missing a date.");
        }

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return Instant.ofEpochMilli(cell.getDateCellValue().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        String value = readAsString(cell);
        for (DateTimeFormatter formatter : SUPPORTED_DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }

        throw new InvalidWorkbookException("Row " + (rowIndex + 1) + " has an invalid date.");
    }

    private TransactionType readTypeCell(Cell cell, int rowIndex) {
        String value = readAsString(cell);
        try {
            return TransactionType.from(value);
        } catch (Exception exception) {
            throw new InvalidWorkbookException("Row " + (rowIndex + 1) + " must use BUY or SELL in the Type column.");
        }
    }

    private BigDecimal readAmountCell(Cell cell, int rowIndex) {
        if (cell == null) {
            throw new InvalidWorkbookException("Row " + (rowIndex + 1) + " is missing an amount.");
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }

        String value = readAsString(cell).replace(",", "").replaceAll("[^0-9.\\-]", "").trim();
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new InvalidWorkbookException("Row " + (rowIndex + 1) + " has an invalid amount.");
        }
    }

    private String readAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> switch (cell.getCachedFormulaResultType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
                case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
                default -> "";
            };
            case BLANK, _NONE, ERROR -> "";
        };
    }

    private void validateTransactions(List<InvestmentTransaction> transactions) {
        if (transactions.size() < 2) {
            throw new InvalidWorkbookException("Add at least one BUY and one SELL transaction.");
        }

        boolean hasBuy = transactions.stream().anyMatch(transaction -> transaction.type() == TransactionType.BUY);
        boolean hasSell = transactions.stream().anyMatch(transaction -> transaction.type() == TransactionType.SELL);

        if (!hasBuy || !hasSell) {
            throw new InvalidWorkbookException("The workbook must contain at least one BUY and one SELL transaction.");
        }

        if (transactions.getFirst().type() != TransactionType.BUY) {
            throw new InvalidWorkbookException("The first transaction must be a BUY entry.");
        }

        if (transactions.getLast().type() != TransactionType.SELL) {
            throw new InvalidWorkbookException("The last transaction must be a SELL entry.");
        }

        LocalDate previousDate = null;
        for (int index = 0; index < transactions.size(); index++) {
            LocalDate currentDate = transactions.get(index).date();
            if (previousDate != null && currentDate.isBefore(previousDate)) {
                throw new InvalidWorkbookException("Transactions must be listed in ascending date order.");
            }
            previousDate = currentDate;
        }
    }
}

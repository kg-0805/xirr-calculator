package com.xirr.calculator.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRowView(
        LocalDate date,
        TransactionType type,
        BigDecimal amount,
        BigDecimal signedCashFlow
) {
}

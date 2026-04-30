package com.xirr.calculator.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentTransaction(
        LocalDate date,
        TransactionType type,
        BigDecimal amount
) {

    public BigDecimal signedCashFlow() {
        return type == TransactionType.BUY ? amount.negate() : amount;
    }
}

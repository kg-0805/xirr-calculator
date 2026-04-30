package com.xirr.calculator.service;

import com.xirr.calculator.model.InvestmentTransaction;
import com.xirr.calculator.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XirrServiceTest {

    private final XirrService xirrService = new XirrService();

    @Test
    void calculatesAnnualizedReturnForSimpleCashFlow() {
        List<InvestmentTransaction> transactions = List.of(
                new InvestmentTransaction(LocalDate.of(2024, 1, 1), TransactionType.BUY, java.math.BigDecimal.valueOf(1000)),
                new InvestmentTransaction(LocalDate.of(2025, 1, 1), TransactionType.SELL, java.math.BigDecimal.valueOf(1100))
        );

        double result = xirrService.calculate(transactions).doubleValue();

        assertThat(result).isBetween(0.09d, 0.11d);
    }
}

package com.xirr.calculator.service;

import com.xirr.calculator.exception.InvalidWorkbookException;
import com.xirr.calculator.model.InvestmentTransaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class XirrService {

    private static final double MIN_RATE = -0.9999d;
    private static final double MAX_INITIAL_RATE = 10.0d;
    private static final double TOLERANCE = 1.0e-8d;
    private static final int MAX_ITERATIONS = 200;

    public BigDecimal calculate(List<InvestmentTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            throw new InvalidWorkbookException("No transactions were found in the uploaded workbook.");
        }

        double rate = solveWithNewtonRaphson(transactions);
        if (Double.isNaN(rate)) {
            rate = solveWithBisection(transactions);
        }

        if (Double.isNaN(rate) || Double.isInfinite(rate)) {
            throw new InvalidWorkbookException("Unable to calculate XIRR from the uploaded transactions.");
        }

        return BigDecimal.valueOf(rate);
    }

    private double solveWithNewtonRaphson(List<InvestmentTransaction> transactions) {
        double rate = 0.10d;

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            double npv = xnpv(rate, transactions);
            if (Math.abs(npv) < TOLERANCE) {
                return rate;
            }

            double derivative = derivative(rate, transactions);
            if (Math.abs(derivative) < TOLERANCE) {
                return Double.NaN;
            }

            double nextRate = rate - (npv / derivative);
            if (nextRate <= MIN_RATE || Double.isInfinite(nextRate) || Double.isNaN(nextRate)) {
                return Double.NaN;
            }
            rate = nextRate;
        }

        return Double.NaN;
    }

    private double solveWithBisection(List<InvestmentTransaction> transactions) {
        double lower = MIN_RATE;
        double upper = MAX_INITIAL_RATE;
        double npvLower = xnpv(lower, transactions);
        double npvUpper = xnpv(upper, transactions);

        while (npvLower * npvUpper > 0 && upper < 1_000_000d) {
            upper *= 2;
            npvUpper = xnpv(upper, transactions);
        }

        if (npvLower * npvUpper > 0) {
            return Double.NaN;
        }

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            double midpoint = (lower + upper) / 2;
            double npvMidpoint = xnpv(midpoint, transactions);

            if (Math.abs(npvMidpoint) < TOLERANCE) {
                return midpoint;
            }

            if (npvLower * npvMidpoint < 0) {
                upper = midpoint;
                npvUpper = npvMidpoint;
            } else {
                lower = midpoint;
                npvLower = npvMidpoint;
            }
        }

        return (lower + upper) / 2;
    }

    private double xnpv(double rate, List<InvestmentTransaction> transactions) {
        var firstDate = transactions.getFirst().date();
        double total = 0;

        for (InvestmentTransaction transaction : transactions) {
            double years = ChronoUnit.DAYS.between(firstDate, transaction.date()) / 365.0d;
            total += transaction.signedCashFlow().doubleValue() / Math.pow(1 + rate, years);
        }

        return total;
    }

    private double derivative(double rate, List<InvestmentTransaction> transactions) {
        var firstDate = transactions.getFirst().date();
        double total = 0;

        for (InvestmentTransaction transaction : transactions) {
            double years = ChronoUnit.DAYS.between(firstDate, transaction.date()) / 365.0d;
            total += (-years * transaction.signedCashFlow().doubleValue()) / Math.pow(1 + rate, years + 1);
        }

        return total;
    }
}

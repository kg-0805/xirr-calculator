package com.xirr.calculator.model;

public enum TransactionType {
    BUY,
    SELL;

    public static TransactionType from(String value) {
        return TransactionType.valueOf(value.trim().toUpperCase());
    }
}

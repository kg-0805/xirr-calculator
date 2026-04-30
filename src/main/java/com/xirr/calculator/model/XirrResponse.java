package com.xirr.calculator.model;

import java.math.BigDecimal;
import java.util.List;

public record XirrResponse(
        BigDecimal xirrPercentage,
        String formattedXirr,
        int transactionCount,
        BigDecimal totalInvested,
        BigDecimal totalRedeemed,
        BigDecimal profitOrLoss,
        List<TransactionRowView> transactions
) {
}

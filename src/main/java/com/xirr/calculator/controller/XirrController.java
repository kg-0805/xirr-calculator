package com.xirr.calculator.controller;

import com.xirr.calculator.model.InvestmentTransaction;
import com.xirr.calculator.model.TransactionRowView;
import com.xirr.calculator.model.XirrResponse;
import com.xirr.calculator.service.ExcelTransactionParser;
import com.xirr.calculator.service.XirrService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
@RequestMapping("/api/xirr")
public class XirrController {

    private final ExcelTransactionParser excelTransactionParser;
    private final XirrService xirrService;

    public XirrController(ExcelTransactionParser excelTransactionParser, XirrService xirrService) {
        this.excelTransactionParser = excelTransactionParser;
        this.xirrService = xirrService;
    }

    @PostMapping(path = "/calculate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public XirrResponse calculate(@RequestPart("file") MultipartFile file) {
        List<InvestmentTransaction> transactions = excelTransactionParser.parse(file);
        BigDecimal xirr = xirrService.calculate(transactions)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalInvested = transactions.stream()
                .filter(t -> t.type() == com.xirr.calculator.model.TransactionType.BUY)
                .map(InvestmentTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRedeemed = transactions.stream()
                .filter(t -> t.type() == com.xirr.calculator.model.TransactionType.SELL)
                .map(InvestmentTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransactionRowView> rowViews = transactions.stream()
                .map(transaction -> new TransactionRowView(
                        transaction.date(),
                        transaction.type(),
                        transaction.amount(),
                        transaction.signedCashFlow()))
                .toList();

        return new XirrResponse(
                xirr,
                xirr.stripTrailingZeros().toPlainString() + "%",
                rowViews.size(),
                totalInvested,
                totalRedeemed,
                totalRedeemed.subtract(totalInvested),
                rowViews);
    }
}

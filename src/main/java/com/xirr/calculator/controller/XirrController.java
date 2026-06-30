package com.xirr.calculator.controller;

import com.xirr.calculator.model.AppUser;
import com.xirr.calculator.model.InvestmentTransaction;
import com.xirr.calculator.model.TransactionRowView;
import com.xirr.calculator.model.XirrResponse;
import com.xirr.calculator.repository.UserRepository;
import com.xirr.calculator.service.EmailNotificationService;
import com.xirr.calculator.service.ExcelTransactionParser;
import com.xirr.calculator.service.XirrService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
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
    private final EmailNotificationService emailNotificationService;
    private final UserRepository userRepository;

    public XirrController(ExcelTransactionParser excelTransactionParser,
                          XirrService xirrService,
                          EmailNotificationService emailNotificationService,
                          UserRepository userRepository) {
        this.excelTransactionParser = excelTransactionParser;
        this.xirrService = xirrService;
        this.emailNotificationService = emailNotificationService;
        this.userRepository = userRepository;
    }

    @PostMapping(path = "/calculate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public XirrResponse calculate(@RequestPart("file") MultipartFile file, Authentication authentication) {
        List<InvestmentTransaction> transactions = excelTransactionParser.parse(file);
        BigDecimal xirr = xirrService.calculate(transactions)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalInvested = transactions.stream()
                .filter(t -> t.type() == com.xirr.calculator.model.TransactionType.BUY)
                .map(InvestmentTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRedeemed = transactions.stream()
                .filter(t -> t.type() == com.xirr.calculator.model.TransactionType.SELL
                        || t.type() == com.xirr.calculator.model.TransactionType.PRESENT)
                .map(InvestmentTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitOrLoss = totalRedeemed.subtract(totalInvested);

        String formattedXirr = xirr.stripTrailingZeros().toPlainString() + "%";

        List<TransactionRowView> rowViews = transactions.stream()
                .map(transaction -> new TransactionRowView(
                        transaction.date(),
                        transaction.type(),
                        transaction.amount(),
                        transaction.signedCashFlow()))
                .toList();

        // Send email notification to the authenticated user
        if (authentication != null && authentication.getName() != null) {
            String userEmail = authentication.getName();
            userRepository.findByEmail(userEmail).ifPresent(user ->
                    emailNotificationService.sendXirrResultNotification(
                            user.getFullName(),
                            user.getEmail(),
                            formattedXirr,
                            rowViews.size(),
                            totalInvested,
                            totalRedeemed,
                            profitOrLoss));
        }

        return new XirrResponse(
                xirr,
                formattedXirr,
                rowViews.size(),
                totalInvested,
                totalRedeemed,
                profitOrLoss,
                rowViews);
    }
}

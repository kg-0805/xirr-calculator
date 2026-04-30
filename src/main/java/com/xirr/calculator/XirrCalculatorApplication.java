package com.xirr.calculator;

import com.xirr.calculator.config.AppAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = AppAuthProperties.class)
@EnableAsync
public class XirrCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(XirrCalculatorApplication.class, args);
    }
}

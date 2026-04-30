package com.xirr.calculator.controller;

import com.xirr.calculator.exception.InvalidWorkbookException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = XirrController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidWorkbookException.class)
    public ResponseEntity<Map<String, String>> handleInvalidWorkbook(InvalidWorkbookException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Something went wrong while processing the workbook."));
    }
}

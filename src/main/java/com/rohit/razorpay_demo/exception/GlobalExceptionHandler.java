package com.rohit.razorpay_demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<?> handle(Throwable t) {
        t.printStackTrace(); // full error in console
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(
                        "error", t.getClass().getName(),
                        "message", String.valueOf(t.getMessage())
                )
        );
    }
}

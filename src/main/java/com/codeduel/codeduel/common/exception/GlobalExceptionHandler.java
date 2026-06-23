package com.codeduel.codeduel.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.JwtException;


@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. record for the structured error body
    public record ErrorResponse(String message) {}

    // 2. handler method for IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex)
    {
        String cleanMessage = ex.getBindingResult()
                            .getFieldError() != null ? ex.getBindingResult().getFieldError().getDefaultMessage() : "Validation Failed";
                            
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(cleanMessage));
    }


    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Invalid or expired token"));
}

}

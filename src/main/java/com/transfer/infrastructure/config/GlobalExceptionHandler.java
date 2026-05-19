package com.transfer.infrastructure.config;

import com.transfer.domain.exception.AccountNotFoundException;
import com.transfer.domain.exception.InsufficientFundsException;
import com.transfer.domain.exception.TransferNotFoundException;
import com.transfer.infrastructure.adapter.in.rest.dto.TransferDtos.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        log.warn("Cuenta no encontrada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(error("ACCOUNT_NOT_FOUND", ex.getMessage(), 404));
    }

    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransferNotFound(TransferNotFoundException ex) {
        log.warn("Transferencia no encontrada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(error("TRANSFER_NOT_FOUND", ex.getMessage(), 404));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Fondos insuficientes: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(error("INSUFFICIENT_FUNDS", ex.getMessage(), 422));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(error("INVALID_ARGUMENT", ex.getMessage(), 400));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("Validación fallida: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(error("VALIDATION_ERROR", message, 400));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error("INTERNAL_ERROR", "Error interno del servidor", 500));
    }

    private ErrorResponse error(String code, String message, int status) {
        return new ErrorResponse(code, message, status, LocalDateTime.now().toString());
    }
}

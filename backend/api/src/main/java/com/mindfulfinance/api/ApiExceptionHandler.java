package com.mindfulfinance.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DuplicateKeyException;

import static com.mindfulfinance.domain.shared.DomainErrorCode.ACCOUNT_DELETE_FORBIDDEN_HAS_TRANSACTIONS;
import com.mindfulfinance.domain.shared.DomainException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final String ACCOUNT_DELETE_FORBIDDEN_MESSAGE =
        "Нельзя удалить счет, пока у него есть транзакции.";

    @ExceptionHandler({AccountNotFoundException.class, TransactionNotFoundException.class, PersonalFinanceCardNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ApiError("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomainException(DomainException ex) {
        if (ex.code() == ACCOUNT_DELETE_FORBIDDEN_HAS_TRANSACTIONS) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError("CONFLICT", ACCOUNT_DELETE_FORBIDDEN_MESSAGE));
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ApiError("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ApiError("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler({IllegalStateException.class, DuplicateKeyException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ApiError("CONFLICT", ex.getMessage()));
    }

    public record ApiError(String error, String message) {}
}

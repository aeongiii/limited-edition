package com.sparta.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===============================
    // 400 Bad Request - 잘못된 요청 관련 예외
    // ===============================

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmailException(
            DuplicateEmailException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicatePaymentException(
            DuplicatePaymentException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(AuthNumberValidationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthNumberValidationException(
            AuthNumberValidationException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStockException(
            InsufficientStockException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(PaymentProcessException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentProcessException(
            PaymentProcessException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    // ===============================
    // 401 Unauthorized - 인증 실패 관련 예외
    // ===============================

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentialsException(
            InvalidCredentialsException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
    }

    // ===============================
    // 404 Not Found - 리소스 찾을 수 없음
    // ===============================

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(
            UserNotFoundException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFoundException(
            ProductNotFoundException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(ProductSnapshotNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductSnapshotNotFoundException(
            ProductSnapshotNotFoundException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotFoundException(
            PaymentNotFoundException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(WishlistItemNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWishlistItemNotFoundException(
            WishlistItemNotFoundException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFoundException(
            OrderNotFoundException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    // ===============================
    // 500 Internal Server Error - 서버 오류
    // ===============================

    @ExceptionHandler(MailSendFailureException.class)
    public ResponseEntity<Map<String, Object>> handleMailSendFailureException(
            MailSendFailureException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", request);
    }

    @ExceptionHandler(StockUpdateException.class)
    public ResponseEntity<Map<String, Object>> handleStockUpdateException(
            StockUpdateException e, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), request);
    }

    // ===============================
    // 공통 응답 생성 메서드
    // ===============================

    private ResponseEntity<Map<String, Object>> createErrorResponse(
            HttpStatus status, String message, HttpServletRequest request) {

        Map<String, Object> errorResponse = Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", request.getRequestURI()
        );

        return ResponseEntity.status(status).body(errorResponse);
    }
}

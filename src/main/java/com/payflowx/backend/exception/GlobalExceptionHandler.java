package com.payflowx.backend.exception;

import com.payflowx.backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for all REST controllers
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        private static final String CODE_VALIDATION = "VALIDATION_ERROR";
        private static final String CODE_NOT_FOUND = "PAYMENT_NOT_FOUND";
        private static final String CODE_DUPLICATE = "DUPLICATE_PAYMENT";
        private static final String CODE_BAD_REQUEST = "BAD_REQUEST";
        private static final String CODE_INTERNAL = "INTERNAL_ERROR";
    
    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        logger.warn("event=validation_failed path={} method={} violations={}",
                request.getRequestURI(), request.getMethod(), ex.getErrorCount());
        
        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
                        String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            validationErrors.add(new ErrorResponse.ValidationError(fieldName, errorMessage));
        });

        String primaryMessage = validationErrors.isEmpty()
                ? "Request validation failed. Please verify input fields and try again."
                : validationErrors.get(0).getMessage();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                CODE_VALIDATION,
                primaryMessage,
                request,
                validationErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        logger.warn("event=constraint_violation path={} method={} message={}",
                request.getRequestURI(), request.getMethod(), ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                CODE_VALIDATION,
                "One or more request values are invalid.",
                request,
                null
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        logger.warn("event=malformed_json path={} method={} message={}",
                request.getRequestURI(), request.getMethod(), ex.getMostSpecificCause().getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed Request",
                CODE_BAD_REQUEST,
                "Request body is malformed. Ensure JSON syntax and field types are correct.",
                request,
                null
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        logger.warn("event=type_mismatch path={} method={} parameter={} value={}",
                request.getRequestURI(), request.getMethod(), ex.getName(), ex.getValue());

        String message = "Invalid value for parameter '%s'. Expected type: %s."
                .formatted(ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Parameter",
                CODE_BAD_REQUEST,
                message,
                request,
                null
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        logger.warn("event=missing_parameter path={} method={} parameter={}",
                request.getRequestURI(), request.getMethod(), ex.getParameterName());

        String message = "Required request parameter '%s' is missing.".formatted(ex.getParameterName());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Missing Parameter",
                CODE_BAD_REQUEST,
                message,
                request,
                null
        );
    }
    
    /**
     * Handle payment validation exceptions
     */
    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<ErrorResponse> handlePaymentValidationException(
            PaymentValidationException ex,
            HttpServletRequest request) {

        logger.warn("event=payment_validation_failed path={} method={} message={}",
                request.getRequestURI(), request.getMethod(), ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Payment Validation Error",
                CODE_VALIDATION,
                ex.getMessage(),
                request,
                null
        );
    }
    
    /**
     * Handle payment not found exceptions
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(
            PaymentNotFoundException ex,
            HttpServletRequest request) {

        logger.warn("event=payment_not_found path={} method={} message={}",
                request.getRequestURI(), request.getMethod(), ex.getMessage());

        String notFoundMessage = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? "The requested payment was not found."
                : "Payment with reference '%s' was not found.".formatted(ex.getMessage());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Payment Not Found",
                CODE_NOT_FOUND,
                notFoundMessage,
                request,
                null
        );
    }
    
    /**
     * Handle duplicate payment exceptions
     */
    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePaymentException(
            DuplicatePaymentException ex,
            HttpServletRequest request) {

        logger.warn("event=duplicate_payment path={} method={} message={}",
                request.getRequestURI(), request.getMethod(), ex.getMessage());

        return buildResponse(
                HttpStatus.CONFLICT,
                "Duplicate Payment",
                CODE_DUPLICATE,
                "A payment with the same reference already exists.",
                request,
                null
        );
    }

        /**
         * Handle duplicate key violations from database constraints
         */
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
                        DataIntegrityViolationException ex,
                        HttpServletRequest request) {

                logger.warn("event=data_integrity_violation path={} method={} message={}",
                                request.getRequestURI(), request.getMethod(), ex.getMostSpecificCause().getMessage());

                return buildResponse(
                                HttpStatus.CONFLICT,
                                "Duplicate Payment",
                                CODE_DUPLICATE,
                                "Duplicate payment reference. Please submit a unique payment reference.",
                                request,
                                null
                );
        }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        logger.error("event=unexpected_error path={} method={} message={}",
                request.getRequestURI(), request.getMethod(), ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                CODE_INTERNAL,
                "Something went wrong while processing the request. Please try again later.",
                request,
                null
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String errorCode,
            String message,
            HttpServletRequest request,
            List<ErrorResponse.ValidationError> validationErrors) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .errorCode(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .method(request.getMethod())
                .correlationId(MDC.get("correlationId"))
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
}

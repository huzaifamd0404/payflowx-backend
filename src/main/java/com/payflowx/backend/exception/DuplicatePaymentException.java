package com.payflowx.backend.exception;

/**
 * Exception thrown when a duplicate payment reference is detected
 */
public class DuplicatePaymentException extends RuntimeException {
    
    public DuplicatePaymentException(String message) {
        super(message);
    }
    
    public DuplicatePaymentException(String paymentReference, Throwable cause) {
        super("Payment with reference " + paymentReference + " already exists", cause);
    }
}

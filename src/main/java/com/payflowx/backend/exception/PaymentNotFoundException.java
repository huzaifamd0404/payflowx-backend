package com.payflowx.backend.exception;

/**
 * Exception thrown when a payment is not found
 */
public class PaymentNotFoundException extends RuntimeException {
    
    public PaymentNotFoundException(String message) {
        super(message);
    }
    
    public PaymentNotFoundException(String paymentReference) {
        super("Payment not found with reference: " + paymentReference);
    }
}

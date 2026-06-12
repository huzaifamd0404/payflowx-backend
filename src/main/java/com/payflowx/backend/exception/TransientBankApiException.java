package com.payflowx.backend.exception;

/**
 * Exception for transient bank-side failures that should be retried.
 */
public class TransientBankApiException extends RuntimeException {

    public TransientBankApiException(String message) {
        super(message);
    }

    public TransientBankApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

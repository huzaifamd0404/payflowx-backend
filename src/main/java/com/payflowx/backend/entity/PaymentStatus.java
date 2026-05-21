package com.payflowx.backend.entity;

/**
 * Enumeration representing the various states of a payment transaction.
 */
public enum PaymentStatus {
    /**
     * Payment has been initiated but not yet validated
     */
    INITIATED,
    
    /**
     * Payment has been validated and ready for processing
     */
    VALIDATED,
    
    /**
     * Payment is currently being processed
     */
    PROCESSING,
    
    /**
     * Payment has been successfully completed
     */
    SUCCESS,
    
    /**
     * Payment has failed
     */
    FAILED,
    
    /**
     * Payment is being retried after a failure
     */
    RETRYING
}

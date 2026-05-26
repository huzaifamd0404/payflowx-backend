package com.payflowx.backend.service;

import com.payflowx.backend.dto.PaymentRequest;
import com.payflowx.backend.dto.PaymentResponse;

/**
 * Service interface for payment operations
 */
public interface PaymentService {
    
    /**
     * Create a new payment
     * @param request Payment creation request
     * @return Created payment response
     */
    PaymentResponse createPayment(PaymentRequest request);
    
    /**
     * Get payment by reference
     * @param paymentReference Unique payment reference
     * @return Payment response
     */
    PaymentResponse getPaymentByReference(String paymentReference);
}

package com.payflowx.backend.service;

import com.payflowx.backend.dto.BankProcessingResult;
import com.payflowx.backend.entity.Payment;

/**
 * Service interface for bank processing operations
 */
public interface BankService {
    
    /**
     * Process payment with bank (mock implementation)
     * @param payment Payment to process
     * @return Bank processing result
     */
    BankProcessingResult processPayment(Payment payment);
}

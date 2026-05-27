package com.payflowx.backend.service;

import com.payflowx.backend.dto.BankProcessingResult;
import com.payflowx.backend.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

/**
 * Mock implementation of BankService for testing and development
 * Simulates bank payment processing with random success/failure outcomes
 */
@Service
public class MockBankServiceImpl implements BankService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockBankServiceImpl.class);
    
    private static final int MIN_PROCESSING_DELAY_MS = 500;
    private static final int MAX_PROCESSING_DELAY_MS = 3000;
    private static final double SUCCESS_RATE = 0.7; // 70% success rate
    
    private final Random random = new Random();
    
    @Override
    public BankProcessingResult processPayment(Payment payment) {
        logger.info("Processing payment {} with mock bank", payment.getPaymentReference());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Simulate artificial delay (random between 500ms - 3000ms)
            int delay = MIN_PROCESSING_DELAY_MS + 
                       random.nextInt(MAX_PROCESSING_DELAY_MS - MIN_PROCESSING_DELAY_MS);
            logger.debug("Simulating bank processing delay of {}ms", delay);
            Thread.sleep(delay);
            
            // Randomly determine success or failure
            boolean isSuccess = random.nextDouble() < SUCCESS_RATE;
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (isSuccess) {
                return buildSuccessResult(payment, processingTime);
            } else {
                return buildFailureResult(payment, processingTime);
            }
            
        } catch (InterruptedException e) {
            logger.error("Bank processing interrupted for payment {}", payment.getPaymentReference(), e);
            Thread.currentThread().interrupt();
            
            long processingTime = System.currentTimeMillis() - startTime;
            return BankProcessingResult.builder()
                    .success(false)
                    .responseCode("ERR_INTERRUPTED")
                    .message("Processing interrupted")
                    .processingTimeMs(processingTime)
                    .build();
        }
    }
    
    /**
     * Build success result
     */
    private BankProcessingResult buildSuccessResult(Payment payment, long processingTime) {
        String transactionId = generateTransactionId();
        
        logger.info("Mock bank approved payment {} with transaction ID: {}", 
                   payment.getPaymentReference(), transactionId);
        
        return BankProcessingResult.builder()
                .success(true)
                .transactionId(transactionId)
                .responseCode("00")
                .message("Transaction approved")
                .processingTimeMs(processingTime)
                .build();
    }
    
    /**
     * Build failure result
     */
    private BankProcessingResult buildFailureResult(Payment payment, long processingTime) {
        // Randomly select a failure reason
        FailureReason reason = getRandomFailureReason();
        
        logger.warn("Mock bank declined payment {} with reason: {} ({})", 
                   payment.getPaymentReference(), reason.getMessage(), reason.getCode());
        
        return BankProcessingResult.builder()
                .success(false)
                .responseCode(reason.getCode())
                .message(reason.getMessage())
                .processingTimeMs(processingTime)
                .build();
    }
    
    /**
     * Generate mock transaction ID
     */
    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 16).toUpperCase();
    }
    
    /**
     * Get random failure reason
     */
    private FailureReason getRandomFailureReason() {
        FailureReason[] reasons = FailureReason.values();
        return reasons[random.nextInt(reasons.length)];
    }
    
    /**
     * Enum for mock failure reasons
     */
    private enum FailureReason {
        INSUFFICIENT_FUNDS("51", "Insufficient funds"),
        INVALID_CARD("14", "Invalid card number"),
        EXPIRED_CARD("54", "Card expired"),
        SUSPECTED_FRAUD("59", "Suspected fraud"),
        DO_NOT_HONOR("05", "Do not honor"),
        TRANSACTION_NOT_PERMITTED("57", "Transaction not permitted"),
        EXCEEDS_WITHDRAWAL_LIMIT("61", "Exceeds withdrawal amount limit"),
        RESTRICTED_CARD("62", "Restricted card"),
        SECURITY_VIOLATION("63", "Security violation"),
        BANK_TIMEOUT("68", "Bank timeout");
        
        private final String code;
        private final String message;
        
        FailureReason(String code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getMessage() {
            return message;
        }
    }
}

package com.payflowx.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for bank processing result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankProcessingResult {
    
    private boolean success;
    private String transactionId;
    private String responseCode;
    private String message;
    private long processingTimeMs;
}

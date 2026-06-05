package com.payflowx.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO published to Kafka after payment processing completes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEventDto {

    private String paymentReference;
    private String status;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}

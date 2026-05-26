package com.payflowx.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for payment creation request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    
    @NotBlank(message = "Customer ID is required")
    @Size(min = 3, max = 100, message = "Customer ID must be between 3 and 100 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Customer ID must contain only uppercase letters and numbers")
    private String customerId;
    
    @NotBlank(message = "Merchant ID is required")
    @Size(min = 3, max = 100, message = "Merchant ID must be between 3 and 100 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Merchant ID must contain only uppercase letters and numbers")
    private String merchantId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999999.99", message = "Amount exceeds maximum limit")
    @Digits(integer = 9, fraction = 2, message = "Amount must have at most 9 integer digits and 2 decimal places")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code (e.g., USD, EUR, GBP)")
    private String currency;
}

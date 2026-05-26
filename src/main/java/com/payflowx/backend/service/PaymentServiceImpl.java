package com.payflowx.backend.service;

import com.payflowx.backend.dto.PaymentRequest;
import com.payflowx.backend.dto.PaymentResponse;
import com.payflowx.backend.entity.Payment;
import com.payflowx.backend.entity.PaymentEvent;
import com.payflowx.backend.entity.PaymentStatus;
import com.payflowx.backend.exception.DuplicatePaymentException;
import com.payflowx.backend.exception.PaymentNotFoundException;
import com.payflowx.backend.exception.PaymentValidationException;
import com.payflowx.backend.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of PaymentService
 */
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    
    private static final Set<String> SUPPORTED_CURRENCIES = new HashSet<>(Arrays.asList(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "INR", "SGD"
    ));
    
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");
    
    private final PaymentRepository paymentRepository;
    
    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        logger.info("Creating payment for customer: {} and merchant: {}", 
                    request.getCustomerId(), request.getMerchantId());
        
        // Validate request
        validatePaymentRequest(request);
        
        // Generate unique payment reference
        String paymentReference = generatePaymentReference(request);
        
        // Check for duplicate payment reference
        if (paymentRepository.existsByPaymentReference(paymentReference)) {
            logger.error("Duplicate payment reference detected: {}", paymentReference);
            throw new DuplicatePaymentException("Payment with reference " + paymentReference + " already exists");
        }
        
        // Create payment entity
        Payment payment = Payment.builder()
                .paymentReference(paymentReference)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .customerId(request.getCustomerId())
                .merchantId(request.getMerchantId())
                .status(PaymentStatus.INITIATED)
                .build();
        
        // Create initial payment event
        PaymentEvent initiationEvent = PaymentEvent.builder()
                .status(PaymentStatus.INITIATED)
                .eventType("PAYMENT_INITIATED")
                .message("Payment initiated by customer " + request.getCustomerId())
                .source("API")
                .build();
        
        payment.addEvent(initiationEvent);
        
        // Save payment
        Payment savedPayment = paymentRepository.save(payment);
        
        logger.info("Payment created successfully with reference: {}", paymentReference);
        
        return mapToResponse(savedPayment);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String paymentReference) {
        logger.info("Fetching payment with reference: {}", paymentReference);
        
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new PaymentNotFoundException(paymentReference));
        
        return mapToResponse(payment);
    }
    
    /**
     * Validate payment request
     */
    private void validatePaymentRequest(PaymentRequest request) {
        // Validate amount
        validateAmount(request.getAmount());
        
        // Validate currency
        validateCurrency(request.getCurrency());
        
        // Validate customer ID
        validateCustomerId(request.getCustomerId());
        
        // Validate merchant ID
        validateMerchantId(request.getMerchantId());
        
        logger.debug("Payment request validation successful");
    }
    
    /**
     * Validate amount
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new PaymentValidationException("Amount cannot be null");
        }
        
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            throw new PaymentValidationException("Amount must be greater than or equal to " + MIN_AMOUNT);
        }
        
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new PaymentValidationException("Amount exceeds maximum limit of " + MAX_AMOUNT);
        }
        
        if (amount.scale() > 2) {
            throw new PaymentValidationException("Amount cannot have more than 2 decimal places");
        }
    }
    
    /**
     * Validate currency
     */
    private void validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new PaymentValidationException("Currency cannot be null or empty");
        }
        
        if (!SUPPORTED_CURRENCIES.contains(currency.toUpperCase())) {
            throw new PaymentValidationException("Unsupported currency: " + currency + 
                    ". Supported currencies: " + SUPPORTED_CURRENCIES);
        }
    }
    
    /**
     * Validate customer ID
     */
    private void validateCustomerId(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new PaymentValidationException("Customer ID cannot be null or empty");
        }
        
        if (customerId.length() < 3) {
            throw new PaymentValidationException("Customer ID must be at least 3 characters long");
        }
        
        if (customerId.length() > 100) {
            throw new PaymentValidationException("Customer ID cannot exceed 100 characters");
        }
        
        if (!customerId.matches("^[A-Z0-9]+$")) {
            throw new PaymentValidationException("Customer ID must contain only uppercase letters and numbers");
        }
    }
    
    /**
     * Validate merchant ID
     */
    private void validateMerchantId(String merchantId) {
        if (merchantId == null || merchantId.trim().isEmpty()) {
            throw new PaymentValidationException("Merchant ID cannot be null or empty");
        }
        
        if (merchantId.length() < 3) {
            throw new PaymentValidationException("Merchant ID must be at least 3 characters long");
        }
        
        if (merchantId.length() > 100) {
            throw new PaymentValidationException("Merchant ID cannot exceed 100 characters");
        }
        
        if (!merchantId.matches("^[A-Z0-9]+$")) {
            throw new PaymentValidationException("Merchant ID must contain only uppercase letters and numbers");
        }
    }
    
    /**
     * Generate unique payment reference
     * Format: PAY-{YYYYMMDD}-{HHMMSS}-{UUID}
     */
    private String generatePaymentReference(PaymentRequest request) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String reference = String.format("PAY-%s-%s", timestamp, uniqueId);
        
        logger.debug("Generated payment reference: {}", reference);
        return reference;
    }
    
    /**
     * Map Payment entity to PaymentResponse DTO
     */
    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .customerId(payment.getCustomerId())
                .merchantId(payment.getMerchantId())
                .status(payment.getStatus())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}

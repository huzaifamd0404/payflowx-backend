package com.payflowx.backend.service;

import com.payflowx.backend.dto.BankProcessingResult;
import com.payflowx.backend.dto.PaymentEventDto;
import com.payflowx.backend.dto.PaymentRequest;
import com.payflowx.backend.dto.PaymentResponse;
import com.payflowx.backend.entity.Payment;
import com.payflowx.backend.entity.PaymentEvent;
import com.payflowx.backend.entity.PaymentStatus;
import com.payflowx.backend.exception.DuplicatePaymentException;
import com.payflowx.backend.exception.PaymentNotFoundException;
import com.payflowx.backend.exception.PaymentValidationException;
import com.payflowx.backend.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
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
    private static final int MAX_REFERENCE_GENERATION_RETRIES = 5;
    private static final String PAYMENT_STATUS_CACHE_KEY_PREFIX = "payment:status:";
    
    private final PaymentRepository paymentRepository;
    private final BankService bankService;
    private final PaymentEventProducerService paymentEventProducerService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final long paymentStatusCacheTtlMinutes;
    
    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            BankService bankService,
            PaymentEventProducerService paymentEventProducerService,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.cache.payment-status.ttl-minutes:30}") long paymentStatusCacheTtlMinutes) {
        this.paymentRepository = paymentRepository;
        this.bankService = bankService;
        this.paymentEventProducerService = paymentEventProducerService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.paymentStatusCacheTtlMinutes = paymentStatusCacheTtlMinutes;
    }
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        logger.info("Creating payment for customer: {} and merchant: {}", 
                    request.getCustomerId(), request.getMerchantId());
        
        // Validate request
        validatePaymentRequest(request);
        
        // Generate unique payment reference
        String paymentReference = generateUniquePaymentReference();
        
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
        Payment savedPayment;
        try {
            savedPayment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            logger.error("Duplicate payment reference detected while saving: {}", paymentReference, ex);
            throw new DuplicatePaymentException("Payment with reference " + paymentReference + " already exists");
        }
        
        logger.info("Payment created successfully with reference: {}", paymentReference);

        PaymentResponse response = mapToResponse(savedPayment);
        cachePaymentResponse(response);

        return response;
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String paymentReference) {
        logger.info("Fetching payment with reference: {}", paymentReference);

        PaymentResponse cachedResponse = getCachedPaymentResponse(paymentReference);
        if (cachedResponse != null) {
            logger.info("Payment {} found in Redis cache", paymentReference);
            return cachedResponse;
        }
        
        logger.info("Payment {} not found in Redis cache. Fetching from PostgreSQL", paymentReference);
        
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new PaymentNotFoundException(paymentReference));

        PaymentResponse response = mapToResponse(payment);
        cachePaymentResponse(response);

        return response;
    }
    
    @Override
    public PaymentResponse processPayment(String paymentReference) {
        logger.info("Processing payment with reference: {}", paymentReference);
        
        // Fetch payment
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new PaymentNotFoundException(paymentReference));
        
        // Validate payment can be processed
        validatePaymentForProcessing(payment);
        
        // Update status to VALIDATED
        updatePaymentStatus(payment, PaymentStatus.VALIDATED, "Payment validated and ready for processing");
        
        // Update status to PROCESSING
        updatePaymentStatus(payment, PaymentStatus.PROCESSING, "Payment being processed with bank");

        // Process with mock bank and update final status.
        processPaymentWithMockBank(payment);
        
        // Save and return
        Payment updatedPayment = paymentRepository.save(payment);

        publishProcessedPaymentEvent(updatedPayment);
        
        logger.info("Payment {} processing completed with status: {}", 
                   paymentReference, updatedPayment.getStatus());

        PaymentResponse response = mapToResponse(updatedPayment);
        cachePaymentResponse(response);

        return response;
    }

    private PaymentResponse getCachedPaymentResponse(String paymentReference) {
        String cacheKey = buildPaymentStatusCacheKey(paymentReference);

        try {
            String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedValue == null || cachedValue.isBlank()) {
                return null;
            }

            return objectMapper.readValue(cachedValue, PaymentResponse.class);
        } catch (JsonProcessingException ex) {
            logger.warn("Unable to deserialize cached payment response for key {}", cacheKey, ex);
            return null;
        } catch (Exception ex) {
            logger.warn("Redis unavailable while reading key {}", cacheKey, ex);
            return null;
        }
    }

    private void cachePaymentResponse(PaymentResponse paymentResponse) {
        if (paymentResponse == null || paymentResponse.getPaymentReference() == null) {
            return;
        }

        String cacheKey = buildPaymentStatusCacheKey(paymentResponse.getPaymentReference());

        try {
            String serializedResponse = objectMapper.writeValueAsString(paymentResponse);
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    serializedResponse,
                    Duration.ofMinutes(paymentStatusCacheTtlMinutes)
            );
        } catch (JsonProcessingException ex) {
            logger.warn("Unable to serialize payment response for cache key {}", cacheKey, ex);
        } catch (Exception ex) {
            logger.warn("Redis unavailable while writing key {}", cacheKey, ex);
        }
    }

    private String buildPaymentStatusCacheKey(String paymentReference) {
        return PAYMENT_STATUS_CACHE_KEY_PREFIX + paymentReference;
    }

    /**
     * Internal mock bank processing method.
     */
    private void processPaymentWithMockBank(Payment payment) {
        BankProcessingResult bankResult = bankService.processPayment(payment);

        if (bankResult.isSuccess()) {
            handleSuccessfulPayment(payment, bankResult);
            return;
        }

        handleFailedPayment(payment, bankResult);
    }

    /**
     * Publish Kafka event for terminal payment status.
     */
    private void publishProcessedPaymentEvent(Payment payment) {
        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.FAILED) {
            return;
        }

        PaymentEventDto paymentEventDto = PaymentEventDto.builder()
                .paymentReference(payment.getPaymentReference())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .timestamp(payment.getUpdatedAt() != null ? payment.getUpdatedAt() : LocalDateTime.now())
                .build();

        paymentEventProducerService.publishPaymentEvent(paymentEventDto);
    }
    
    /**
     * Validate payment can be processed
     */
    private void validatePaymentForProcessing(Payment payment) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentValidationException("Payment already processed successfully");
        }
        
        if (payment.getStatus() == PaymentStatus.PROCESSING) {
            throw new PaymentValidationException("Payment is already being processed");
        }
    }
    
    /**
     * Handle successful payment
     */
    private void handleSuccessfulPayment(Payment payment, BankProcessingResult bankResult) {
        logger.info("Payment {} approved by bank. Transaction ID: {}", 
                   payment.getPaymentReference(), bankResult.getTransactionId());
        
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setFailureReason(null);
        
        PaymentEvent successEvent = PaymentEvent.builder()
                .status(PaymentStatus.SUCCESS)
                .eventType("PAYMENT_SUCCESS")
                .message("Payment approved by bank")
                .eventData(String.format("Transaction ID: %s, Response Code: %s, Processing Time: %dms", 
                          bankResult.getTransactionId(), 
                          bankResult.getResponseCode(), 
                          bankResult.getProcessingTimeMs()))
                .source("MOCK_BANK")
                .build();
        
        payment.addEvent(successEvent);
    }
    
    /**
     * Handle failed payment
     */
    private void handleFailedPayment(Payment payment, BankProcessingResult bankResult) {
        logger.warn("Payment {} declined by bank. Reason: {} ({})", 
                   payment.getPaymentReference(), 
                   bankResult.getMessage(), 
                   bankResult.getResponseCode());
        
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(bankResult.getMessage() + " (Code: " + bankResult.getResponseCode() + ")");
        
        PaymentEvent failureEvent = PaymentEvent.builder()
                .status(PaymentStatus.FAILED)
                .eventType("PAYMENT_FAILED")
                .message("Payment declined by bank")
                .eventData(String.format("Response Code: %s, Reason: %s, Processing Time: %dms", 
                          bankResult.getResponseCode(), 
                          bankResult.getMessage(), 
                          bankResult.getProcessingTimeMs()))
                .source("MOCK_BANK")
                .build();
        
        payment.addEvent(failureEvent);
    }
    
    /**
     * Update payment status and add event
     */
    private void updatePaymentStatus(Payment payment, PaymentStatus status, String message) {
        payment.setStatus(status);
        
        PaymentEvent event = PaymentEvent.builder()
                .status(status)
                .eventType("STATUS_CHANGE")
                .message(message)
                .source("SYSTEM")
                .build();
        
        payment.addEvent(event);
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
            throw new PaymentValidationException("Amount is required");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Amount must be greater than zero");
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
            throw new PaymentValidationException("Currency is required");
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
            throw new PaymentValidationException("Customer ID is required");
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
            throw new PaymentValidationException("Merchant ID is required");
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
    private String generateUniquePaymentReference() {
        for (int attempt = 1; attempt <= MAX_REFERENCE_GENERATION_RETRIES; attempt++) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String reference = String.format("PAY-%s-%s", timestamp, uniqueId);

            if (!paymentRepository.existsByPaymentReference(reference)) {
                logger.debug("Generated payment reference: {}", reference);
                return reference;
            }

            logger.warn("Duplicate payment reference generated on attempt {}: {}", attempt, reference);
        }

        throw new DuplicatePaymentException("Unable to generate unique payment reference");
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

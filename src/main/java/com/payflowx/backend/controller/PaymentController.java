package com.payflowx.backend.controller;

import com.payflowx.backend.dto.PaymentRequest;
import com.payflowx.backend.dto.PaymentResponse;
import com.payflowx.backend.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for payment operations
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    private final PaymentService paymentService;
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * Create a new payment
     * POST /api/payments
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        logger.info("event=create_payment_request customerId={} merchantId={} amount={} currency={}",
            request.getCustomerId(), request.getMerchantId(), request.getAmount(), request.getCurrency());
        
        PaymentResponse response = paymentService.createPayment(request);
        
        logger.info("event=create_payment_success paymentReference={} status={}",
            response.getPaymentReference(), response.getStatus());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get payment by reference
     * GET /api/payments/{paymentReference}
     */
    @GetMapping("/{paymentReference}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentReference) {
        logger.info("event=get_payment_request paymentReference={}", paymentReference);
        
        PaymentResponse response = paymentService.getPaymentByReference(paymentReference);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Process payment with bank
     * POST /api/payments/{paymentReference}/process
     */
    @PostMapping("/{paymentReference}/process")
    public ResponseEntity<PaymentResponse> processPayment(@PathVariable String paymentReference) {
        logger.info("event=process_payment_request paymentReference={}", paymentReference);
        
        PaymentResponse response = paymentService.processPayment(paymentReference);
        
        logger.info("event=process_payment_success paymentReference={} finalStatus={}",
            paymentReference, response.getStatus());
        
        return ResponseEntity.ok(response);
    }
}

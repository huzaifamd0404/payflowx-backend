package com.payflowx.backend.service;

import com.payflowx.backend.dto.PaymentRequest;
import com.payflowx.backend.dto.PaymentResponse;
import com.payflowx.backend.entity.PaymentStatus;
import com.payflowx.backend.exception.PaymentNotFoundException;
import com.payflowx.backend.exception.PaymentValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {
    
    @Autowired
    private PaymentService paymentService;
    
    @Test
    void createAndProcessPayment_Success() {
        // Create payment
        PaymentRequest request = PaymentRequest.builder()
                .customerId("CUST001")
                .merchantId("MER001")
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .build();
        
        PaymentResponse createdPayment = paymentService.createPayment(request);
        
        assertThat(createdPayment).isNotNull();
        assertThat(createdPayment.getPaymentReference()).isNotNull();
        assertThat(createdPayment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        
        // Process payment
        PaymentResponse processedPayment = paymentService.processPayment(createdPayment.getPaymentReference());
        
        assertThat(processedPayment).isNotNull();
        assertThat(processedPayment.getStatus()).isIn(PaymentStatus.SUCCESS, PaymentStatus.FAILED);
        
        if (processedPayment.getStatus() == PaymentStatus.SUCCESS) {
            assertThat(processedPayment.getFailureReason()).isNull();
        } else {
            assertThat(processedPayment.getFailureReason()).isNotNull();
        }
    }
    
    @Test
    void processPayment_PaymentNotFound_ThrowsException() {
        assertThatThrownBy(() -> paymentService.processPayment("NON_EXISTENT"))
                .isInstanceOf(PaymentNotFoundException.class);
    }
    
    @Test
    void processPayment_AlreadyProcessed_ThrowsException() {
        // Create and process payment
        PaymentRequest request = PaymentRequest.builder()
                .customerId("CUST002")
                .merchantId("MER002")
                .amount(new BigDecimal("500.00"))
                .currency("EUR")
                .build();
        
        PaymentResponse createdPayment = paymentService.createPayment(request);
        PaymentResponse processedPayment = paymentService.processPayment(createdPayment.getPaymentReference());
        
        // Try to process again if it succeeded
        if (processedPayment.getStatus() == PaymentStatus.SUCCESS) {
            assertThatThrownBy(() -> 
                paymentService.processPayment(processedPayment.getPaymentReference()))
                    .isInstanceOf(PaymentValidationException.class)
                    .hasMessageContaining("already processed successfully");
        }
    }
    
    @Test
    void createPayment_InvalidAmount_ThrowsException() {
        PaymentRequest request = PaymentRequest.builder()
                .customerId("CUST003")
                .merchantId("MER003")
                .amount(new BigDecimal("-100.00"))
                .currency("USD")
                .build();
        
        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(PaymentValidationException.class);
    }
    
    @Test
    void createPayment_InvalidCurrency_ThrowsException() {
        PaymentRequest request = PaymentRequest.builder()
                .customerId("CUST004")
                .merchantId("MER004")
                .amount(new BigDecimal("100.00"))
                .currency("XXX")
                .build();
        
        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("Unsupported currency");
    }
    
    @Test
    void getPaymentByReference_ExistingPayment_ReturnsPayment() {
        // Create payment
        PaymentRequest request = PaymentRequest.builder()
                .customerId("CUST005")
                .merchantId("MER005")
                .amount(new BigDecimal("750.50"))
                .currency("GBP")
                .build();
        
        PaymentResponse createdPayment = paymentService.createPayment(request);
        
        // Retrieve payment
        PaymentResponse retrievedPayment = paymentService.getPaymentByReference(
                createdPayment.getPaymentReference());
        
        assertThat(retrievedPayment).isNotNull();
        assertThat(retrievedPayment.getPaymentReference())
                .isEqualTo(createdPayment.getPaymentReference());
        assertThat(retrievedPayment.getAmount()).isEqualTo(new BigDecimal("750.50"));
    }
}

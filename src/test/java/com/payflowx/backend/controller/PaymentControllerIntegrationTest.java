package com.payflowx.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflowx.backend.dto.PaymentRequest;
import com.payflowx.backend.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void createPayment_ValidRequest_ReturnsCreated() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .customerId("CUST001")
                .merchantId("MER001")
                .amount(new BigDecimal("2500.00"))
                .currency("USD")
                .build();
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentReference", notNullValue()))
                .andExpect(jsonPath("$.amount", is(2500.00)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.customerId", is("CUST001")))
                .andExpect(jsonPath("$.merchantId", is("MER001")))
                .andExpect(jsonPath("$.status", is(PaymentStatus.INITIATED.toString())))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }
    
    @Test
    void createPayment_InvalidAmount_ReturnsBadRequest() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .customerId("CUST001")
                .merchantId("MER001")
                .amount(new BigDecimal("-100.00"))
                .currency("USD")
                .build();
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }
    
    @Test
    void createPayment_MissingCustomerId_ReturnsBadRequest() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .merchantId("MER001")
                .amount(new BigDecimal("2500.00"))
                .currency("USD")
                .build();
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("customerId")));
    }
    
    @Test
    void createPayment_InvalidCurrency_ReturnsBadRequest() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .customerId("CUST001")
                .merchantId("MER001")
                .amount(new BigDecimal("2500.00"))
                .currency("INVALID")
                .build();
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void createPayment_InvalidCustomerIdFormat_ReturnsBadRequest() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .customerId("cust-001")
                .merchantId("MER001")
                .amount(new BigDecimal("2500.00"))
                .currency("USD")
                .build();
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("customerId")));
    }
    
    @Test
    void createPayment_AmountTooLarge_ReturnsBadRequest() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .customerId("CUST001")
                .merchantId("MER001")
                .amount(new BigDecimal("9999999999.99"))
                .currency("USD")
                .build();
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}

package com.payflowx.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a payment transaction in the system.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_reference", columnList = "paymentReference"),
    @Index(name = "idx_customer_id", columnList = "customerId"),
    @Index(name = "idx_merchant_id", columnList = "merchantId"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String paymentReference;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(nullable = false, length = 100)
    private String customerId;
    
    @Column(nullable = false, length = 100)
    private String merchantId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;
    
    @Column(length = 500)
    private String failureReason;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PaymentEvent> events = new ArrayList<>();
    
    /**
     * Helper method to add a payment event
     */
    public void addEvent(PaymentEvent event) {
        events.add(event);
        event.setPayment(this);
    }
    
    /**
     * Helper method to remove a payment event
     */
    public void removeEvent(PaymentEvent event) {
        events.remove(event);
        event.setPayment(null);
    }
}

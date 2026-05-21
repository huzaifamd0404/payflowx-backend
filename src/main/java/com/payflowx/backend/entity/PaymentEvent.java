package com.payflowx.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an event in the lifecycle of a payment transaction.
 * This table tracks all state changes and significant events for each payment.
 */
@Entity
@Table(name = "payment_events", indexes = {
    @Index(name = "idx_payment_event_payment_id", columnList = "payment_id"),
    @Index(name = "idx_payment_event_status", columnList = "status"),
    @Index(name = "idx_payment_event_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;
    
    @Column(nullable = false, length = 100)
    private String eventType;
    
    @Column(columnDefinition = "TEXT")
    private String eventData;
    
    @Column(length = 500)
    private String message;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    @Column(length = 100)
    private String source;
    
    /**
     * Constructor for creating events without payment reference (will be set by parent)
     */
    public PaymentEvent(PaymentStatus status, String eventType, String message) {
        this.status = status;
        this.eventType = eventType;
        this.message = message;
    }
}

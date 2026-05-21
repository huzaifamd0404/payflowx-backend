package com.payflowx.backend.repository;

import com.payflowx.backend.entity.Payment;
import com.payflowx.backend.entity.PaymentEvent;
import com.payflowx.backend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for PaymentEvent entity operations.
 */
@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    
    /**
     * Find all events for a specific payment
     */
    List<PaymentEvent> findByPaymentOrderByTimestampAsc(Payment payment);
    
    /**
     * Find all events for a specific payment by payment ID
     */
    List<PaymentEvent> findByPaymentIdOrderByTimestampAsc(Long paymentId);
    
    /**
     * Find all events with a specific status
     */
    List<PaymentEvent> findByStatus(PaymentStatus status);
    
    /**
     * Find all events of a specific type
     */
    List<PaymentEvent> findByEventType(String eventType);
    
    /**
     * Find events within a date range
     */
    List<PaymentEvent> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find the latest event for a payment
     */
    @Query("SELECT e FROM PaymentEvent e WHERE e.payment.id = :paymentId ORDER BY e.timestamp DESC LIMIT 1")
    PaymentEvent findLatestEventByPaymentId(@Param("paymentId") Long paymentId);
    
    /**
     * Count events for a specific payment
     */
    Long countByPaymentId(Long paymentId);
    
    /**
     * Find events by payment reference
     */
    @Query("SELECT e FROM PaymentEvent e WHERE e.payment.paymentReference = :paymentReference ORDER BY e.timestamp ASC")
    List<PaymentEvent> findByPaymentReference(@Param("paymentReference") String paymentReference);
}

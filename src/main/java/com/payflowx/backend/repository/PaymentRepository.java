package com.payflowx.backend.repository;

import com.payflowx.backend.entity.Payment;
import com.payflowx.backend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * Find a payment by its unique payment reference
     */
    Optional<Payment> findByPaymentReference(String paymentReference);
    
    /**
     * Find all payments for a specific customer
     */
    List<Payment> findByCustomerId(String customerId);
    
    /**
     * Find all payments for a specific merchant
     */
    List<Payment> findByMerchantId(String merchantId);
    
    /**
     * Find all payments with a specific status
     */
    List<Payment> findByStatus(PaymentStatus status);
    
    /**
     * Find all payments for a customer with a specific status
     */
    List<Payment> findByCustomerIdAndStatus(String customerId, PaymentStatus status);
    
    /**
     * Find all payments for a merchant with a specific status
     */
    List<Payment> findByMerchantIdAndStatus(String merchantId, PaymentStatus status);
    
    /**
     * Find all payments created within a date range
     */
    List<Payment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find payments that are stuck in PROCESSING or RETRYING status for too long
     */
    @Query("SELECT p FROM Payment p WHERE p.status IN :statuses AND p.updatedAt < :threshold")
    List<Payment> findStalePayments(
        @Param("statuses") List<PaymentStatus> statuses,
        @Param("threshold") LocalDateTime threshold
    );
    
    /**
     * Count payments by status
     */
    Long countByStatus(PaymentStatus status);
    
    /**
     * Check if a payment reference already exists
     */
    boolean existsByPaymentReference(String paymentReference);
}

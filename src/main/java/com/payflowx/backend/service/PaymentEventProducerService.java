package com.payflowx.backend.service;

import com.payflowx.backend.dto.PaymentEventDto;

/**
 * Producer service for publishing payment events to Kafka.
 */
public interface PaymentEventProducerService {

    /**
     * Publish processed payment event to Kafka.
     * @param eventDto Payment event payload
     */
    void publishPaymentEvent(PaymentEventDto eventDto);
}

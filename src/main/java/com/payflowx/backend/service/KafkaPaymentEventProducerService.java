package com.payflowx.backend.service;

import com.payflowx.backend.dto.PaymentEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka implementation of payment event producer.
 */
@Service
public class KafkaPaymentEventProducerService implements PaymentEventProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaPaymentEventProducerService.class);

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String paymentEventsTopic;

    public KafkaPaymentEventProducerService(
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${app.kafka.topic.payment-events}") String paymentEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentEventsTopic = paymentEventsTopic;
    }

    @Override
    public void publishPaymentEvent(PaymentEventDto eventDto) {
        kafkaTemplate.send(paymentEventsTopic, eventDto.getPaymentReference(), eventDto)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to publish payment event for reference: {}",
                                eventDto.getPaymentReference(), ex);
                        return;
                    }

                    logger.info("Published payment event to Kafka topic {} for reference: {}",
                            paymentEventsTopic, eventDto.getPaymentReference());
                });
    }
}

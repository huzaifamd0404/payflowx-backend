# Project and Log Snapshots

This file stores runtime snapshots captured during local docker-compose execution.

## 1) Service Status Snapshot

```text
NAME                 IMAGE                             COMMAND                  SERVICE     CREATED          STATUS                    PORTS
payflowx-app         payflowx-backend-app              "java -jar /app/app.…"   app         16 minutes ago   Up 15 minutes             0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp
payflowx-kafka       confluentinc/cp-kafka:7.6.1       "/etc/confluent/dock…"   kafka       16 minutes ago   Up 15 minutes (healthy)   0.0.0.0:9092->9092/tcp, [::]:9092->9092/tcp, 0.0.0.0:29092->29092/tcp, [::]:29092->29092/tcp
payflowx-postgres    postgres:16-alpine                "docker-entrypoint.s…"   postgres    16 minutes ago   Up 16 minutes (healthy)   0.0.0.0:5432->5432/tcp, [::]:5432->5432/tcp
payflowx-redis       redis:7.2-alpine                  "docker-entrypoint.s…"   redis       16 minutes ago   Up 16 minutes (healthy)   0.0.0.0:6379->6379/tcp, [::]:6379->6379/tcp
payflowx-zookeeper   confluentinc/cp-zookeeper:7.6.1   "/etc/confluent/dock…"   zookeeper   16 minutes ago   Up 16 minutes             0.0.0.0:2181->2181/tcp, [::]:2181->2181/tcp
```

## 2) Application Log Snapshot

```text
{"timestamp":"2026-06-18T16:30:32.561Z","level":"INFO","thread":"http-nio-8080-exec-3","logger":"c.p.b.service.PaymentServiceImpl","correlationId":"43565a27-bf4a-42dc-9da8-1711ac599dbc","message":"Payment PAY-20260618-163023-7E2500B0 processing completed with status: SUCCESS","exception":""}
{"timestamp":"2026-06-18T16:30:32.594Z","level":"INFO","thread":"http-nio-8080-exec-3","logger":"c.p.b.controller.PaymentController","correlationId":"43565a27-bf4a-42dc-9da8-1711ac599dbc","message":"event=process_payment_success paymentReference=PAY-20260618-163023-7E2500B0 finalStatus=SUCCESS","exception":""}
{"timestamp":"2026-06-18T16:30:32.693Z","level":"INFO","thread":"kafka-producer-network-thread | producer-1","logger":"c.p.b.s.KafkaPaymentEventProducerService","correlationId":"N/A","message":"Published payment event to Kafka topic payment-events for reference: PAY-20260618-163023-7E2500B0","exception":""}
{"timestamp":"2026-06-18T16:30:32.714Z","level":"INFO","thread":"org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1","logger":"c.p.b.s.KafkaPaymentEventConsumerService","correlationId":"N/A","message":"Consumed payment event saved successfully. eventId=5, paymentReference=PAY-20260618-163023-7E2500B0, status=SUCCESS","exception":""}
```

## 3) Kafka Broker Log Snapshot

```text
[2026-06-18 16:14:50,576] INFO [GroupCoordinator 1]: Stabilized group payflowx-payment-events-consumer generation 1 (__consumer_offsets-4) with 6 members (kafka.coordinator.group.GroupCoordinator)
[2026-06-18 16:14:50,610] INFO [GroupCoordinator 1]: Assignment received from leader consumer-payflowx-payment-events-consumer-3-125965fe-5982-413d-97fd-dc9783f46830 for group payflowx-payment-events-consumer for generation 1. The group has 6 members, 0 of which are static. (kafka.coordinator.group.GroupCoordinator)
[2026-06-18 16:30:32,601] INFO [Controller id=1] Acquired new producerId block ProducerIdsBlock(assignedBrokerId=1, firstProducerId=0, size=1000) by writing to Zk with path version 1 (kafka.controller.KafkaController)
```

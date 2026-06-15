# payflowx-backend

## High Request Volume Preparation

The app is configured for higher concurrent request throughput using:

- Tomcat request threads and connection limits
- Spring async executor pool
- Kafka listener concurrency
- HikariCP connection pool scaling

All related settings are in [src/main/resources/application.properties](src/main/resources/application.properties).

## Load Test with Postman Collection Runner

Use these files:

- [postman/PayFlowX-High-Volume.postman_collection.json](postman/PayFlowX-High-Volume.postman_collection.json)
- [postman/PayFlowX-Local.postman_environment.json](postman/PayFlowX-Local.postman_environment.json)
- [postman/payment-load-data.csv](postman/payment-load-data.csv)

### Runner setup

1. Import collection and environment in Postman.
2. Select environment: PayFlowX Local.
3. Open Collection Runner.
4. Choose data file: payment-load-data.csv.
5. Set iterations based on target volume (for example: 200, 500, 1000).
6. Run with the full collection order:
	- Health Check
	- Create Payment
	- Get Payment

The Create Payment test script also calls process endpoint for each created payment.

## Post-run Verification (DB, Kafka, Redis)

Run verification script:

```powershell
./scripts/verify-after-load.ps1 -ExpectedPayments 200
```

Optional parameters:

- -PostgresHost, -PostgresPort, -PostgresDb, -PostgresUser
- -KafkaBootstrap, -KafkaTopic
- -RedisHost, -RedisPort
- -PsqlPath, -RedisCliPath, -KafkaConsoleConsumerPath

The script checks:

- payments and payment_events row counts in PostgreSQL
- Kafka topic sample consumption from payment-events
- Redis keys matching payment:status:*
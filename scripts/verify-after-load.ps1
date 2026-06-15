param(
    [string]$PostgresHost = "localhost",
    [int]$PostgresPort = 5432,
    [string]$PostgresDb = "payflowx_db",
    [string]$PostgresUser = "postgres",
    [string]$KafkaBootstrap = "localhost:9092",
    [string]$KafkaTopic = "payment-events",
    [string]$RedisHost = "localhost",
    [int]$RedisPort = 6379,
    [int]$ExpectedPayments = 100,
    [string]$PsqlPath = "psql",
    [string]$RedisCliPath = "redis-cli",
    [string]$KafkaConsoleConsumerPath = "kafka-console-consumer"
)

$ErrorActionPreference = "Stop"

Write-Host "=== PayFlowX Post-load Verification ==="

# 1) Verify payment records and event linkage in PostgreSQL
try {
    Write-Host "[DB] Checking payments and payment_events in PostgreSQL..."

    $query = @"
SELECT
  (SELECT COUNT(*) FROM payments) AS payment_count,
  (SELECT COUNT(*) FROM payment_events) AS event_count,
  (SELECT COUNT(*) FROM payments WHERE status IN ('SUCCESS','FAILED')) AS terminal_payment_count,
  (SELECT COUNT(*)
     FROM payment_events pe
     JOIN payments p ON pe.payment_id = p.id
    WHERE pe.event_type = 'KAFKA_PAYMENT_EVENT') AS kafka_event_rows;
"@

    $dbOutput = & $PsqlPath -h $PostgresHost -p $PostgresPort -U $PostgresUser -d $PostgresDb -t -A -F "," -c $query
    if ($LASTEXITCODE -ne 0) {
        throw "psql command failed"
    }

    $line = ($dbOutput | Select-Object -First 1).Trim()
    $cols = $line.Split(",")

    $paymentCount = [int]$cols[0]
    $eventCount = [int]$cols[1]
    $terminalPaymentCount = [int]$cols[2]
    $kafkaEventRows = [int]$cols[3]

    Write-Host "[DB] payments=$paymentCount, payment_events=$eventCount, terminal_payments=$terminalPaymentCount, kafka_event_rows=$kafkaEventRows"

    if ($paymentCount -lt $ExpectedPayments) {
        Write-Warning "[DB] Fewer payments than expected. expected=$ExpectedPayments actual=$paymentCount"
    }

    if ($kafkaEventRows -lt $terminalPaymentCount) {
        Write-Warning "[DB] Kafka consumer rows are below terminal payment count. expected-at-least=$terminalPaymentCount actual=$kafkaEventRows"
    }
}
catch {
    Write-Warning "[DB] Verification failed: $($_.Exception.Message)"
}

# 2) Verify Kafka topic has produced messages
try {
    Write-Host "[Kafka] Sampling recent messages from topic $KafkaTopic ..."

    $kafkaOutput = & $KafkaConsoleConsumerPath --bootstrap-server $KafkaBootstrap --topic $KafkaTopic --from-beginning --max-messages 5 --timeout-ms 10000 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw "Kafka consumer command failed"
    }

    if (-not $kafkaOutput -or $kafkaOutput.Count -eq 0) {
        Write-Warning "[Kafka] No messages sampled from topic $KafkaTopic"
    }
    else {
        Write-Host "[Kafka] Sampled messages:"
        $kafkaOutput | Select-Object -First 5 | ForEach-Object { Write-Host $_ }
    }
}
catch {
    Write-Warning "[Kafka] Verification failed: $($_.Exception.Message)"
}

# 3) Verify Redis cache keys for payment status
try {
    Write-Host "[Redis] Counting cache keys payment:status:* ..."

    $redisOutput = & $RedisCliPath -h $RedisHost -p $RedisPort --scan --pattern "payment:status:*"
    if ($LASTEXITCODE -ne 0) {
        throw "redis-cli command failed"
    }

    $cacheKeyCount = ($redisOutput | Measure-Object).Count
    Write-Host "[Redis] cache_keys=$cacheKeyCount"

    if ($cacheKeyCount -eq 0) {
        Write-Warning "[Redis] No payment cache keys found. Ensure GET /api/payments/{reference} requests were executed."
    }
}
catch {
    Write-Warning "[Redis] Verification failed: $($_.Exception.Message)"
}

Write-Host "=== Verification complete ==="

# Sample API Requests and Responses

These examples are aligned with the local compose setup where base URL is http://localhost:8080.

## 1) Health Check

Request:

```bash
curl -X GET "http://localhost:8080/api/health"
```

Sample response:

```json
{
  "status": "UP",
  "message": "PayFlowX Backend is running successfully",
  "timestamp": "2026-06-18T16:30:18.792181053"
}
```

## 2) Create Payment

Request:

```bash
curl -X POST "http://localhost:8080/api/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST1001",
    "merchantId": "MERCH5001",
    "amount": 249.99,
    "currency": "USD"
  }'
```

Sample response:

```json
{
  "id": 1,
  "paymentReference": "PAY-20260618-163023-7E2500B0",
  "amount": 249.99,
  "currency": "USD",
  "customerId": "CUST1001",
  "merchantId": "MERCH5001",
  "status": "INITIATED",
  "failureReason": null,
  "createdAt": "2026-06-18T16:30:23.333986",
  "updatedAt": "2026-06-18T16:30:23.334043"
}
```

## 3) Process Payment

Request:

```bash
curl -X POST "http://localhost:8080/api/payments/PAY-20260618-163023-7E2500B0/process"
```

Sample response:

```json
{
  "id": 1,
  "paymentReference": "PAY-20260618-163023-7E2500B0",
  "amount": 249.99,
  "currency": "USD",
  "customerId": "CUST1001",
  "merchantId": "MERCH5001",
  "status": "SUCCESS",
  "failureReason": null,
  "createdAt": "2026-06-18T16:30:23.333986",
  "updatedAt": "2026-06-18T16:30:23.334043"
}
```

## 4) Get Payment by Reference

Request:

```bash
curl -X GET "http://localhost:8080/api/payments/PAY-20260618-163023-7E2500B0"
```

Sample response:

```json
{
  "id": 1,
  "paymentReference": "PAY-20260618-163023-7E2500B0",
  "amount": 249.99,
  "currency": "USD",
  "customerId": "CUST1001",
  "merchantId": "MERCH5001",
  "status": "SUCCESS",
  "failureReason": null,
  "createdAt": "2026-06-18T16:30:23.333986",
  "updatedAt": "2026-06-18T16:30:23.334043"
}
```

## 5) Validation Failure Example

Request:

```bash
curl -X POST "http://localhost:8080/api/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-lowercase",
    "merchantId": "M1",
    "amount": -10,
    "currency": "USDX"
  }'
```

Expected behavior:

- HTTP 400 Bad Request
- Validation error payload from global exception handler

## 6) Not Found Example

Request:

```bash
curl -X GET "http://localhost:8080/api/payments/PAY-DOES-NOT-EXIST"
```

Expected behavior:

- HTTP 404 Not Found
- Error response with message indicating missing payment reference

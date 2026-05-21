-- ============================================
-- PayFlowX Database Schema
-- PostgreSQL Database Initialization Script
-- ============================================

-- Drop tables if they exist (for clean setup)
DROP TABLE IF EXISTS payment_events CASCADE;
DROP TABLE IF EXISTS payments CASCADE;

-- ============================================
-- Table: payments
-- Description: Stores payment transaction data
-- ============================================
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    payment_reference VARCHAR(100) NOT NULL UNIQUE,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    merchant_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_status_valid CHECK (
        status IN ('INITIATED', 'VALIDATED', 'PROCESSING', 'SUCCESS', 'FAILED', 'RETRYING')
    )
);

-- ============================================
-- Table: payment_events
-- Description: Stores event history for payments
-- ============================================
CREATE TABLE payment_events (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data TEXT,
    message VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(100),
    
    -- Foreign Key Constraint
    CONSTRAINT fk_payment_event_payment 
        FOREIGN KEY (payment_id) 
        REFERENCES payments(id) 
        ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_event_status_valid CHECK (
        status IN ('INITIATED', 'VALIDATED', 'PROCESSING', 'SUCCESS', 'FAILED', 'RETRYING')
    )
);

-- ============================================
-- Indexes for performance optimization
-- ============================================

-- Payments table indexes
CREATE INDEX idx_payment_reference ON payments(payment_reference);
CREATE INDEX idx_customer_id ON payments(customer_id);
CREATE INDEX idx_merchant_id ON payments(merchant_id);
CREATE INDEX idx_status ON payments(status);
CREATE INDEX idx_created_at ON payments(created_at);
CREATE INDEX idx_updated_at ON payments(updated_at);

-- Payment Events table indexes
CREATE INDEX idx_payment_event_payment_id ON payment_events(payment_id);
CREATE INDEX idx_payment_event_status ON payment_events(status);
CREATE INDEX idx_payment_event_timestamp ON payment_events(timestamp);
CREATE INDEX idx_payment_event_type ON payment_events(event_type);

-- ============================================
-- Comments for documentation
-- ============================================

COMMENT ON TABLE payments IS 'Stores all payment transaction records';
COMMENT ON COLUMN payments.payment_reference IS 'Unique identifier for the payment transaction';
COMMENT ON COLUMN payments.amount IS 'Payment amount with 2 decimal precision';
COMMENT ON COLUMN payments.currency IS 'ISO 4217 currency code (e.g., USD, EUR, GBP)';
COMMENT ON COLUMN payments.status IS 'Current status of the payment: INITIATED, VALIDATED, PROCESSING, SUCCESS, FAILED, RETRYING';
COMMENT ON COLUMN payments.failure_reason IS 'Reason for payment failure if status is FAILED';

COMMENT ON TABLE payment_events IS 'Stores audit trail and event history for each payment';
COMMENT ON COLUMN payment_events.payment_id IS 'Reference to the parent payment record';
COMMENT ON COLUMN payment_events.status IS 'Status at the time of the event';
COMMENT ON COLUMN payment_events.event_type IS 'Type of event (e.g., STATUS_CHANGE, VALIDATION_FAILED)';
COMMENT ON COLUMN payment_events.event_data IS 'JSON or text data associated with the event';

-- ============================================
-- Sample Data (Optional - for testing)
-- ============================================

-- Uncomment below to insert sample data

/*
-- Sample Payment 1
INSERT INTO payments (payment_reference, amount, currency, customer_id, merchant_id, status, created_at, updated_at)
VALUES ('PAY-001-2026-05-21', 100.00, 'USD', 'CUST-001', 'MERCHANT-001', 'SUCCESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample Payment 2
INSERT INTO payments (payment_reference, amount, currency, customer_id, merchant_id, status, failure_reason, created_at, updated_at)
VALUES ('PAY-002-2026-05-21', 250.50, 'EUR', 'CUST-002', 'MERCHANT-001', 'FAILED', 'Insufficient funds', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample Events for Payment 1
INSERT INTO payment_events (payment_id, status, event_type, message, timestamp)
VALUES 
    (1, 'INITIATED', 'PAYMENT_INITIATED', 'Payment initiated by customer', CURRENT_TIMESTAMP),
    (1, 'VALIDATED', 'PAYMENT_VALIDATED', 'Payment details validated successfully', CURRENT_TIMESTAMP),
    (1, 'PROCESSING', 'PAYMENT_PROCESSING', 'Payment being processed', CURRENT_TIMESTAMP),
    (1, 'SUCCESS', 'PAYMENT_SUCCESS', 'Payment completed successfully', CURRENT_TIMESTAMP);

-- Sample Events for Payment 2
INSERT INTO payment_events (payment_id, status, event_type, message, timestamp)
VALUES 
    (2, 'INITIATED', 'PAYMENT_INITIATED', 'Payment initiated by customer', CURRENT_TIMESTAMP),
    (2, 'VALIDATED', 'PAYMENT_VALIDATED', 'Payment details validated successfully', CURRENT_TIMESTAMP),
    (2, 'PROCESSING', 'PAYMENT_PROCESSING', 'Payment being processed', CURRENT_TIMESTAMP),
    (2, 'FAILED', 'PAYMENT_FAILED', 'Payment failed due to insufficient funds', CURRENT_TIMESTAMP);
*/

-- ============================================
-- Verification Queries
-- ============================================

-- Count records
-- SELECT COUNT(*) FROM payments;
-- SELECT COUNT(*) FROM payment_events;

-- Verify structure
-- SELECT table_name, column_name, data_type FROM information_schema.columns WHERE table_name IN ('payments', 'payment_events');

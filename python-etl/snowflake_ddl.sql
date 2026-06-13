-- ============================================================
-- Money Transfer - Snowflake MIRROR schema (Idempotent DDL)
--
-- Why mirrored?
-- - Operational simplicity: 1:1 mapping with MySQL source tables
-- - Fewer ETL transformations / fewer failure modes
-- - MERGE provides idempotent upserts to keep Snowflake in sync
-- ============================================================

CREATE WAREHOUSE IF NOT EXISTS COMPUTE_WH
  WAREHOUSE_SIZE = 'XSMALL'
  AUTO_SUSPEND = 60
  AUTO_RESUME = TRUE
  INITIALLY_SUSPENDED = TRUE;

CREATE DATABASE IF NOT EXISTS MONEY_TRANSFER_DW;
CREATE SCHEMA IF NOT EXISTS MONEY_TRANSFER_DW.ANALYTICS;

USE WAREHOUSE COMPUTE_WH;
USE DATABASE MONEY_TRANSFER_DW;
USE SCHEMA ANALYTICS;

-- =========================
-- Mirror tables (source-of-truth = MySQL)
-- =========================

CREATE TABLE IF NOT EXISTS USERS (
  id NUMBER(38,0),
  username VARCHAR,
  password VARCHAR,
  role VARCHAR,
  enabled BOOLEAN
);

CREATE TABLE IF NOT EXISTS ACCOUNTS (
  account_id VARCHAR,
  holder_name VARCHAR,
  balance NUMBER(19,4),
  status VARCHAR,
  version NUMBER(38,0),
  last_updated TIMESTAMP_NTZ,
  user_id NUMBER(38,0)
);

CREATE TABLE IF NOT EXISTS TRANSACTION_LOGS (
  transaction_id VARCHAR,
  idempotency_key VARCHAR,
  from_account_id VARCHAR,
  to_account_id VARCHAR,
  amount NUMBER(19,4),
  status VARCHAR,
  failure_reason VARCHAR,
  created_on TIMESTAMP_NTZ
);

CREATE TABLE IF NOT EXISTS REWARD_GRANTS (
  reward_id VARCHAR,
  user_id NUMBER(38,0),
  transaction_id VARCHAR,
  points NUMBER(38,0),
  transaction_amount NUMBER(19,4),
  created_on TIMESTAMP_NTZ
);

CREATE TABLE IF NOT EXISTS REWARD_REDEMPTIONS (
  redemption_id VARCHAR,
  user_id NUMBER(38,0),
  transaction_id VARCHAR,
  points_used NUMBER(38,0),
  rupee_value NUMBER(19,4),
  created_on TIMESTAMP_NTZ
);


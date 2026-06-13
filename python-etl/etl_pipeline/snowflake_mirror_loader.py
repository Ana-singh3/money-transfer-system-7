from __future__ import annotations

"""
Snowflake loaders for the MySQL-mirrored schema.

Idempotency:
- We use MERGE for USERS/ACCOUNTS/TRANSACTION_LOGS based on their natural primary keys.

Inserted vs Updated counts:
- Snowflake MERGE doesn't reliably return a per-action rowcount breakdown.
- We compute counts by pre-checking which keys already exist in the target table for this batch.
"""

from dataclasses import dataclass
import logging
from typing import Iterable

from .snowflake_client import SnowflakeClient
from .snowflake_bootstrap import ensure_warehouse_db_schema

log = logging.getLogger(__name__)


@dataclass(frozen=True)
class MergeCounts:
    inserted: int
    updated: int


def ensure_mirror_tables(sf: SnowflakeClient, warehouse: str, database: str, schema: str) -> None:
    ensure_warehouse_db_schema(sf, warehouse, database, schema)

    # Replace star schema with mirror schema objects (idempotent).
    sf.execute(
        """
        CREATE TABLE IF NOT EXISTS USERS (
          id NUMBER(38,0),
          username VARCHAR,
          password VARCHAR,
          role VARCHAR,
          enabled BOOLEAN
        )
        """
    )
    sf.execute(
        """
        CREATE TABLE IF NOT EXISTS ACCOUNTS (
          account_id VARCHAR,
          holder_name VARCHAR,
          balance NUMBER(19,4),
          status VARCHAR,
          version NUMBER(38,0),
          last_updated TIMESTAMP_NTZ,
          user_id NUMBER(38,0)
        )
        """
    )
    sf.execute(
        """
        CREATE TABLE IF NOT EXISTS TRANSACTION_LOGS (
          transaction_id VARCHAR,
          idempotency_key VARCHAR,
          from_account_id VARCHAR,
          to_account_id VARCHAR,
          amount NUMBER(19,4),
          status VARCHAR,
          failure_reason VARCHAR,
          created_on TIMESTAMP_NTZ
        )
        """
    )
    sf.execute(
        """
        CREATE TABLE IF NOT EXISTS REWARD_GRANTS (
          reward_id VARCHAR,
          user_id NUMBER(38,0),
          transaction_id VARCHAR,
          points NUMBER(38,0),
          transaction_amount NUMBER(19,4),
          created_on TIMESTAMP_NTZ
        )
        """
    )
    sf.execute(
        """
        CREATE TABLE IF NOT EXISTS REWARD_REDEMPTIONS (
          redemption_id VARCHAR,
          user_id NUMBER(38,0),
          transaction_id VARCHAR,
          points_used NUMBER(38,0),
          rupee_value NUMBER(19,4),
          created_on TIMESTAMP_NTZ
        )
        """
    )


def merge_users(sf: SnowflakeClient, rows: list[tuple]) -> MergeCounts:
    """
    rows tuples:
      (id, username, password, role, enabled)
    """
    ids = [r[0] for r in rows]
    existing = _fetch_existing_number_keys(sf, "USERS", "id", ids)
    inserted = sum(1 for i in ids if i not in existing)
    updated = len(ids) - inserted

    sql = """
        MERGE INTO USERS t
        USING (
          SELECT %s AS id, %s AS username, %s AS password, %s AS role, %s AS enabled
        ) s
        ON t.id = s.id
        WHEN MATCHED THEN UPDATE SET
          username = s.username,
          password = s.password,
          role = s.role,
          enabled = s.enabled
        WHEN NOT MATCHED THEN INSERT (id, username, password, role, enabled)
          VALUES (s.id, s.username, s.password, s.role, s.enabled)
    """
    sf.executemany(sql, rows)
    return MergeCounts(inserted=inserted, updated=updated)


def merge_accounts(sf: SnowflakeClient, rows: list[tuple]) -> MergeCounts:
    """
    rows tuples:
      (account_id, holder_name, balance, status, version, last_updated, user_id)
    """
    keys = [r[0] for r in rows]
    existing = _fetch_existing_string_keys(sf, "ACCOUNTS", "account_id", keys)
    inserted = sum(1 for k in keys if k not in existing)
    updated = len(keys) - inserted

    sql = """
        MERGE INTO ACCOUNTS t
        USING (
          SELECT %s AS account_id, %s AS holder_name, %s AS balance, %s AS status,
                 %s AS version, %s AS last_updated, %s AS user_id
        ) s
        ON t.account_id = s.account_id
        WHEN MATCHED THEN UPDATE SET
          holder_name = s.holder_name,
          balance = s.balance,
          status = s.status,
          version = s.version,
          last_updated = s.last_updated,
          user_id = s.user_id
        WHEN NOT MATCHED THEN INSERT (account_id, holder_name, balance, status, version, last_updated, user_id)
          VALUES (s.account_id, s.holder_name, s.balance, s.status, s.version, s.last_updated, s.user_id)
    """
    sf.executemany(sql, rows)
    return MergeCounts(inserted=inserted, updated=updated)


def merge_transaction_logs(sf: SnowflakeClient, rows: list[tuple]) -> MergeCounts:
    """
    rows tuples:
      (transaction_id, idempotency_key, from_account_id, to_account_id, amount, status, failure_reason, created_on)
    """
    keys = [r[0] for r in rows]
    existing = _fetch_existing_string_keys(sf, "TRANSACTION_LOGS", "transaction_id", keys)
    inserted = sum(1 for k in keys if k not in existing)
    updated = len(keys) - inserted

    sql = """
        MERGE INTO TRANSACTION_LOGS t
        USING (
          SELECT %s AS transaction_id, %s AS idempotency_key, %s AS from_account_id, %s AS to_account_id,
                 %s AS amount, %s AS status, %s AS failure_reason, %s AS created_on
        ) s
        ON t.transaction_id = s.transaction_id
        WHEN MATCHED THEN UPDATE SET
          idempotency_key = s.idempotency_key,
          from_account_id = s.from_account_id,
          to_account_id = s.to_account_id,
          amount = s.amount,
          status = s.status,
          failure_reason = s.failure_reason,
          created_on = s.created_on
        WHEN NOT MATCHED THEN INSERT (
          transaction_id, idempotency_key, from_account_id, to_account_id, amount, status, failure_reason, created_on
        ) VALUES (
          s.transaction_id, s.idempotency_key, s.from_account_id, s.to_account_id, s.amount, s.status, s.failure_reason, s.created_on
        )
    """
    sf.executemany(sql, rows)
    return MergeCounts(inserted=inserted, updated=updated)


def merge_reward_grants(sf: SnowflakeClient, rows: list[tuple]) -> MergeCounts:
    """
    rows tuples:
      (reward_id, user_id, transaction_id, points, transaction_amount, created_on)
    """
    keys = [r[0] for r in rows]
    existing = _fetch_existing_string_keys(sf, "REWARD_GRANTS", "reward_id", keys)
    inserted = sum(1 for k in keys if k not in existing)
    updated = len(keys) - inserted

    sql = """
        MERGE INTO REWARD_GRANTS t
        USING (
          SELECT %s AS reward_id, %s AS user_id, %s AS transaction_id,
                 %s AS points, %s AS transaction_amount, %s AS created_on
        ) s
        ON t.reward_id = s.reward_id
        WHEN MATCHED THEN UPDATE SET
          user_id = s.user_id,
          transaction_id = s.transaction_id,
          points = s.points,
          transaction_amount = s.transaction_amount,
          created_on = s.created_on
        WHEN NOT MATCHED THEN INSERT (
          reward_id, user_id, transaction_id, points, transaction_amount, created_on
        ) VALUES (
          s.reward_id, s.user_id, s.transaction_id, s.points, s.transaction_amount, s.created_on
        )
    """
    sf.executemany(sql, rows)
    return MergeCounts(inserted=inserted, updated=updated)


def merge_reward_redemptions(sf: SnowflakeClient, rows: list[tuple]) -> MergeCounts:
    keys = [r[0] for r in rows]
    existing = _fetch_existing_string_keys(sf, "REWARD_REDEMPTIONS", "redemption_id", keys)
    inserted = sum(1 for k in keys if k not in existing)
    updated = len(keys) - inserted

    sql = """
        MERGE INTO REWARD_REDEMPTIONS t
        USING (
          SELECT %s AS redemption_id, %s AS user_id, %s AS transaction_id,
                 %s AS points_used, %s AS rupee_value, %s AS created_on
        ) s
        ON t.redemption_id = s.redemption_id
        WHEN MATCHED THEN UPDATE SET
          user_id = s.user_id,
          transaction_id = s.transaction_id,
          points_used = s.points_used,
          rupee_value = s.rupee_value,
          created_on = s.created_on
        WHEN NOT MATCHED THEN INSERT (
          redemption_id, user_id, transaction_id, points_used, rupee_value, created_on
        ) VALUES (
          s.redemption_id, s.user_id, s.transaction_id, s.points_used, s.rupee_value, s.created_on
        )
    """
    sf.executemany(sql, rows)
    return MergeCounts(inserted=inserted, updated=updated)


def _chunked(xs: list, size: int) -> Iterable[list]:
    for i in range(0, len(xs), size):
        yield xs[i : i + size]


def _fetch_existing_string_keys(sf: SnowflakeClient, table: str, col: str, keys: list[str]) -> set[str]:
    if not keys:
        return set()
    out: set[str] = set()
    # Chunk to avoid overly long IN clauses.
    for batch in _chunked(sorted(set(keys)), 500):
        placeholders = ",".join(["%s"] * len(batch))
        sql = f"SELECT {col} FROM {table} WHERE {col} IN ({placeholders})"
        rows = sf.fetch_all(sql, tuple(batch))
        out.update(str(r[0]) for r in rows)
    return out


def _fetch_existing_number_keys(sf: SnowflakeClient, table: str, col: str, keys: list[int]) -> set[int]:
    if not keys:
        return set()
    out: set[int] = set()
    for batch in _chunked(sorted(set(keys)), 500):
        placeholders = ",".join(["%s"] * len(batch))
        sql = f"SELECT {col} FROM {table} WHERE {col} IN ({placeholders})"
        rows = sf.fetch_all(sql, tuple(batch))
        out.update(int(r[0]) for r in rows)
    return out



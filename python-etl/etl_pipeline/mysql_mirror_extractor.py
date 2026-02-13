from __future__ import annotations

"""
MySQL -> Snowflake mirror extractors.

Why mirror?
- The Snowflake schema matches MySQL exactly (same table/column names)
- ETL avoids complex mappings/dimensions and focuses on reliable replication
"""

from dataclasses import dataclass
from datetime import datetime
import logging
from decimal import Decimal
from typing import Any

import mysql.connector

from .config import MySQLConfig

log = logging.getLogger(__name__)


@dataclass(frozen=True)
class UserRow:
    id: int
    username: str
    password: str
    role: str
    enabled: bool


@dataclass(frozen=True)
class AccountRow:
    account_id: str
    holder_name: str | None
    balance: Decimal
    status: str | None
    version: int | None
    last_updated: datetime | None
    user_id: int | None


@dataclass(frozen=True)
class TransactionLogRow:
    transaction_id: str
    idempotency_key: str
    from_account_id: str | None
    to_account_id: str | None
    amount: Decimal
    status: str | None
    failure_reason: str | None
    created_on: datetime


def _connect(cfg: MySQLConfig):
    return mysql.connector.connect(
        host=cfg.host,
        port=cfg.port,
        user=cfg.user,
        password=cfg.password,
        database=cfg.database,
    )


def extract_users(cfg: MySQLConfig) -> list[UserRow]:
    sql = """
        SELECT id, username, password, role, enabled
        FROM users
        ORDER BY id ASC
    """
    conn = _connect(cfg)
    try:
        cur = conn.cursor(dictionary=True)
        cur.execute(sql)
        rows = cur.fetchall()
        log.info("MySQL extracted USERS rows=%s", len(rows))
        out: list[UserRow] = []
        for r in rows:
            out.append(
                UserRow(
                    id=int(r["id"]),
                    username=str(r["username"]),
                    password=str(r["password"]),
                    role=str(r["role"]),
                    enabled=bool(r["enabled"]),
                )
            )
        return out
    finally:
        conn.close()


def extract_accounts(cfg: MySQLConfig) -> list[AccountRow]:
    sql = """
        SELECT account_id, holder_name, balance, status, version, last_updated, user_id
        FROM accounts
        ORDER BY account_id ASC
    """
    conn = _connect(cfg)
    try:
        cur = conn.cursor(dictionary=True)
        cur.execute(sql)
        rows = cur.fetchall()
        log.info("MySQL extracted ACCOUNTS rows=%s", len(rows))
        out: list[AccountRow] = []
        for r in rows:
            out.append(
                AccountRow(
                    account_id=str(r["account_id"]),
                    holder_name=r.get("holder_name"),
                    balance=Decimal(str(r["balance"])),
                    status=r.get("status"),
                    version=(int(r["version"]) if r.get("version") is not None else None),
                    last_updated=r.get("last_updated"),
                    user_id=(int(r["user_id"]) if r.get("user_id") is not None else None),
                )
            )
        return out
    finally:
        conn.close()


def extract_transaction_logs_all(cfg: MySQLConfig) -> list[TransactionLogRow]:
    sql = """
        SELECT
          transaction_id,
          idempotency_key,
          from_account_id,
          to_account_id,
          amount,
          status,
          failure_reason,
          created_on
        FROM transaction_logs
        ORDER BY created_on ASC
    """
    conn = _connect(cfg)
    try:
        cur = conn.cursor(dictionary=True)
        cur.execute(sql)
        rows = cur.fetchall()
        log.info("MySQL extracted TRANSACTION_LOGS rows=%s", len(rows))
        return [_to_tx_row(r) for r in rows]
    finally:
        conn.close()


def extract_transaction_logs_since(cfg: MySQLConfig, last_run_exclusive: datetime) -> list[TransactionLogRow]:
    """
    Incremental extraction based on created_on watermark.

    We use a strict greater-than filter per requirement:
    WHERE created_on > last_run
    """
    sql = """
        SELECT
          transaction_id,
          idempotency_key,
          from_account_id,
          to_account_id,
          amount,
          status,
          failure_reason,
          created_on
        FROM transaction_logs
        WHERE created_on > %s
        ORDER BY created_on ASC
    """
    conn = _connect(cfg)
    try:
        cur = conn.cursor(dictionary=True)
        cur.execute(sql, (last_run_exclusive,))
        rows = cur.fetchall()
        log.info("MySQL extracted TRANSACTION_LOGS rows=%s", len(rows))
        return [_to_tx_row(r) for r in rows]
    finally:
        conn.close()


def _to_tx_row(r: dict[str, Any]) -> TransactionLogRow:
    created_on = r["created_on"]
    if not isinstance(created_on, datetime):
        raise TypeError(f"Unexpected created_on type: {type(created_on)}")
    return TransactionLogRow(
        transaction_id=str(r["transaction_id"]),
        idempotency_key=str(r["idempotency_key"]),
        from_account_id=(str(r["from_account_id"]) if r.get("from_account_id") is not None else None),
        to_account_id=(str(r["to_account_id"]) if r.get("to_account_id") is not None else None),
        amount=Decimal(str(r["amount"])),
        status=(str(r["status"]) if r.get("status") is not None else None),
        failure_reason=(str(r["failure_reason"]) if r.get("failure_reason") is not None else None),
        # Treat MySQL timestamps as UTC-naive for this pipeline
        created_on=created_on.replace(tzinfo=None),
    )



from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
import logging

from .config import AppConfig
from .mysql_mirror_extractor import (
    extract_accounts,
    extract_reward_grants_all,
    extract_reward_grants_since,
    extract_reward_redemptions_all,
    extract_reward_redemptions_since,
    extract_transaction_logs_all,
    extract_transaction_logs_since,
    extract_users,
)
from .snowflake_client import SnowflakeClient
from .snowflake_mirror_loader import (
    ensure_mirror_tables,
    merge_accounts,
    merge_reward_grants,
    merge_reward_redemptions,
    merge_transaction_logs,
    merge_users,
)
from .watermark import read_watermark, watermark_exists, write_watermark

log = logging.getLogger(__name__)


@dataclass(frozen=True)
class EtlRunResult:
    mode: str
    extracted: int
    loaded: int


def run_etl_once(cfg: AppConfig) -> EtlRunResult:
    """
    One ETL run:
    Mirror design:
    - Snowflake tables mirror MySQL tables 1:1 (no star schema, no surrogate keys).

    Incremental strategy:
    - If watermark missing: FULL load (extract all tables)
    - If watermark exists: incremental load for TRANSACTION_LOGS, REWARD_GRANTS, REWARD_REDEMPTIONS
      using created_on > last_run (USERS and ACCOUNTS merged fully each run).

    Idempotency:
    - Snowflake MERGE is used for USERS/ACCOUNTS/TRANSACTION_LOGS based on their natural PKs.
    """
    watermark_file = cfg.etl.watermark_file
    is_full_load = not watermark_exists(watermark_file)
    mode = "full" if is_full_load else "incremental"
    log.info("Using watermark file: %s (exists=%s)", watermark_file, (not is_full_load))

    sf = SnowflakeClient(cfg.snowflake)
    try:
        ensure_mirror_tables(sf, cfg.snowflake.warehouse, cfg.snowflake.database, cfg.snowflake.schema)

        # Extract
        users = extract_users(cfg.mysql)
        accounts = extract_accounts(cfg.mysql)

        if is_full_load:
            log.info("Watermark missing -> running FULL load")
            tx_logs = extract_transaction_logs_all(cfg.mysql)
            reward_grants = extract_reward_grants_all(cfg.mysql)
            reward_redemptions = extract_reward_redemptions_all(cfg.mysql)
        else:
            wm = read_watermark(watermark_file)
            log.info(
                "Watermark found -> INCREMENTAL where created_on > %s",
                wm.last_run_utc.isoformat(),
            )
            tx_logs = extract_transaction_logs_since(cfg.mysql, wm.last_run_utc)
            reward_grants = extract_reward_grants_since(cfg.mysql, wm.last_run_utc)
            reward_redemptions = extract_reward_redemptions_since(cfg.mysql, wm.last_run_utc)

        extracted_total = (
            len(users) + len(accounts) + len(tx_logs)
            + len(reward_grants) + len(reward_redemptions)
        )
        log.info(
            "Extract summary mode=%s USERS=%s ACCOUNTS=%s TX=%s GRANTS=%s REDEMPTIONS=%s total=%s",
            mode, len(users), len(accounts), len(tx_logs),
            len(reward_grants), len(reward_redemptions), extracted_total,
        )

        # Load (MERGE for idempotency)
        users_params = [(u.id, u.username, u.password, u.role, u.enabled) for u in users]
        accounts_params = [
            (
                a.account_id,
                a.holder_name,
                a.balance,
                a.status,
                a.version,
                a.last_updated,
                a.user_id,
            )
            for a in accounts
        ]
        tx_params = [
            (
                t.transaction_id,
                t.idempotency_key,
                t.from_account_id,
                t.to_account_id,
                t.amount,
                t.status,
                t.failure_reason,
                t.created_on,
            )
            for t in tx_logs
        ]
        reward_params = [
            (
                r.reward_id,
                r.user_id,
                r.transaction_id,
                r.points,
                r.transaction_amount,
                r.created_on,
            )
            for r in reward_grants
        ]

        redemption_params = [
            (
                r.redemption_id,
                r.user_id,
                r.transaction_id,
                r.points_used,
                r.rupee_value,
                r.created_on,
            )
            for r in reward_redemptions
        ]

        counts_users = merge_users(sf, users_params) if users_params else None
        counts_accounts = merge_accounts(sf, accounts_params) if accounts_params else None
        counts_tx = merge_transaction_logs(sf, tx_params) if tx_params else None
        counts_rewards = merge_reward_grants(sf, reward_params) if reward_params else None
        counts_redemptions = merge_reward_redemptions(sf, redemption_params) if redemption_params else None

        if counts_users:
            log.info("USERS merge inserted=%s updated=%s", counts_users.inserted, counts_users.updated)
        if counts_accounts:
            log.info("ACCOUNTS merge inserted=%s updated=%s", counts_accounts.inserted, counts_accounts.updated)
        if counts_tx:
            log.info(
                "TRANSACTION_LOGS merge inserted=%s updated=%s",
                counts_tx.inserted,
                counts_tx.updated,
            )
        if counts_rewards:
            log.info(
                "REWARD_GRANTS merge inserted=%s updated=%s",
                counts_rewards.inserted,
                counts_rewards.updated,
            )
        if counts_redemptions:
            log.info(
                "REWARD_REDEMPTIONS merge inserted=%s updated=%s",
                counts_redemptions.inserted,
                counts_redemptions.updated,
            )

        timestamps = (
            [t.created_on for t in tx_logs]
            + [r.created_on for r in reward_grants]
            + [r.created_on for r in reward_redemptions]
        )
        if timestamps:
            write_watermark(watermark_file, max(timestamps))
        elif is_full_load:
            # Empty tables: write "now" so we don't full-load forever.
            write_watermark(watermark_file, datetime.utcnow())

        loaded_total = (
            (len(users_params) if users_params else 0)
            + (len(accounts_params) if accounts_params else 0)
            + (len(tx_params) if tx_params else 0)
            + (len(reward_params) if reward_params else 0)
            + (len(redemption_params) if redemption_params else 0)
        )
        return EtlRunResult(mode=mode, extracted=extracted_total, loaded=loaded_total)
    finally:
        sf.close()



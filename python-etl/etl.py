from __future__ import annotations

import argparse
from datetime import datetime
import logging
import os
import signal
import sys
import time

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.interval import IntervalTrigger
from dotenv import load_dotenv

from etl_pipeline.config import load_config
from etl_pipeline.etl_job import run_etl_once
from etl_pipeline.logging_setup import setup_logging


log = logging.getLogger("etl")


def _parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Money Transfer - MySQL -> Snowflake ETL")
    p.add_argument(
        "--once",
        action="store_true",
        help="Run a single ETL cycle and exit (no scheduler).",
    )
    p.add_argument(
        "--env-file",
        default=".env",
        help="Path to dotenv file (default: .env).",
    )
    return p.parse_args()


def _load_env(env_file: str) -> None:
    if os.path.exists(env_file):
        load_dotenv(env_file)
        return
    # Fallback: support using the provided template name directly.
    if env_file == ".env" and os.path.exists("env"):
        load_dotenv("env")


def _run_once_with_logging(*, raise_on_error: bool) -> None:
    cfg = load_config()
    setup_logging(cfg.etl.log_level)

    started = datetime.utcnow()
    log.info("ETL run started at %s UTC", started.isoformat())
    try:
        result = run_etl_once(cfg)
        log.info(
            "ETL run finished: mode=%s extracted=%s loaded=%s",
            result.mode,
            result.extracted,
            result.loaded,
        )
    except Exception:
        log.exception("ETL run failed")
        if raise_on_error:
            raise


def main() -> int:
    args = _parse_args()
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    _load_env(args.env_file)

    # Logging needs config; do a minimal default until env is loaded.
    if not logging.getLogger().handlers:
        logging.basicConfig(
            level=logging.INFO,
            format="%(asctime)s %(levelname)s %(name)s - %(message)s",
        )

    if args.once:
        _run_once_with_logging(raise_on_error=True)
        return 0

    cfg = load_config()
    setup_logging(cfg.etl.log_level)
    poll_seconds = max(60, int(cfg.etl.poll_seconds))

    # Run immediately on startup, then continue on schedule.
    _run_once_with_logging(raise_on_error=False)

    scheduler = BackgroundScheduler()
    scheduler.add_job(
        lambda: _run_once_with_logging(raise_on_error=False),
        trigger=IntervalTrigger(seconds=poll_seconds),
        id="money_transfer_etl",
        max_instances=1,
        coalesce=True,
        misfire_grace_time=60,
        replace_existing=True,
    )
    scheduler.start()
    log.info("Scheduler started (interval=%ss). Press Ctrl+C to stop.", poll_seconds)

    stop = {"flag": False}

    def _handle_stop(_sig: int, _frame: object) -> None:
        stop["flag"] = True

    signal.signal(signal.SIGINT, _handle_stop)
    signal.signal(signal.SIGTERM, _handle_stop)

    try:
        while not stop["flag"]:
            time.sleep(0.5)
    finally:
        scheduler.shutdown(wait=True)
        log.info("Scheduler stopped.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())



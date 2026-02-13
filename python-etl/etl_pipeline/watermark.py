from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
import logging
from pathlib import Path

log = logging.getLogger(__name__)


@dataclass(frozen=True)
class Watermark:
    """
    File-based watermark for incremental extraction.

    We store a single UTC-naive timestamp (ISO-8601) representing the latest processed
    `created_on` (aka `created_at`) from the source system.
    """

    last_run_utc: datetime


def watermark_exists(path: str) -> bool:
    return Path(path).exists()


def read_watermark(path: str) -> Watermark:
    """
    Read last_run timestamp from last_run.txt.

    Expected format: ISO-8601 datetime, e.g. 2026-02-13T12:34:56.123456
    """
    p = Path(path)
    raw = p.read_text(encoding="utf-8").strip()
    if not raw:
        raise ValueError(f"Watermark file {path!r} is empty")
    try:
        ts = datetime.fromisoformat(raw)
    except ValueError as e:
        raise ValueError(f"Invalid watermark timestamp in {path!r}: {raw!r}") from e
    # Treat as UTC-naive for this pipeline.
    return Watermark(last_run_utc=ts.replace(tzinfo=None))


def write_watermark(path: str, ts: datetime) -> None:
    """
    Persist the new watermark *after a successful load*.

    We write atomically to prevent partial writes (temp file then replace).
    """
    p = Path(path)
    tmp = p.with_suffix(p.suffix + ".tmp")
    payload = ts.replace(tzinfo=None).isoformat()
    tmp.write_text(payload + "\n", encoding="utf-8")
    tmp.replace(p)
    log.info("Watermark updated: %s=%s", str(p), payload)



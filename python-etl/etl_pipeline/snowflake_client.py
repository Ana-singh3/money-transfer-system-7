from __future__ import annotations

from contextlib import contextmanager
import logging
from typing import Any, Iterator

import snowflake.connector

from .config import SnowflakeConfig

log = logging.getLogger(__name__)


class SnowflakeClient:
    def __init__(self, cfg: SnowflakeConfig):
        self._cfg = cfg
        self._conn = None

    def connect(self) -> None:
        if self._conn is not None:
            return
        # Connect without assuming the database/schema/warehouse already exist.
        self._conn = snowflake.connector.connect(
            account=self._cfg.account,
            user=self._cfg.user,
            password=self._cfg.password,
            autocommit=True,
        )

    def close(self) -> None:
        if self._conn is None:
            return
        try:
            self._conn.close()
        finally:
            self._conn = None

    @contextmanager
    def cursor(self) -> Iterator[Any]:
        self.connect()
        assert self._conn is not None
        cur = self._conn.cursor()
        try:
            yield cur
        finally:
            cur.close()

    def execute(self, sql: str, params: tuple[Any, ...] | None = None) -> None:
        with self.cursor() as cur:
            cur.execute(sql, params) if params else cur.execute(sql)

    def executemany(self, sql: str, seq: list[tuple[Any, ...]]) -> None:
        if not seq:
            return
        with self.cursor() as cur:
            cur.executemany(sql, seq)

    def fetch_all(
        self, sql: str, params: tuple[Any, ...] | None = None
    ) -> list[tuple[Any, ...]]:
        with self.cursor() as cur:
            cur.execute(sql, params) if params else cur.execute(sql)
            return list(cur.fetchall())



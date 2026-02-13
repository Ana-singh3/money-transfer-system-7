from __future__ import annotations

from dataclasses import dataclass
import os


def _require(name: str) -> str:
    value = os.getenv(name)
    if value is None or value == "":
        raise ValueError(f"Missing required environment variable: {name}")
    return value


def _optional(name: str, default: str) -> str:
    value = os.getenv(name)
    return default if value is None or value == "" else value


@dataclass(frozen=True)
class MySQLConfig:
    host: str
    port: int
    user: str
    password: str
    database: str


@dataclass(frozen=True)
class SnowflakeConfig:
    account: str
    user: str
    password: str
    warehouse: str
    database: str
    schema: str


@dataclass(frozen=True)
class EtlConfig:
    poll_seconds: int
    watermark_file: str
    log_level: str


@dataclass(frozen=True)
class AppConfig:
    mysql: MySQLConfig
    snowflake: SnowflakeConfig
    etl: EtlConfig


def load_config() -> AppConfig:
    mysql = MySQLConfig(
        host=_optional("MYSQL_HOST", "localhost"),
        port=int(_optional("MYSQL_PORT", "3306")),
        user=_require("MYSQL_USER"),
        password=_optional("MYSQL_PASSWORD", ""),
        database=_optional("MYSQL_DATABASE", "moneytransfer"),
    )
    snowflake = SnowflakeConfig(
        account=_require("SNOWFLAKE_ACCOUNT"),
        user=_require("SNOWFLAKE_USER"),
        password=_require("SNOWFLAKE_PASSWORD"),
        warehouse=_optional("SNOWFLAKE_WAREHOUSE", "COMPUTE_WH"),
        database=_optional("SNOWFLAKE_DATABASE", "MONEY_TRANSFER_DW"),
        schema=_optional("SNOWFLAKE_SCHEMA", "ANALYTICS"),
    )
    etl = EtlConfig(
        poll_seconds=int(_optional("ETL_POLL_SECONDS", "300")),
        # Default per requirement: last_run.txt
        # Back-compat: if someone still sets ETL_STATE_FILE, we'll use it as the watermark file.
        watermark_file=_optional("WATERMARK_FILE", _optional("ETL_STATE_FILE", "last_run.txt")),
        log_level=_optional("LOG_LEVEL", "INFO").upper(),
    )
    return AppConfig(mysql=mysql, snowflake=snowflake, etl=etl)



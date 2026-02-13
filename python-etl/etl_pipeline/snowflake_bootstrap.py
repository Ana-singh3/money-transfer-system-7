from __future__ import annotations

"""
Snowflake bootstrap utilities.

We keep this separate from any schema-specific loader so that:
- bootstrapping (warehouse/db/schema) is reusable
- loaders can create only the tables they own (mirror vs star schema, etc.)
"""

import logging

from .snowflake_client import SnowflakeClient

log = logging.getLogger(__name__)


def _validate_ident(name: str, value: str) -> str:
    ok = value.replace("_", "").isalnum()
    if not ok:
        raise ValueError(f"Invalid Snowflake identifier for {name}: {value!r}")
    return value


def ensure_warehouse_db_schema(sf: SnowflakeClient, warehouse: str, database: str, schema: str) -> None:
    warehouse = _validate_ident("warehouse", warehouse)
    database = _validate_ident("database", database)
    schema = _validate_ident("schema", schema)

    sf.execute(
        f"""
        CREATE WAREHOUSE IF NOT EXISTS {warehouse}
          WAREHOUSE_SIZE = 'XSMALL'
          AUTO_SUSPEND = 60
          AUTO_RESUME = TRUE
          INITIALLY_SUSPENDED = TRUE
        """
    )
    sf.execute(f"CREATE DATABASE IF NOT EXISTS {database}")
    sf.execute(f"CREATE SCHEMA IF NOT EXISTS {database}.{schema}")
    sf.execute(f"USE WAREHOUSE {warehouse}")
    sf.execute(f"USE DATABASE {database}")
    sf.execute(f"USE SCHEMA {schema}")



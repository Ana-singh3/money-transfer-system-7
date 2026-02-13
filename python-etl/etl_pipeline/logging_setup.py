from __future__ import annotations

import logging
import sys


def setup_logging(level: str) -> None:
    root = logging.getLogger()
    if root.handlers:
        # Avoid duplicate handlers if running under a reloader / interactive session.
        return

    root.setLevel(level)
    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(level)
    formatter = logging.Formatter(
        fmt="%(asctime)s %(levelname)s %(name)s - %(message)s",
        datefmt="%Y-%m-%dT%H:%M:%S%z",
    )
    handler.setFormatter(formatter)
    root.addHandler(handler)



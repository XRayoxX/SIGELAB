from __future__ import annotations

import logging
import time
from contextlib import contextmanager
from typing import Iterator

import pyodbc

from .config import Settings
from .errors import DatabaseUnavailableError


logger = logging.getLogger(__name__)

# Errores típicos de Azure SQL mientras la base serverless se reanuda
# o durante una conmutación transitoria.
_TRANSIENT_CODES = ("40613", "40197", "40501", "49918", "49919", "49920", "4060", "HYT00", "08S01", "08001")


def _is_transient(exc: Exception) -> bool:
    text = " ".join(str(arg) for arg in getattr(exc, "args", ()))
    return any(code in text for code in _TRANSIENT_CODES)


class Database:
    def __init__(self, settings: Settings):
        self.settings = settings

    def connect(self) -> pyodbc.Connection:
        attempts = max(1, self.settings.db_connect_retries)
        last: Exception | None = None
        for attempt in range(1, attempts + 1):
            try:
                return pyodbc.connect(self.settings.connection_string, timeout=self.settings.db_connect_timeout)
            except pyodbc.Error as exc:
                last = exc
                if attempt == attempts or not _is_transient(exc):
                    break
                wait = 5 * attempt
                logger.warning("Azure SQL no disponible (intento %s/%s); reintentando en %ss", attempt, attempts, wait)
                time.sleep(wait)
        logger.error("No fue posible conectar a la base de datos: %s", last)
        raise DatabaseUnavailableError() from last

    def ping(self) -> bool:
        try:
            with self.connect() as connection:
                connection.execute("SELECT 1").fetchone()
            return True
        except (pyodbc.Error, DatabaseUnavailableError):
            return False

    @contextmanager
    def cursor(self) -> Iterator[pyodbc.Cursor]:
        connection = self.connect()
        try:
            cursor = connection.cursor()
            yield cursor
            connection.commit()
        except pyodbc.Error as exc:
            connection.rollback()
            logger.exception("Error SQL")
            raise DatabaseUnavailableError("Error al consultar la base de datos.") from exc
        finally:
            connection.close()

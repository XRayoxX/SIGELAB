"""Creación idempotente de las tablas faciales (se puede ejecutar al arrancar con AUTO_MIGRATE=true)."""
from __future__ import annotations

import logging
import re
from pathlib import Path

from .config import Settings
from .db import Database


logger = logging.getLogger(__name__)
SQL_FILE = Path(__file__).resolve().parents[1] / "sql" / "001_sigelab_facial_tables.sql"


def apply_migrations(settings: Settings) -> int:
    script = SQL_FILE.read_text(encoding="utf-8")
    script = script.replace(
        "REFERENCES dbo.Usuario(IdUsuario)",
        f"REFERENCES dbo.[{settings.users_table}]([{settings.user_id_column}])",
    )
    batches = [b.strip() for b in re.split(r"^\s*GO\s*$", script, flags=re.MULTILINE | re.IGNORECASE) if b.strip()]
    with Database(settings).connect() as connection:
        cursor = connection.cursor()
        for batch in batches:
            cursor.execute(batch)
        connection.commit()
        return int(cursor.execute(
            "SELECT COUNT(*) FROM sys.tables WHERE name IN (N'PerfilFacial', N'AuditoriaFacial')"
        ).fetchone()[0])


def apply_migrations_safely(settings: Settings) -> None:
    try:
        count = apply_migrations(settings)
        logger.info("AUTO_MIGRATE: %s/2 tablas faciales presentes en %s", count, settings.db_name)
    except Exception:
        logger.exception("AUTO_MIGRATE falló; revise DB_* y que exista la tabla %s", settings.users_table)

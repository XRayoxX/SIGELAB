"""Crea las tablas PerfilFacial y AuditoriaFacial en Azure SQL usando las variables DB_* del .env.

    python -m scripts.migrate

Usa USERS_TABLE / USER_ID_COLUMN para la llave foránea hacia la tabla de usuarios.
"""
from app.config import settings
from app.migrations import apply_migrations


if __name__ == "__main__":
    count = apply_migrations(settings)
    print(f"Migración aplicada en {settings.db_name}: {count}/2 tablas faciales presentes.")

from __future__ import annotations

import logging
from datetime import datetime
from typing import Any

from .config import Settings
from .db import Database


logger = logging.getLogger(__name__)

_TRUE_VALUES = {"1", "TRUE", "T", "Y", "S", "SI", "SÍ", "A", "ACTIVO", "ACTIVE", "HABILITADO"}


def _is_active(value: Any) -> bool:
    if value is None:
        return False
    if isinstance(value, (bool, int)):
        return bool(value)
    return str(value).strip().upper() in _TRUE_VALUES


class FacialRepository:
    """Acceso a SQL. La tabla de usuarios de SIGELAB es configurable por variables de entorno."""

    def __init__(self, database: Database, settings: Settings):
        self.database = database
        self.s = settings

    # ---------- Usuarios (tabla propia de SIGELAB) ----------
    def get_user(self, user_id: int) -> dict[str, Any] | None:
        s = self.s
        role = f"[{s.user_role_column}]" if s.user_role_column else "NULL"
        active = f"[{s.user_active_column}]" if s.user_active_column else "1"
        sql = (
            f"SELECT [{s.user_id_column}], [{s.user_name_column}], {role}, {active} "
            f"FROM dbo.[{s.users_table}] WHERE [{s.user_id_column}] = ?"
        )
        with self.database.cursor() as cursor:
            row = cursor.execute(sql, user_id).fetchone()
        if row is None:
            return None
        return {
            "id_usuario": int(row[0]),
            "usuario": str(row[1]),
            "rol": "" if row[2] is None else str(row[2]).strip(),
            "activo": _is_active(row[3]),
        }

    def get_usernames(self, user_ids: list[int]) -> dict[int, str]:
        if not user_ids:
            return {}
        s = self.s
        placeholders = ",".join("?" for _ in user_ids)
        sql = (
            f"SELECT [{s.user_id_column}], [{s.user_name_column}] FROM dbo.[{s.users_table}] "
            f"WHERE [{s.user_id_column}] IN ({placeholders})"
        )
        with self.database.cursor() as cursor:
            rows = cursor.execute(sql, *user_ids).fetchall()
        return {int(r[0]): str(r[1]) for r in rows}

    # ---------- Perfiles faciales ----------
    def get_profile(self, user_id: int) -> dict[str, Any] | None:
        sql = """
            SELECT id_perfil_facial, id_usuario, embedding, modelo, dimension,
                   fecha_registro, fecha_actualizacion, cantidad_muestras,
                   quality_score, version_algoritmo
            FROM dbo.PerfilFacial
            WHERE id_usuario = ? AND activo = 1
        """
        with self.database.cursor() as cursor:
            row = cursor.execute(sql, user_id).fetchone()
        if row is None:
            return None
        return {
            "id_perfil_facial": int(row[0]),
            "id_usuario": int(row[1]),
            "embedding": bytes(row[2]),
            "modelo": str(row[3]),
            "dimension": int(row[4]),
            "fecha_registro": row[5],
            "fecha_actualizacion": row[6],
            "cantidad_muestras": int(row[7]),
            "quality_score": float(row[8]) if row[8] is not None else None,
            "version_algoritmo": str(row[9]),
        }

    def all_profiles(self) -> list[dict[str, Any]]:
        sql = "SELECT id_usuario, embedding, dimension FROM dbo.PerfilFacial WHERE activo = 1"
        with self.database.cursor() as cursor:
            rows = cursor.execute(sql).fetchall()
        return [{"id_usuario": int(r[0]), "embedding": bytes(r[1]), "dimension": int(r[2])} for r in rows]

    def save_profile(
        self,
        user_id: int,
        embedding: bytes,
        model: str,
        dimension: int,
        sample_count: int,
        quality_score: float,
        algorithm_version: str,
    ) -> None:
        update = """
            UPDATE dbo.PerfilFacial
            SET embedding = ?, modelo = ?, dimension = ?,
                fecha_actualizacion = SYSDATETIME(), activo = 1,
                cantidad_muestras = ?, quality_score = ?, version_algoritmo = ?
            WHERE id_usuario = ?
        """
        insert = """
            INSERT INTO dbo.PerfilFacial
                (id_usuario, embedding, modelo, dimension, fecha_registro,
                 fecha_actualizacion, activo, cantidad_muestras, quality_score,
                 version_algoritmo)
            VALUES (?, ?, ?, ?, SYSDATETIME(), SYSDATETIME(), 1, ?, ?, ?)
        """
        with self.database.cursor() as cursor:
            updated = cursor.execute(
                update, embedding, model, dimension, sample_count, quality_score, algorithm_version, user_id
            ).rowcount
            if updated == 0:
                cursor.execute(
                    insert, user_id, embedding, model, dimension, sample_count, quality_score, algorithm_version
                )

    def delete_profile(self, user_id: int) -> bool:
        with self.database.cursor() as cursor:
            return cursor.execute("DELETE FROM dbo.PerfilFacial WHERE id_usuario = ?", user_id).rowcount > 0

    # ---------- Auditoría ----------
    def write_audit(
        self,
        user_id: int | None,
        action: str,
        result: str,
        score: float | None,
        threshold: float | None,
        detail: str,
        device: str,
    ) -> None:
        sql = """
            INSERT INTO dbo.AuditoriaFacial
                (id_usuario, fecha_hora, accion, resultado, score, threshold, detalle, dispositivo)
            VALUES (?, SYSDATETIME(), ?, ?, ?, ?, ?, ?)
        """
        try:
            with self.database.cursor() as cursor:
                cursor.execute(sql, user_id, action[:30], result[:20], score, threshold, detail[:1000], device[:200])
        except Exception:
            # Una falla de auditoría nunca debe ocultar el resultado facial.
            logger.exception("No fue posible registrar auditoría facial")


def iso(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value.isoformat()
    return str(value)

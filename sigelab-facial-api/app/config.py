"""Configuración por variables de entorno.

En Azure Container Apps todas las variables se definen en la propia app
(los valores sensibles como *secrets*). En local se puede usar un archivo .env.
"""
from __future__ import annotations

import base64
import hashlib
import hmac
import os
import re
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv


SERVICE_ROOT = Path(__file__).resolve().parents[1]
load_dotenv(SERVICE_ROOT / ".env")

_IDENTIFIER = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")


def _str(name: str, default: str = "") -> str:
    value = os.getenv(name)
    return default if value is None else value.strip()


def _int(name: str, default: int) -> int:
    value = os.getenv(name)
    return default if value is None or value.strip() == "" else int(value)


def _float(name: str, default: float) -> float:
    value = os.getenv(name)
    return default if value is None or value.strip() == "" else float(value)


def _bool(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None or value.strip() == "":
        return default
    return value.strip().lower() in {"1", "true", "yes", "on", "si", "sí"}


def _csv(name: str, default: tuple[str, ...] = ()) -> tuple[str, ...]:
    value = os.getenv(name)
    if value is None or value.strip() == "":
        return default
    return tuple(item.strip() for item in value.split(",") if item.strip())


def _identifier(name: str, default: str, optional: bool = False) -> str:
    """Nombres de tabla/columna configurables, validados para evitar inyección SQL."""
    value = _str(name, default)
    if value == "" and optional:
        return ""
    if not _IDENTIFIER.match(value):
        raise ValueError(f"{name}='{value}' no es un identificador SQL válido.")
    return value


@dataclass(frozen=True)
class Settings:
    # --- Azure SQL ---
    db_host: str
    db_port: int
    db_name: str
    db_user: str
    db_password: str
    db_driver: str
    db_encrypt: bool
    db_trust_server_certificate: bool
    db_connect_timeout: int
    db_connect_retries: int

    # --- Tabla de usuarios de SIGELAB (configurable) ---
    users_table: str
    user_id_column: str
    user_name_column: str
    user_role_column: str
    user_active_column: str
    admin_roles: tuple[str, ...]

    # --- Seguridad de la API ---
    api_keys: tuple[str, ...]
    facial_encryption_key: str
    challenge_secret: str
    challenge_ttl_seconds: int

    # --- Modelo ---
    face_model: str
    insightface_home: str
    onnx_threads: int
    inference_concurrency: int

    # --- Calibración ---
    face_match_threshold: float
    same_person_threshold: float
    duplicate_check: bool
    face_min_size: int
    blur_threshold: float
    brightness_min: float
    brightness_max: float
    max_yaw: float
    max_pitch: float
    front_max_yaw: float
    turn_min_yaw: float

    # --- Reglas de captura ---
    require_liveness: bool
    enroll_min_images: int
    enroll_min_front: int
    max_images: int
    max_image_bytes: int
    max_image_side: int
    identify_cache_seconds: int
    auto_migrate: bool

    @classmethod
    def from_env(cls) -> "Settings":
        return cls(
            db_host=_str("DB_HOST", "sigelabumg.database.windows.net"),
            db_port=_int("DB_PORT", 1433),
            db_name=_str("DB_NAME", "sigelabadministrador"),
            db_user=_str("DB_USER", ""),
            db_password=os.getenv("DB_PASSWORD", ""),
            db_driver=_str("DB_DRIVER", "ODBC Driver 18 for SQL Server"),
            db_encrypt=_bool("DB_ENCRYPT", True),
            # Azure SQL tiene certificado válido: no hace falta confiar a ciegas.
            db_trust_server_certificate=_bool("DB_TRUST_SERVER_CERTIFICATE", False),
            # La base gratuita (serverless) se pausa sola y tarda en "despertar".
            db_connect_timeout=_int("DB_CONNECT_TIMEOUT", 15),
            db_connect_retries=_int("DB_CONNECT_RETRIES", 4),
            users_table=_identifier("USERS_TABLE", "Usuario"),
            user_id_column=_identifier("USER_ID_COLUMN", "IdUsuario"),
            user_name_column=_identifier("USER_NAME_COLUMN", "NombreUsuario"),
            user_role_column=_identifier("USER_ROLE_COLUMN", "IdRol", optional=True),
            user_active_column=_identifier("USER_ACTIVE_COLUMN", "Estado", optional=True),
            admin_roles=tuple(r.upper() for r in _csv("ADMIN_ROLES", ("1",))),
            api_keys=_csv("API_KEYS"),
            facial_encryption_key=_str("FACIAL_ENCRYPTION_KEY"),
            challenge_secret=_str("CHALLENGE_SECRET"),
            challenge_ttl_seconds=_int("CHALLENGE_TTL_SECONDS", 120),
            face_model=_str("FACE_MODEL", "buffalo_l"),
            insightface_home=_str("INSIGHTFACE_HOME", str(Path.home() / ".insightface")),
            onnx_threads=_int("ONNX_THREADS", 0),
            inference_concurrency=_int("INFERENCE_CONCURRENCY", 2),
            face_match_threshold=_float("FACE_MATCH_THRESHOLD", 0.55),
            same_person_threshold=_float("SAME_PERSON_THRESHOLD", 0.40),
            duplicate_check=_bool("DUPLICATE_CHECK", True),
            face_min_size=_int("FACE_MIN_SIZE", 90),
            blur_threshold=_float("BLUR_THRESHOLD", 60),
            brightness_min=_float("BRIGHTNESS_MIN", 45),
            brightness_max=_float("BRIGHTNESS_MAX", 220),
            max_yaw=_float("MAX_YAW", 40),
            max_pitch=_float("MAX_PITCH", 25),
            front_max_yaw=_float("FRONT_MAX_YAW", 10),
            turn_min_yaw=_float("TURN_MIN_YAW", 15),
            require_liveness=_bool("REQUIRE_LIVENESS", True),
            enroll_min_images=_int("ENROLL_MIN_IMAGES", 5),
            enroll_min_front=_int("ENROLL_MIN_FRONT", 2),
            max_images=_int("MAX_IMAGES", 12),
            max_image_bytes=_int("MAX_IMAGE_BYTES", 4 * 1024 * 1024),
            max_image_side=_int("MAX_IMAGE_SIDE", 1280),
            identify_cache_seconds=_int("IDENTIFY_CACHE_SECONDS", 300),
            auto_migrate=_bool("AUTO_MIGRATE", False),
        )

    @property
    def connection_string(self) -> str:
        encrypt = "yes" if self.db_encrypt else "no"
        trust = "yes" if self.db_trust_server_certificate else "no"
        server = self.db_host if self.db_host.startswith("tcp:") else f"tcp:{self.db_host}"
        return (
            f"DRIVER={{{self.db_driver}}};"
            f"SERVER={server},{self.db_port};"
            f"DATABASE={self.db_name};"
            f"UID={self.db_user};PWD={self.db_password};"
            f"Encrypt={encrypt};TrustServerCertificate={trust};"
            f"Connection Timeout={self.db_connect_timeout};"
        )

    def encryption_key_bytes(self) -> bytes:
        if not self.facial_encryption_key:
            raise ValueError("FACIAL_ENCRYPTION_KEY no está configurada.")
        try:
            key = base64.b64decode(self.facial_encryption_key, validate=True)
        except Exception as exc:
            raise ValueError("FACIAL_ENCRYPTION_KEY no es base64 válida.") from exc
        if len(key) not in {16, 24, 32}:
            raise ValueError("FACIAL_ENCRYPTION_KEY debe decodificar a 16, 24 o 32 bytes.")
        return key

    def challenge_key_bytes(self) -> bytes:
        """Clave HMAC de los desafíos de prueba de vida.

        Si CHALLENGE_SECRET no está definido se deriva de la clave de cifrado,
        así el servicio funciona con un secreto menos.
        """
        if self.challenge_secret:
            return self.challenge_secret.encode("utf-8")
        return hmac.new(self.encryption_key_bytes(), b"sigelab-liveness-challenge", hashlib.sha256).digest()


settings = Settings.from_env()

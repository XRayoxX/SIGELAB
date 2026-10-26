from __future__ import annotations

from typing import Any


class FacialServiceError(Exception):
    """Error controlado: se devuelve al cliente como JSON {detail, code, ...}."""

    def __init__(
        self,
        message: str,
        status_code: int = 400,
        code: str = "FACIAL_ERROR",
        extra: dict[str, Any] | None = None,
    ):
        super().__init__(message)
        self.message = message
        self.status_code = status_code
        self.code = code
        self.extra = extra or {}


class ModelUnavailableError(FacialServiceError):
    def __init__(self, message: str = "El modelo facial no está disponible."):
        super().__init__(message, 503, "MODELO_NO_DISPONIBLE")


class InvalidImageError(FacialServiceError):
    def __init__(self, message: str, index: int | None = None):
        extra = {"imageIndex": index} if index is not None else None
        super().__init__(message, 400, "IMAGEN_INVALIDA", extra)


class DatabaseUnavailableError(FacialServiceError):
    def __init__(self, message: str = "La base de datos no está disponible. Intente de nuevo en unos segundos."):
        super().__init__(message, 503, "BD_NO_DISPONIBLE")

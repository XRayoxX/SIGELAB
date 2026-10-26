"""API key y desafíos firmados para la prueba de vida."""
from __future__ import annotations

import base64
import hashlib
import hmac
import json
import secrets
import threading
import time

from fastapi import Header

from .config import Settings, settings
from .errors import FacialServiceError


CHALLENGES = {
    "turn_left": "Mire al frente y luego gire ligeramente la cabeza hacia su IZQUIERDA.",
    "turn_right": "Mire al frente y luego gire ligeramente la cabeza hacia su DERECHA.",
}


def require_api_key(x_api_key: str | None = Header(default=None, alias="X-API-Key")) -> None:
    """Dependencia de FastAPI: exige el header X-API-Key en los endpoints protegidos."""
    if not settings.api_keys:
        raise FacialServiceError("El servidor no tiene API_KEYS configuradas.", 503, "API_KEY_NO_CONFIGURADA")
    supplied = (x_api_key or "").encode("utf-8")
    if not any(hmac.compare_digest(supplied, key.encode("utf-8")) for key in settings.api_keys):
        raise FacialServiceError("API key inválida o ausente (header X-API-Key).", 401, "NO_AUTORIZADO")


def _b64(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _unb64(text: str) -> bytes:
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


class ChallengeManager:
    """Emite desafíos aleatorios firmados (HMAC) y evita que un mismo desafío se reutilice."""

    def __init__(self, configuration: Settings):
        self.settings = configuration
        self._used: dict[str, float] = {}
        self._lock = threading.Lock()

    def issue(self, user_id: int | None) -> dict[str, object]:
        challenge = secrets.choice(sorted(CHALLENGES))
        expires = int(time.time()) + self.settings.challenge_ttl_seconds
        payload = {"c": challenge, "u": int(user_id or 0), "e": expires, "n": secrets.token_hex(8)}
        body = _b64(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
        signature = _b64(hmac.new(self.settings.challenge_key_bytes(), body.encode("ascii"), hashlib.sha256).digest())
        return {
            "challengeToken": f"{body}.{signature}",
            "challenge": challenge,
            "instruction": CHALLENGES[challenge],
            "expiresInSeconds": self.settings.challenge_ttl_seconds,
        }

    def consume(self, token: str | None, user_id: int | None) -> str:
        """Valida firma, vencimiento, usuario y uso único. Devuelve el tipo de desafío."""
        if not token:
            raise FacialServiceError(
                "Falta challengeToken: pida uno en GET /api/face/challenge antes de capturar.",
                400,
                "DESAFIO_REQUERIDO",
            )
        try:
            body, signature = token.split(".", 1)
            expected = _b64(hmac.new(self.settings.challenge_key_bytes(), body.encode("ascii"), hashlib.sha256).digest())
            if not hmac.compare_digest(signature, expected):
                raise ValueError("firma")
            payload = json.loads(_unb64(body))
        except Exception as exc:
            raise FacialServiceError("challengeToken inválido.", 400, "DESAFIO_INVALIDO") from exc
        now = time.time()
        if payload.get("e", 0) < now:
            raise FacialServiceError("El desafío venció; solicite uno nuevo.", 400, "DESAFIO_VENCIDO")
        bound_user = int(payload.get("u", 0))
        if bound_user and user_id is not None and bound_user != user_id:
            raise FacialServiceError("El desafío pertenece a otro usuario.", 400, "DESAFIO_INVALIDO")
        nonce = str(payload.get("n"))
        with self._lock:
            for key in [k for k, exp in self._used.items() if exp < now]:
                del self._used[key]
            if nonce in self._used:
                raise FacialServiceError("Este desafío ya fue utilizado; solicite uno nuevo.", 400, "DESAFIO_USADO")
            self._used[nonce] = float(payload["e"])
        challenge = str(payload.get("c"))
        if challenge not in CHALLENGES:
            raise FacialServiceError("challengeToken inválido.", 400, "DESAFIO_INVALIDO")
        return challenge

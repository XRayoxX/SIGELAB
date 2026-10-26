from __future__ import annotations

import logging
import threading
import time

import numpy as np

from .config import Settings
from .crypto import decrypt_embedding, encrypt_embedding, normalize
from .engine import FaceEngine, FrameAnalysis
from .errors import FacialServiceError, ModelUnavailableError
from .images import decode_image
from .liveness import check_liveness, mean_embedding
from .repository import FacialRepository, iso
from .security import ChallengeManager
from .similarity import cosine_similarity


logger = logging.getLogger(__name__)

ALGORITHM_VERSION = "rgb-arcface-cloud-v1"


def consistency(frames: list[FrameAnalysis]) -> float:
    """Peor similitud de cada foto contra el promedio de las demás (detecta fotos de otra persona)."""
    if len(frames) < 2:
        return 1.0
    vectors = np.vstack([f.embedding for f in frames])
    total = vectors.sum(axis=0)
    return float(min(cosine_similarity(vectors[i], total - vectors[i]) for i in range(len(frames))))


class ProfileCache:
    """Embeddings descifrados de todos los perfiles, para identificar (1:N) sin pedir userId."""

    def __init__(self, repository: FacialRepository, settings: Settings):
        self.repository = repository
        self.settings = settings
        self._lock = threading.Lock()
        self._loaded_at: float | None = None
        self._ids: list[int] = []
        self._matrix: np.ndarray | None = None

    def invalidate(self) -> None:
        with self._lock:
            self._loaded_at = None

    def get(self) -> tuple[list[int], np.ndarray | None]:
        with self._lock:
            if self._loaded_at is None or time.monotonic() - self._loaded_at > self.settings.identify_cache_seconds:
                key = self.settings.encryption_key_bytes()
                ids, vectors = [], []
                for row in self.repository.all_profiles():
                    try:
                        vectors.append(decrypt_embedding(row["embedding"], key, row["dimension"]))
                        ids.append(row["id_usuario"])
                    except Exception:
                        logger.warning("Perfil facial ilegible para user_id=%s (¿cambió la clave?)", row["id_usuario"])
                self._ids = ids
                self._matrix = np.vstack(vectors) if vectors else None
                self._loaded_at = time.monotonic()
            return self._ids, self._matrix

    def best_match(self, probe: np.ndarray, exclude: int | None = None) -> tuple[int | None, float, float]:
        """Devuelve (user_id, mejor score, segundo mejor score)."""
        ids, matrix = self.get()
        if matrix is None:
            return None, 0.0, 0.0
        scores = matrix @ normalize(probe)
        order = np.argsort(-scores)
        candidates = [(ids[i], float(scores[i])) for i in order if ids[i] != exclude]
        if not candidates:
            return None, 0.0, 0.0
        second = candidates[1][1] if len(candidates) > 1 else 0.0
        return candidates[0][0], candidates[0][1], second


class FaceService:
    def __init__(self, repository: FacialRepository, engine: FaceEngine | None, settings: Settings):
        self.repository = repository
        self.engine = engine
        self.settings = settings
        self.challenges = ChallengeManager(settings)
        self.cache = ProfileCache(repository, settings)

    # ------------------------------------------------------------------ util
    @property
    def model_loaded(self) -> bool:
        return self.engine is not None and self.engine.model_loaded

    def _require_engine(self) -> FaceEngine:
        if not self.model_loaded:
            raise ModelUnavailableError()
        return self.engine  # type: ignore[return-value]

    def analyze_images(self, images: list[bytes], mirrored: bool) -> list[FrameAnalysis]:
        if not images:
            raise FacialServiceError("Debe enviar al menos una imagen (campo 'images').", 400, "SIN_IMAGENES")
        if len(images) > self.settings.max_images:
            raise FacialServiceError(
                f"Máximo {self.settings.max_images} imágenes por petición.", 400, "DEMASIADAS_IMAGENES"
            )
        engine = self._require_engine()
        results = []
        for index, data in enumerate(images):
            frame = decode_image(data, self.settings, index, mirrored)
            results.append(engine.analyze(frame, index))
        return results

    @staticmethod
    def _frames_public(frames: list[FrameAnalysis]) -> list[dict[str, object]]:
        return [f.public() for f in frames]

    def _require_active_user(self, user_id: int) -> dict[str, object]:
        user = self.repository.get_user(user_id)
        if user is None:
            raise FacialServiceError("El usuario no existe.", 404, "USUARIO_NO_EXISTE")
        if not user["activo"]:
            raise FacialServiceError("El usuario está inactivo.", 403, "USUARIO_INACTIVO")
        return user

    def _audit(self, user_id, action, result, score, detail, device, threshold=None) -> None:
        self.repository.write_audit(user_id, action, result, score, threshold, detail, device)

    def _fail(self, user_id, action, device, error: FacialServiceError) -> FacialServiceError:
        self._audit(user_id, action, "DENIED", None, f"{error.code}: {error.message}", device)
        return error

    def _accepted_or_fail(self, frames: list[FrameAnalysis]) -> list[FrameAnalysis]:
        accepted = [f for f in frames if f.accepted]
        if not accepted:
            raise FacialServiceError(
                "Ninguna foto tiene un rostro válido. " + frames[0].reason,
                422,
                "CALIDAD_INSUFICIENTE",
                {"frames": self._frames_public(frames)},
            )
        return accepted

    def _liveness(self, frames, token, user_id) -> tuple[bool, str]:
        if not self.settings.require_liveness and not token:
            return False, ""
        challenge = self.challenges.consume(token, user_id)
        result = check_liveness(frames, challenge, self.settings)
        if not result.passed:
            raise FacialServiceError(
                result.reason, 422, "LIVENESS_FALLIDO", {"frames": self._frames_public(frames), "challenge": challenge}
            )
        return True, challenge

    @staticmethod
    def _probe(accepted: list[FrameAnalysis]) -> np.ndarray:
        front = [f for f in accepted if f.pose == "front"]
        return mean_embedding(front or accepted)

    # ------------------------------------------------------------- endpoints
    def health(self) -> dict[str, object]:
        return {
            "status": "ok" if self.model_loaded else "degraded",
            "model": self.settings.face_model,
            "modelLoaded": self.model_loaded,
            "livenessRequired": self.settings.require_liveness,
        }

    def status(self, user_id: int) -> dict[str, object]:
        if self.repository.get_user(user_id) is None:
            raise FacialServiceError("El usuario no existe.", 404, "USUARIO_NO_EXISTE")
        profile = self.repository.get_profile(user_id)
        if profile is None:
            return {"userId": user_id, "enrolled": False}
        return {
            "userId": user_id,
            "enrolled": True,
            "registeredAt": iso(profile["fecha_registro"]),
            "updatedAt": iso(profile["fecha_actualizacion"]),
            "model": profile["modelo"],
            "dimension": profile["dimension"],
            "samples": profile["cantidad_muestras"],
        }

    def analyze(self, images: list[bytes], mirrored: bool) -> dict[str, object]:
        frames = self.analyze_images(images, mirrored)
        return {"frames": self._frames_public(frames)}

    def enroll(self, user_id: int, replace: bool, images: list[bytes], mirrored: bool, device: str) -> dict[str, object]:
        user = self._require_active_user(user_id)
        if self.repository.get_profile(user_id) is not None and not replace:
            raise self._fail(user_id, "ENROLL_FALLIDO", device, FacialServiceError(
                "El usuario ya tiene un perfil facial. Envíe replace=true para actualizarlo.", 409, "PERFIL_YA_EXISTE"
            ))
        try:
            frames = self.analyze_images(images, mirrored)
            accepted = self._accepted_or_fail(frames)
            counts = {pose: sum(1 for f in accepted if f.pose == pose) for pose in ("front", "left", "right")}
            missing = []
            if counts["front"] < self.settings.enroll_min_front:
                missing.append(f"al menos {self.settings.enroll_min_front} fotos de frente")
            if counts["left"] < 1:
                missing.append("una foto girando a la izquierda")
            if counts["right"] < 1:
                missing.append("una foto girando a la derecha")
            if len(accepted) < self.settings.enroll_min_images:
                missing.append(f"{self.settings.enroll_min_images} fotos válidas en total (hay {len(accepted)})")
            if missing:
                raise FacialServiceError(
                    "Registro incompleto. Falta: " + "; ".join(missing) + ".",
                    422,
                    "CAPTURA_INCOMPLETA",
                    {"frames": self._frames_public(frames), "counts": counts},
                )

            final = mean_embedding(accepted)
            if consistency(accepted) < self.settings.same_person_threshold:
                raise FacialServiceError(
                    "Las fotos no parecen ser de la misma persona. Repita la captura.",
                    422,
                    "PERSONAS_DISTINTAS",
                    {"frames": self._frames_public(frames)},
                )

            if self.settings.duplicate_check:
                other, score, _ = self.cache.best_match(final, exclude=user_id)
                if other is not None and score >= self.settings.face_match_threshold:
                    raise FacialServiceError(
                        "Este rostro ya está registrado para otro usuario.",
                        409,
                        "ROSTRO_YA_REGISTRADO",
                        {"otherUserId": other, "score": round(score, 4)},
                    )

            quality = float(np.mean([f.quality for f in accepted]))
            encrypted = encrypt_embedding(final, self.settings.encryption_key_bytes())
            self.repository.save_profile(
                user_id, encrypted, self.settings.face_model, int(final.size), len(accepted), quality, ALGORITHM_VERSION
            )
            self.cache.invalidate()
            self._audit(user_id, "ENROLL_EXITOSO", "MATCH", quality,
                        f"Usuario {user['usuario']}; muestras={len(accepted)}; poses={counts}", device)
            logger.info("Enrollment exitoso user_id=%s muestras=%s", user_id, len(accepted))
            return {
                "success": True,
                "userId": user_id,
                "model": self.settings.face_model,
                "dimension": int(final.size),
                "samples": len(accepted),
                "qualityScore": round(quality, 4),
                "livenessPassed": counts["left"] > 0 and counts["right"] > 0,
                "message": "Perfil facial registrado correctamente.",
                "frames": self._frames_public(frames),
            }
        except FacialServiceError as exc:
            raise self._fail(user_id, "ENROLL_FALLIDO", device, exc)

    def verify(self, user_id: int, images: list[bytes], token: str | None, mirrored: bool, device: str) -> dict[str, object]:
        user = self._require_active_user(user_id)
        profile = self.repository.get_profile(user_id)
        if profile is None:
            raise self._fail(user_id, "VERIFY_FALLIDO", device, FacialServiceError(
                "El usuario no posee un perfil facial registrado.", 404, "PERFIL_NO_REGISTRADO"
            ))
        try:
            frames = self.analyze_images(images, mirrored)
            accepted = self._accepted_or_fail(frames)
            try:
                liveness, challenge = self._liveness(frames, token, user_id)
            except FacialServiceError as exc:
                if exc.code == "LIVENESS_FALLIDO":
                    self._audit(user_id, "LIVENESS_FALLIDO", "DENIED", None, exc.message, device)
                raise
            stored = decrypt_embedding(profile["embedding"], self.settings.encryption_key_bytes(), profile["dimension"])
            score = cosine_similarity(self._probe(accepted), stored)
            threshold = self.settings.face_match_threshold
            matched = score >= threshold
            self._audit(user_id, "VERIFY_EXITOSO" if matched else "VERIFY_FALLIDO",
                        "MATCH" if matched else "NO_MATCH", score,
                        f"Usuario {user['usuario']}; frames={len(accepted)}; desafio={challenge}", device, threshold)
            return {
                "success": True,
                "matched": matched,
                "userId": user_id,
                "username": user["usuario"],
                "score": round(score, 4),
                "threshold": threshold,
                "livenessPassed": liveness,
                "framesEvaluated": len(accepted),
                "message": "Identidad verificada correctamente." if matched else "El rostro no coincide.",
                "frames": self._frames_public(frames),
            }
        except FacialServiceError as exc:
            if exc.code in {"LIVENESS_FALLIDO"}:
                raise
            raise self._fail(user_id, "VERIFY_FALLIDO", device, exc)

    def identify(self, images: list[bytes], token: str | None, mirrored: bool, device: str) -> dict[str, object]:
        try:
            frames = self.analyze_images(images, mirrored)
            accepted = self._accepted_or_fail(frames)
            try:
                liveness, challenge = self._liveness(frames, token, None)
            except FacialServiceError as exc:
                if exc.code == "LIVENESS_FALLIDO":
                    self._audit(None, "LIVENESS_FALLIDO", "DENIED", None, exc.message, device)
                raise
            user_id, score, second = self.cache.best_match(self._probe(accepted))
            threshold = self.settings.face_match_threshold
            matched = user_id is not None and score >= threshold
            username = None
            if matched:
                user = self.repository.get_user(user_id)
                if user is None or not user["activo"]:
                    matched = False
                else:
                    username = user["usuario"]
            self._audit(user_id if matched else None, "IDENTIFY_EXITOSO" if matched else "IDENTIFY_FALLIDO",
                        "MATCH" if matched else "NO_MATCH", score,
                        f"frames={len(accepted)}; desafio={challenge}; segundo={second:.4f}", device, threshold)
            return {
                "success": True,
                "matched": matched,
                "userId": user_id if matched else None,
                "username": username,
                "score": round(score, 4),
                "threshold": threshold,
                "livenessPassed": liveness,
                "framesEvaluated": len(accepted),
                "message": f"Usuario identificado: {username}." if matched else "No se reconoció a ningún usuario registrado.",
                "frames": self._frames_public(frames),
            }
        except FacialServiceError as exc:
            if exc.code != "LIVENESS_FALLIDO":
                self._audit(None, "IDENTIFY_FALLIDO", "DENIED", None, f"{exc.code}: {exc.message}", device)
            raise

    def delete_profile(self, user_id: int, admin_user_id: int, device: str) -> dict[str, object]:
        admin = self.repository.get_user(admin_user_id)
        is_admin = admin is not None and admin["activo"] and (
            not self.settings.user_role_column or str(admin["rol"]).upper() in self.settings.admin_roles
        )
        if not is_admin:
            raise FacialServiceError("Solo un usuario administrador activo puede eliminar perfiles.", 403, "ADMIN_REQUERIDO")
        if self.repository.get_user(user_id) is None:
            raise FacialServiceError("El usuario no existe.", 404, "USUARIO_NO_EXISTE")
        deleted = self.repository.delete_profile(user_id)
        self.cache.invalidate()
        self._audit(user_id, "PERFIL_ELIMINADO", "OK", None, f"Eliminado por admin {admin_user_id}", device)
        return {"success": True, "userId": user_id, "deleted": deleted, "message": "Perfil facial eliminado."}

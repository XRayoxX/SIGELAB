"""Motor facial: InsightFace (detección RetinaFace + reconocimiento ArcFace) sobre ONNX Runtime CPU.

A diferencia de la versión local, aquí no hay cámara ni ventanas: el motor
recibe frames ya decodificados y devuelve, por cada uno, el análisis del rostro.
"""
from __future__ import annotations

import logging
import threading
from dataclasses import dataclass, field

import numpy as np

from .config import Settings
from .crypto import normalize
from .errors import ModelUnavailableError
from .quality import evaluate_face


logger = logging.getLogger(__name__)


@dataclass
class FrameAnalysis:
    index: int
    faces: int
    accepted: bool
    reason: str
    yaw: float | None = None
    pitch: float | None = None
    pose: str | None = None  # front | left | right | partial
    quality: float | None = None
    embedding: np.ndarray | None = field(default=None, repr=False)

    def public(self) -> dict[str, object]:
        return {
            "index": self.index,
            "faces": self.faces,
            "accepted": self.accepted,
            "reason": self.reason,
            "pose": self.pose,
            "yaw": None if self.yaw is None else round(self.yaw, 1),
            "pitch": None if self.pitch is None else round(self.pitch, 1),
            "quality": None if self.quality is None else round(self.quality, 3),
        }


class FaceEngine:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.model = None
        self.model_loaded = False
        self._semaphore = threading.Semaphore(max(1, settings.inference_concurrency))
        try:
            import onnxruntime as ort
            from insightface.app import FaceAnalysis

            available = ort.get_available_providers()
            providers = ["CPUExecutionProvider"] if "CPUExecutionProvider" in available else available
            kwargs = {}
            if settings.onnx_threads > 0:
                options = ort.SessionOptions()
                options.intra_op_num_threads = settings.onnx_threads
                kwargs["session_options"] = options
            # Solo detección + reconocimiento: menos RAM y arranque más rápido.
            self.model = FaceAnalysis(
                name=settings.face_model,
                root=settings.insightface_home,
                providers=providers,
                allowed_modules=["detection", "recognition"],
                **kwargs,
            )
            self.model.prepare(ctx_id=-1, det_size=(640, 640))
            self.model_loaded = True
            logger.info("InsightFace %s listo (providers=%s)", settings.face_model, providers)
        except Exception as exc:
            logger.exception("No fue posible cargar InsightFace")
            raise ModelUnavailableError(f"No fue posible cargar el modelo facial: {exc}") from exc

    def detect(self, frame: np.ndarray) -> list[object]:
        if not self.model_loaded or self.model is None:
            raise ModelUnavailableError()
        with self._semaphore:
            return list(self.model.get(frame))

    @staticmethod
    def embedding(face: object) -> np.ndarray:
        value = getattr(face, "normed_embedding", None)
        if value is None:
            value = getattr(face, "embedding", None)
        if value is None:
            raise ValueError("El modelo no devolvió un embedding facial.")
        return normalize(np.asarray(value, dtype=np.float32))

    def analyze(self, frame: np.ndarray, index: int) -> FrameAnalysis:
        faces = self.detect(frame)
        if not faces:
            return FrameAnalysis(index, 0, False, "No se detectó ningún rostro.")
        if len(faces) > 1:
            return FrameAnalysis(index, len(faces), False, "Solo debe aparecer una persona en la foto.")
        face = faces[0]
        quality = evaluate_face(frame, face, self.settings)
        yaw, pitch = quality.pose.yaw, quality.pose.pitch
        pose = classify_pose(yaw, self.settings)
        if not quality.accepted:
            return FrameAnalysis(index, 1, False, quality.message, yaw, pitch, pose, quality.score)
        return FrameAnalysis(index, 1, True, "Rostro válido.", yaw, pitch, pose, quality.score, self.embedding(face))


def classify_pose(yaw: float, settings: Settings) -> str:
    """Izquierda/derecha desde el punto de vista de la persona (yaw > 0 = giró a SU izquierda).

    Las fotos deben llegar sin espejo; si el cliente envía fotos en espejo
    (cámara frontal tipo selfie) debe mandar mirrored=true.
    """
    if abs(yaw) <= settings.front_max_yaw:
        return "front"
    if yaw >= settings.turn_min_yaw:
        return "left"
    if yaw <= -settings.turn_min_yaw:
        return "right"
    return "partial"

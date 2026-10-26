"""Prueba de vida por desafío: el servidor pide girar a un lado elegido al azar.

Una foto impresa o una imagen en pantalla no puede girar la cabeza, y el
cliente no sabe de antemano hacia qué lado se le pedirá. Además se exige que
las fotos de frente y la girada sean de la misma persona.

Es una mitigación académica, no un anti-spoofing comercial.
"""
from __future__ import annotations

from dataclasses import dataclass

import numpy as np

from .config import Settings
from .crypto import normalize
from .engine import FrameAnalysis
from .similarity import cosine_similarity


@dataclass(frozen=True)
class LivenessResult:
    passed: bool
    reason: str
    turn_frames: int = 0


_DIRECTION = {"turn_left": "left", "turn_right": "right"}
_NAMES = {"left": "IZQUIERDA", "right": "DERECHA"}


def mean_embedding(frames: list[FrameAnalysis]) -> np.ndarray:
    return normalize(np.mean([f.embedding for f in frames], axis=0))


def check_liveness(frames: list[FrameAnalysis], challenge: str, settings: Settings) -> LivenessResult:
    accepted = [f for f in frames if f.accepted and f.embedding is not None]
    front = [f for f in accepted if f.pose == "front"]
    wanted = _DIRECTION[challenge]
    opposite = "right" if wanted == "left" else "left"
    turned = [f for f in accepted if f.pose == wanted]

    if not front:
        return LivenessResult(False, "Falta al menos una foto válida mirando al frente.")
    if not turned:
        if any(f.pose == opposite for f in accepted):
            return LivenessResult(
                False,
                f"Se detectó el giro hacia el lado contrario. Debe girar hacia su {_NAMES[wanted]}. "
                "Si su cámara entrega fotos en espejo, envíe mirrored=true.",
            )
        return LivenessResult(False, f"No se detectó el giro de cabeza hacia su {_NAMES[wanted]}.")

    reference = mean_embedding(front)
    for frame in turned:
        if cosine_similarity(frame.embedding, reference) < settings.same_person_threshold:
            return LivenessResult(False, "Las fotos de frente y de giro no parecen de la misma persona.")
    return LivenessResult(True, "Prueba de vida correcta.", len(turned))

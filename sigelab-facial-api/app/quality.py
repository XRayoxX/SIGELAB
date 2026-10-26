from __future__ import annotations

import math
from dataclasses import dataclass

import cv2
import numpy as np


@dataclass(frozen=True)
class Pose:
    yaw: float
    pitch: float


@dataclass(frozen=True)
class QualityResult:
    accepted: bool
    score: float
    message: str
    pose: Pose
    pose_bin: str


def estimate_pose(kps: object, frame_shape: tuple[int, ...]) -> Pose:
    """Pose aproximada a partir de los 5 puntos de InsightFace, sin depender de la inclinación (roll).

    Convención (desde el punto de vista de la PERSONA, en una foto sin espejo):
      yaw  > 0  -> giró la cabeza hacia SU izquierda
      yaw  < 0  -> giró la cabeza hacia SU derecha
      pitch > 0 -> mira hacia abajo; pitch < 0 -> mira hacia arriba

    Se rota el rostro para dejar los ojos horizontales y se mide cuánto se
    desplaza la punta de la nariz respecto al centro de los ojos. La nariz
    sobresale ~0.5 veces la distancia entre ojos, por eso yaw = atan(r / 0.5).
    """
    points = np.asarray(kps, dtype=np.float64)
    if points.shape != (5, 2):
        return Pose(0.0, 0.0)
    left_eye, right_eye = points[0], points[1]  # ojo a la izquierda / derecha de la IMAGEN
    angle = math.atan2(right_eye[1] - left_eye[1], right_eye[0] - left_eye[0])
    cos_a, sin_a = math.cos(-angle), math.sin(-angle)
    rotation = np.array([[cos_a, -sin_a], [sin_a, cos_a]])
    p = (points - left_eye) @ rotation.T
    eye_l, eye_r, nose, mouth_l, mouth_r = p
    eye_distance = eye_r[0] - eye_l[0]
    if eye_distance <= 1e-6:
        return Pose(0.0, 0.0)
    eyes_mid = (eye_l + eye_r) / 2.0
    mouth_mid = (mouth_l + mouth_r) / 2.0
    ratio = (nose[0] - eyes_mid[0]) / eye_distance
    yaw = math.degrees(math.atan(ratio / 0.5))
    vertical = mouth_mid[1] - eyes_mid[1]
    pitch = 0.0 if vertical <= 1e-6 else ((nose[1] - eyes_mid[1]) / vertical - 0.55) * 100.0
    return Pose(float(yaw), float(pitch))


def evaluate_face(frame: np.ndarray, face: object, configuration: object) -> QualityResult:
    bbox = np.asarray(getattr(face, "bbox", [0, 0, 0, 0]), dtype=int)
    if bbox.size != 4:
        return QualityResult(False, 0.0, "No se pudo leer el encuadre facial.", Pose(0, 0), "front")
    x1, y1, x2, y2 = bbox.tolist()
    width = max(0, min(frame.shape[1], x2) - max(0, x1))
    height = max(0, min(frame.shape[0], y2) - max(0, y1))
    if min(width, height) < configuration.face_min_size:
        return QualityResult(False, 0.1, "Acérquese un poco a la cámara.", Pose(0, 0), "front")

    crop = frame[max(0, y1):min(frame.shape[0], y2), max(0, x1):min(frame.shape[1], x2)]
    if crop.size == 0:
        return QualityResult(False, 0.0, "No se pudo analizar el rostro.", Pose(0, 0), "front")
    gray = cv2.cvtColor(crop, cv2.COLOR_BGR2GRAY)
    brightness = float(np.mean(gray))
    blur = float(cv2.Laplacian(gray, cv2.CV_64F).var())
    det_score = float(getattr(face, "det_score", 0.0))
    pose = estimate_pose(getattr(face, "kps", []), frame.shape)
    if det_score < 0.5:
        return QualityResult(False, 0.2, "Mantenga el rostro visible.", pose, "front")
    if brightness < configuration.brightness_min:
        return QualityResult(False, 0.25, "Hay poca iluminación en el rostro.", pose, "front")
    if brightness > configuration.brightness_max:
        return QualityResult(False, 0.25, "Reduzca la luz directa sobre el rostro.", pose, "front")
    if blur < configuration.blur_threshold:
        return QualityResult(False, 0.3, "Mantenga la cabeza quieta para enfocar.", pose, "front")
    if abs(pose.yaw) > configuration.max_yaw:
        return QualityResult(False, 0.35, "Reduzca el giro de la cabeza.", pose, "front")
    if abs(pose.pitch) > configuration.max_pitch:
        return QualityResult(False, 0.35, "Mire al frente de la cámara.", pose, "front")

    quality = min(
        1.0,
        max(
            0.0,
            0.35 * det_score
            + 0.25 * min(1.0, blur / max(configuration.blur_threshold * 4, 1))
            + 0.40,
        ),
    )
    if pose.yaw < -8:
        pose_bin = "left"
    elif pose.yaw > 8:
        pose_bin = "right"
    else:
        pose_bin = "front"
    return QualityResult(True, quality, "Rostro válido.", pose, pose_bin)

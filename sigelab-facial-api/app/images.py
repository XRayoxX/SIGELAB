"""Decodificación de las fotos que envían el celular y la app de escritorio.

Las imágenes solo viven en memoria durante la petición: nunca se guardan.
"""
from __future__ import annotations

import io

import cv2
import numpy as np
from PIL import Image, ImageOps, UnidentifiedImageError

from .config import Settings
from .errors import InvalidImageError


def decode_image(data: bytes, settings: Settings, index: int | None = None, mirrored: bool = False) -> np.ndarray:
    """Convierte bytes JPEG/PNG en un frame BGR (formato OpenCV / InsightFace).

    - Respeta la orientación EXIF (las fotos de celular suelen venir rotadas).
    - Reduce el tamaño si el lado mayor supera MAX_IMAGE_SIDE.
    - Si el cliente indica `mirrored=true` (foto en espejo, típico de la cámara
      frontal del celular) se invierte para que izquierda/derecha sean reales.
    """
    if not data:
        raise InvalidImageError("Una de las imágenes está vacía.", index)
    if len(data) > settings.max_image_bytes:
        mb = settings.max_image_bytes / (1024 * 1024)
        raise InvalidImageError(f"La imagen supera {mb:.0f} MB; redúzcala antes de enviarla.", index)
    try:
        with Image.open(io.BytesIO(data)) as image:
            image = ImageOps.exif_transpose(image)
            image = image.convert("RGB")
            width, height = image.size
            longest = max(width, height)
            if longest > settings.max_image_side:
                scale = settings.max_image_side / float(longest)
                image = image.resize((max(1, int(width * scale)), max(1, int(height * scale))), Image.LANCZOS)
            rgb = np.asarray(image, dtype=np.uint8)
    except (UnidentifiedImageError, OSError, ValueError) as exc:
        raise InvalidImageError("El archivo enviado no es una imagen JPEG/PNG válida.", index) from exc
    frame = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
    if mirrored:
        frame = cv2.flip(frame, 1)
    return np.ascontiguousarray(frame)

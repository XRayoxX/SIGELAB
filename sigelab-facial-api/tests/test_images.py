import io

import numpy as np
import pytest
from PIL import Image

from app.errors import InvalidImageError
from app.images import decode_image


def jpeg(width, height, exif_orientation=None):
    image = Image.new("RGB", (width, height), (200, 10, 10))
    image.putpixel((0, 0), (0, 255, 0))
    buffer = io.BytesIO()
    if exif_orientation:
        exif = Image.Exif()
        exif[0x0112] = exif_orientation
        image.save(buffer, "JPEG", exif=exif)
    else:
        image.save(buffer, "JPEG")
    return buffer.getvalue()


def test_large_photo_is_resized_and_bgr(settings):
    frame = decode_image(jpeg(4000, 3000), settings)
    assert max(frame.shape[:2]) == settings.max_image_side
    assert frame[50, 50, 2] > 150  # rojo queda en el canal R de BGR


def test_exif_rotation_is_applied(settings):
    frame = decode_image(jpeg(400, 200, exif_orientation=6), settings)
    assert frame.shape[:2] == (400, 200)


def test_mirrored_flag_flips_image(settings):
    normal = decode_image(jpeg(100, 50), settings)
    flipped = decode_image(jpeg(100, 50), settings, mirrored=True)
    assert np.array_equal(normal[:, ::-1], flipped)


def test_invalid_bytes_are_rejected(settings):
    with pytest.raises(InvalidImageError):
        decode_image(b"esto no es una imagen", settings, 0)

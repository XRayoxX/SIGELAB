"""Descarga el modelo de InsightFace y deja solo los archivos que usa la API.

Se ejecuta dentro del Dockerfile para que la imagen ya traiga el modelo y el
contenedor no tenga que descargarlo cada vez que Azure lo enciende.
"""
from __future__ import annotations

import os
from pathlib import Path

from insightface.utils.storage import ensure_available

KEEP = {
    "buffalo_l": {"det_10g.onnx", "w600k_r50.onnx"},
    "buffalo_s": {"det_500m.onnx", "w600k_mbf.onnx"},
}


def main() -> None:
    name = os.getenv("FACE_MODEL", "buffalo_l")
    root = os.getenv("INSIGHTFACE_HOME", str(Path.home() / ".insightface"))
    model_dir = Path(ensure_available("models", name, root=root))
    keep = KEEP.get(name)
    if keep:
        for onnx in model_dir.glob("*.onnx"):
            if onnx.name not in keep:
                onnx.unlink()
    zip_file = model_dir.parent / f"{name}.zip"
    if zip_file.exists():
        zip_file.unlink()
    print(f"Modelo {name} listo en {model_dir}: {sorted(p.name for p in model_dir.glob('*.onnx'))}")


if __name__ == "__main__":
    main()

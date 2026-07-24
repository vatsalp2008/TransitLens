"""Export the trained YOLO detector to TFLite and copy it into the app assets.

YOLO TFLite output is decoded on-device (raw boxes + class scores); the safety
threshold from ADR-006 is applied in the Android decoder / fusion, not here.
"""
import argparse
import shutil
from pathlib import Path

from config import APP_MODELS_DIR, EXPORT_DIR, IMG_SIZE, RUNS_DIR


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--weights", default=str(RUNS_DIR / "detector" / "weights" / "best.pt"))
    ap.add_argument("--imgsz", type=int, default=IMG_SIZE)
    ap.add_argument("--copy-to-app", action="store_true")
    args = ap.parse_args()

    try:
        from ultralytics import YOLO
    except ImportError as exc:  # pragma: no cover
        raise SystemExit("Install training deps first: pip install -r ml_training/requirements.txt") from exc

    model = YOLO(args.weights)
    exported = Path(model.export(format="tflite", imgsz=args.imgsz))
    print("Exported:", exported, f"({exported.stat().st_size / 1024:.0f} KB)")

    EXPORT_DIR.mkdir(parents=True, exist_ok=True)
    target = EXPORT_DIR / "object_detector.tflite"
    shutil.copy(exported, target)
    print("Copied ->", target)

    if args.copy_to_app:
        APP_MODELS_DIR.mkdir(parents=True, exist_ok=True)
        shutil.copy(exported, APP_MODELS_DIR / "object_detector.tflite")
        print("Copied ->", APP_MODELS_DIR / "object_detector.tflite")


if __name__ == "__main__":
    main()

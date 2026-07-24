"""Train the transit object detector with Ultralytics YOLOv8-nano.

Chosen over the deprecated TFLite Model Maker for reliable arm64 support (ADR-009).
Reports mAP@0.5 on the validation set.
"""
import argparse

from config import DATA_YAML, IMG_SIZE, RUNS_DIR


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--epochs", type=int, default=30)
    ap.add_argument("--batch", type=int, default=16)
    ap.add_argument("--imgsz", type=int, default=IMG_SIZE)
    ap.add_argument("--data", default=str(DATA_YAML))
    ap.add_argument("--weights", default="yolov8n.pt")
    args = ap.parse_args()

    try:
        from ultralytics import YOLO
    except ImportError as exc:  # pragma: no cover
        raise SystemExit("Install training deps first: pip install -r ml_training/requirements.txt") from exc

    model = YOLO(args.weights)
    results = model.train(
        data=args.data,
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        project=str(RUNS_DIR),
        name="detector",
        exist_ok=True,
        verbose=True,
    )
    # results.box.map50 is mAP@0.5 on the val split.
    try:
        print(f"\nmAP@0.5: {results.box.map50:.4f}")
    except Exception:
        pass


if __name__ == "__main__":
    main()

"""Shared configuration for the transit scene classifier pipeline."""
from pathlib import Path

# Must match SceneClass order in :core (SceneClass.index 0..5).
CLASSES = [
    "bus_stop",
    "train_platform",
    "street_corner",
    "vehicle_interior",
    "transfer_hub",
    "unknown",
]

IMG_SIZE = 224

_HERE = Path(__file__).resolve().parent          # ml_training/scene_classifier
_ML = _HERE.parent                               # ml_training
_REPO = _ML.parent                               # repo root

DATA_DIR = _ML / "data" / "scene"                # gitignored
MODEL_DIR = _ML / "runs" / "scene"               # gitignored (SavedModel/Keras + reports)
EXPORT_DIR = _ML / "exports" / "scene"           # gitignored (.tflite)
APP_MODELS_DIR = _REPO / "app" / "src" / "main" / "assets" / "models"  # gitignored

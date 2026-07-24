"""Shared configuration for the transit object detector pipeline."""
from pathlib import Path

# Detection classes (see spec Model 2). Index order is the model's class ids and
# must be mirrored by the Android decoder mapping to :core DetectedObject.
CLASSES = [
    "bus",
    "train_car",
    "elevator_door",
    "escalator",
    "crosswalk_marking",
    "wheelchair_ramp",
    "tactile_paving",
    "accessibility_sign",
]

IMG_SIZE = 320

_HERE = Path(__file__).resolve().parent          # ml_training/object_detector
_ML = _HERE.parent                               # ml_training
_REPO = _ML.parent                               # repo root

DATA_DIR = _ML / "data" / "detect"               # gitignored
DATA_YAML = DATA_DIR / "data.yaml"
RUNS_DIR = _ML / "runs" / "detect"               # gitignored
EXPORT_DIR = _ML / "exports" / "detect"          # gitignored
APP_MODELS_DIR = _REPO / "app" / "src" / "main" / "assets" / "models"  # gitignored

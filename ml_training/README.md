# TransitLens — ML training

Trains the two on-device models and exports them to TFLite for the Android app.
See [ADR-002](../docs/ADR-002-mobilenetv3-small.md) and
[ADR-009](../docs/ADR-009-object-detector-training.md) for the model and
toolchain decisions.

## Setup

```bash
python3.12 -m venv ml_training/.venv          # system 3.14 is too new for TF
ml_training/.venv/bin/pip install -r ml_training/requirements.txt
# macOS: let weight downloads verify TLS
export SSL_CERT_FILE=$(ml_training/.venv/bin/python -c "import certifi; print(certifi.where())")
export REQUESTS_CA_BUNDLE=$SSL_CERT_FILE
```

## Dataset status (read this)

The scripts default to `--mode smoke`: a small **synthetic** dataset that
validates the train → export → verify pipeline without any downloads. It is
**not** a production dataset. Production accuracy (ADR-002 targets: scene top-1
> 85%, detector mAP@0.5 > 72%) requires the real data — Open Images
(`bus_stop`/`train_station`, `bus`/`train`), Mapillary Vistas street-level
imagery, and 200+/500+ custom-annotated Seattle photos. Use `--mode openimages`
for assembly guidance, then re-run the same scripts.

## Scene classifier (`scene_classifier/`)

```bash
PY=ml_training/.venv/bin/python
$PY scene_classifier/collect_data.py --mode smoke
$PY scene_classifier/train.py --epochs 8
$PY scene_classifier/evaluate.py                 # accuracy + confusion matrix
$PY scene_classifier/export_tflite.py --copy-to-app
```

Exports `scene_classifier_fp16.tflite` (~1.9 MB) and `scene_classifier_int8.tflite`
(~1.2 MB); both are float32 I/O, `[1,224,224,3]` → `[1,6]`, and are copied into
`app/src/main/assets/models/`.

## Object detector (`object_detector/`)

```bash
$PY object_detector/make_dataset.py --mode smoke
$PY object_detector/train.py --epochs 30 --imgsz 320   # Ultralytics YOLOv8n; prints mAP@0.5
```

TFLite export: the default Ultralytics LiteRT export is incompatible with very
new torch (ADR-009), so convert via ONNX + onnx2tf:

```bash
$PY -c "from ultralytics import YOLO; YOLO('ml_training/runs/detect/detector/weights/best.pt').export(format='onnx', imgsz=320, opset=13)"
ml_training/.venv/bin/onnx2tf -i ml_training/runs/detect/detector/weights/best.onnx -o ml_training/exports/detect/tf
cp ml_training/exports/detect/tf/best_float32.tflite app/src/main/assets/models/object_detector.tflite
```

Detector TFLite: input `[1,160,160,3]` float32, output `[1,12,525]` (4 bbox +
8 class scores × anchors), decoded on-device.

## Outputs

`data/`, `runs/`, `exports/`, and `app/src/main/assets/models/*.tflite` are
gitignored (large binaries / regenerable). Commit only the scripts.

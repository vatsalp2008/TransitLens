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
> 85%, detector mAP@0.5 > 72%) requires real data — fetched by the `--mode`
options below into the same `data/` layout, then trained on a GPU (Colab).

### Real data (no manual photography needed for most classes)

**Scene classifier** — `data/scene/{train,val,test}/{class}/`:
```bash
PY=ml_training/.venv/bin/python
$PY scene_classifier/collect_data.py --mode places365 --max-per-class 200   # all 6 classes; streams ~500 MB
$PY scene_classifier/collect_data.py --mode openimages --max-per-class 300  # bus_stop/train_platform/street_corner
# or both at once:
$PY scene_classifier/collect_data.py --mode real
```
Places365 → class mapping (validated in `datasets_places365.py`): `airport_terminal`→transfer_hub,
`bus_interior`/`train_interior`→vehicle_interior, `bus_station`→bus_stop, `crosswalk`/`street`→street_corner,
`subway_station`/`train_station`/`railroad_track`→train_platform, plus a random non-transit sample → unknown.

**Object detector** — `data/detect/{train,val}/{images,labels}/`:
```bash
$PY object_detector/make_dataset.py --mode openimages --max-samples 400   # bus + train boxes
$PY object_detector/make_dataset.py --mode roboflow                       # accessibility classes (see below)
```
Four **Roboflow Universe** datasets are already wired in `object_detector/datasets_roboflow.py`
(crosswalk + zebra-crossing → crosswalk_marking; tactile-pavement; escalator). Put your free
key in a gitignored `ml_training/.env` (`ROBOFLOW_API_KEY=...`) and run the command above — it
downloads, converts segmentation polygons → bboxes, remaps class names to our 8-class scheme,
and merges (~2,600 labeled images; verified). Still needing a good public dataset:
**elevator_door, wheelchair_ramp, accessibility_sign** — add more projects to `DATASETS`.

macOS: `export SSL_CERT_FILE=$(ml_training/.venv/bin/python -c "import certifi;print(certifi.where())")`
for the Places365 downloads. Old `--mode smoke` still works for a zero-download pipeline check.

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

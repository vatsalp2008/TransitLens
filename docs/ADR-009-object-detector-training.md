# ADR-009: Object-detector training toolchain (Model Maker is deprecated)

- Status: Accepted
- Date: 2026-07-23 (updated 2026-07-24 with the exercised outcome)

## Context

The original spec names **TFLite Model Maker** to fine-tune the object detector.
It is deprecated and unmaintained: it pins old TensorFlow/Python (~3.9) and does
not install on current Python (3.12) or Apple Silicon (arm64), the dev
environment here.

## Decision

- **Scene classifier:** plain TensorFlow/Keras `MobileNetV3Small` transfer
  learning → TFLite (float16 + INT8). Works cleanly on arm64.
- **Object detector:** **Ultralytics YOLOv8-nano** for training. Chosen over
  MediaPipe Model Maker for dependable arm64 support.
- **Detector → TFLite:** the default Ultralytics `export(format="tflite")`
  (LiteRT / `ai-edge-torch`) **failed** against the installed torch 2.13 with
  `cannot import name 'get_cuda_generator_meta_val'` — the LiteRT export path
  lags very new torch. The working path is **YOLO → ONNX → `onnx2tf` → TFLite**
  (float32), which succeeded.

## Consequences

- Exercised end-to-end on synthetic smoke data: scene classifier exports and
  verifies as fp16 (1.9 MB) and INT8 (1.2 MB); the detector trains (reports
  mAP@0.5) and exports to a float32 TFLite (~12 MB) that loads and runs.
- The detector TFLite input is `[1,160,160,3]` float32 and output is
  `[1,12,525]` (4 bbox + 8 class scores × 525 anchors). This is **decoded
  on-device** (transpose, score threshold, NMS); the [ADR-006](ADR-006-elevator-safety-threshold.md)
  elevator threshold is applied in that decoder/fusion, not in the model.
- The float16 TFLite has float16 I/O that the reference CPU interpreter rejects
  (`CONV_2D` prepare fails); the float32 export is used for the app.
- Reproducibility gotchas (captured in `ml_training/README.md`): set
  `SSL_CERT_FILE` to `certifi`'s bundle for weight downloads on macOS Python,
  and the `onnx2tf` toolchain install perturbs the torch/torchvision pins — keep
  detector training and TFLite conversion in separate, pinned environments if
  re-running.

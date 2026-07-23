# ADR-009: Object-detector training toolchain (Model Maker is deprecated)

- Status: Accepted
- Date: 2026-07-23

## Context

The original spec names **TFLite Model Maker** to fine-tune the object
detector. TFLite Model Maker is deprecated and effectively unmaintained: it
pins old TensorFlow and Python (≈3.9) versions and does not install cleanly on
current Python (3.11+) or Apple Silicon (arm64), which is the development
environment here.

## Decision

- **Scene classifier:** plain TensorFlow/Keras `MobileNetV3Small` transfer
  learning → TFLite (float16 + INT8). This path is well-supported on arm64 and
  needs no deprecated tooling.
- **Object detector:** prefer **MediaPipe Model Maker**, which produces a
  metadata-rich TFLite model consumable directly by the MediaPipe Tasks
  `ObjectDetector` on Android (minimal glue). If MediaPipe Model Maker will not
  install/train reliably on arm64, fall back to **Ultralytics YOLOv8-nano** and
  export to TFLite, decoding the output tensors manually on-device.

The final choice is made at training time based on what installs and trains
reliably, and recorded here.

## Consequences

- Avoids sinking time into a dead toolchain.
- The Android integration path depends on the choice: MediaPipe Tasks
  (metadata, near-zero decode code) vs a raw TFLite interpreter with custom
  YOLO output decoding. Whichever is used, the safety threshold from
  [ADR-006](ADR-006-elevator-safety-threshold.md) is applied identically in
  fusion.
- Training runs in a dedicated Python 3.12 venv (system Python 3.14 is too new
  for TensorFlow).

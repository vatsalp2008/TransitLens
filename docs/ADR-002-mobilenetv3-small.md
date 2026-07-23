# ADR-002: MobileNetV3-Small for scene classification

- Status: Accepted
- Date: 2026-07-23

## Context

The scene classifier solves a 6-class problem (bus_stop, train_platform,
street_corner, vehicle_interior, transfer_hub, unknown) and must run every
frame on mid-range hardware (Pixel 6 / Galaxy S21) inside a < 50 ms budget so
guidance keeps up with a walking user.

## Decision

Fine-tune MobileNetV3-Small (ImageNet-pretrained) via transfer learning, then
export to TFLite with float16 and INT8 post-training quantization.

## Consequences

- ~4 MB quantized model, comfortably inside the latency budget with the GPU
  delegate / NNAPI.
- A 6-class problem does not benefit meaningfully from a larger backbone;
  larger models add latency without accuracy that changes rider outcomes.
- Target top-1 accuracy is ~85% on the validation set. If real-world accuracy
  is insufficient, the fallback is MobileNetV3-Large or EfficientNet-Lite,
  re-benchmarked against the same latency budget.
- `unknown` (confidence < 0.6) is a first-class outcome that falls back to GPS,
  so a low-confidence classification never produces a confident wrong action.

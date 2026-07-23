# ADR-001: On-device ML over a cloud inference API

- Status: Accepted
- Date: 2026-07-23

## Context

TransitLens runs a camera-driven perception loop (scene + objects + text) to
guide riders in real time. The users are people with visual, cognitive, or
language barriers, often on limited data plans, frequently underground or in
areas with poor connectivity.

## Decision

Run the scene classifier and object detector on-device with TensorFlow Lite,
and text recognition with ML Kit (on-device). The network is used only for
GTFS-RT arrival predictions, which degrade gracefully to cached static
schedules when offline.

## Consequences

- Core navigation works with no connectivity and no data usage.
- No cloud round-trip, so per-frame guidance latency is bounded by on-device
  inference (target < 50 ms scene, < 35 ms detection) rather than network RTT.
- The camera feed never leaves the device — a strong privacy property for a
  vision app pointed at the user's surroundings and strangers.
- We accept a model-size/latency budget (small quantized models) and give up
  the accuracy ceiling of large server models. See [ADR-002](ADR-002-mobilenetv3-small.md).
- No server to run or pay for, which matters for a solo, zero-budget project.

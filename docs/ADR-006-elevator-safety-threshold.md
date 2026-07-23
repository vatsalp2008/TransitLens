# ADR-006: Higher confidence threshold for elevator detection

- Status: Accepted
- Date: 2026-07-23

## Context

The object detector's default acceptance threshold is 0.65. For most classes a
false positive is harmless (a spurious "crosswalk" is ignored downstream). The
`elevator_door` class is different: telling a visually impaired user an elevator
is present/open when it is not can lead them toward an open shaft or a fall.
The cost of a false positive here is physical injury; the cost of a false
negative is a missed cue the user can recover from.

## Decision

Use per-class thresholds. `ELEVATOR_DOOR` requires confidence ≥ 0.80; all other
classes use the 0.65 default. This is enforced centrally in `DetectionThresholds`
and applied in `ContextFusionEngine.fuse()`, which filters detections *before*
any guidance is derived. As a result a sub-threshold elevator can never reach
`deriveAction()` and can never trigger a `SEEK_ELEVATOR` cue.

## Consequences

- We deliberately trade more false negatives (occasionally missing a real
  elevator) for far fewer dangerous false positives.
- The rule is codified once and covered by tests: `DetectionThresholdsTest`
  (threshold values and pass/reject at the boundary) and `ContextFusionTest`
  (a 0.70 elevator never yields `SEEK_ELEVATOR`, even for a rider who requires
  an elevator; a 0.85 elevator does).
- If field data shows the elevator model is well-calibrated, the threshold can
  be revisited — but only with evidence, and the safety-biased default stays.

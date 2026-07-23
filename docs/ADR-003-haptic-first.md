# ADR-003: Haptic-first guidance over voice-first

- Status: Accepted
- Date: 2026-07-23

## Context

Guidance must reach the user in loud, chaotic urban transit environments. Voice
is unreliable there, requires earphones (which block environmental awareness the
user needs) or a speaker (which broadcasts the user's disability and
destination), and excludes users with hearing impairments.

## Decision

Make haptics the primary guidance channel with a designed vibration vocabulary
(see `HapticPattern`), and always provide an audio equivalent that the user can
enable. Every haptic pattern has an audio counterpart; the user can run
audio-only if they prefer.

## Consequences

- An 11-pattern haptic language (turn, board, alight, wait, cross, elevator,
  arrived, alert, recalculating) that is learnable and mutually distinguishable
  — validated by `HapticPatternCatalogTest`.
- A single-motor phone cannot render spatial left/right, so TURN_LEFT and
  TURN_RIGHT are distinguished temporally (short-short-long vs long-short-short)
  rather than spatially.
- Amplitude is auto-calibrated to ambient noise (louder outdoors, gentler
  indoors) using the microphone level.
- Accessibility requirement: no meaning is conveyed by haptics alone; audio
  parity is mandatory.

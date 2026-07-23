# ADR-008: Split pure-Kotlin `:core` from the Android `:app`

- Status: Accepted
- Date: 2026-07-23

## Context

The algorithmically interesting, compliance-critical logic (accessibility
router, context fusion, haptic vocabulary, GTFS parsing, text entity
extraction) has no inherent dependency on Android. Building it inside an
`com.android.application` module would (a) require the Android SDK just to
compile and unit-test it, and (b) couple business logic to the framework.

## Decision

Two Gradle modules:

- **`:core`** — a plain Kotlin/JVM library (`org.jetbrains.kotlin.jvm`) holding
  domain models, `AccessibilityRouter`, `ContextFusionEngine`,
  `HapticPattern`/catalog, `GtfsStaticParser`, and `TransitTextExtractor`.
- **`:app`** — the `com.android.application` module (camera, TFLite/ML Kit,
  Compose, Room, Hilt, vibration, TTS) that depends on `:core`.

## Consequences

- `:core` compiles and unit-tests with only a JDK — no Android SDK, no
  emulator, no device. The full suite (36 tests) runs in ~1 s via
  `./gradlew :core:test`, which is what made a verified first phase possible
  before any Android toolchain was installed.
- Business logic stays framework-free and portable; the Android layer is a thin
  adapter over `:core`.
- Slight overhead of a multi-module build and mapping `:core` types at the
  Android boundary — a worthwhile trade for the testability and isolation.

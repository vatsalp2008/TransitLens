# ADR-007: Jetpack Compose over XML layouts

- Status: Accepted
- Date: 2026-07-23

## Context

TransitLens is accessibility-first: TalkBack support, custom semantics for a
camera viewfinder, announced navigation actions, and font/contrast scaling are
core requirements, not add-ons.

## Decision

Build the UI in Jetpack Compose.

## Consequences

- Accessibility semantics are first-class (`Modifier.semantics`,
  `contentDescription`), which makes TalkBack integration and custom
  announcements cleaner than the XML + accessibility-delegate approach.
- Declarative UI speeds iteration for a solo developer.
- Compose is the current Android standard, so the project reflects modern
  practice for a portfolio.
- Requires Kotlin 2.0's built-in Compose compiler plugin (pinned in the version
  catalog) and the Android SDK to build the `:app` module.

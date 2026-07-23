# ADR-004: GTFS over a proprietary transit API

- Status: Accepted
- Date: 2026-07-23

## Context

TransitLens needs stop/route/schedule data and live arrivals. Proprietary
agency APIs vary per city, often require keys, and lock the project to one
region.

## Decision

Use GTFS static (routes, stops, trips, stop_times) as the schedule source and
GTFS-RT for live arrivals. Seattle / King County Metro is the first target
feed, accessed via OneBusAway for real-time.

## Consequences

- GTFS is the open standard used by essentially every major transit agency, so
  the project ports to any GTFS city by swapping the feed.
- Static data needs no API key; only GTFS-RT (OneBusAway) requires a free key,
  and it is optional — the app falls back to cached static schedules offline.
- GTFS parsing lives in pure-Kotlin `:core` (`GtfsStaticParser`) so it is
  unit-tested and reused by the Android Room cache.

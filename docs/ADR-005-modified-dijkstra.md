# ADR-005: Hand-rolled constraint-weighted Dijkstra over an off-the-shelf router

- Status: Accepted
- Date: 2026-07-23

## Context

The core value of TransitLens is routing that respects a rider's physical and
cognitive constraints: avoid stairs, require an elevator, avoid steep grades,
cap walking distance, minimize transfers, require wheelchair-accessible edges.
Off-the-shelf routers (OSRM, Valhalla, Google Directions) do not model these as
first-class, edge-level constraints — e.g. none natively enforce "this transfer
is only usable if it has an elevator."

## Decision

Implement a constraint-weighted Dijkstra in pure Kotlin (`AccessibilityRouter`
in `:core`). A rider's `ConstraintProfile` reshapes the graph before search:

- hard exclusions set edge weight to +infinity (stairs when avoided;
  non-wheelchair edges for wheelchair users; walking legs over the distance cap);
- soft penalties multiply/add weight (steep grades ×3; transfers get a penalty
  that doubles under cognitive simplification).

## Consequences

- Full control over the exact accessibility semantics, and they are unit-tested
  per constraint (`AccessibilityRouterTest`).
- We own graph construction: the router operates on a `TransitGraph` fused from
  GTFS + OpenStreetMap pedestrian data (built in a later phase).
- A `NavigationPlan` carries an accessibility compliance score alongside the
  route, so the UI can communicate how well a route matched the constraints.
- We forgo the map-matching / large-network optimizations of mature routers;
  acceptable because journeys are short and the graph is bounded per query.

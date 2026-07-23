package com.vatsalp.transitlens.core.routing

import com.vatsalp.transitlens.core.model.ConstraintProfile
import com.vatsalp.transitlens.core.model.HapticPattern
import com.vatsalp.transitlens.core.model.LatLng
import com.vatsalp.transitlens.core.model.NavigationPlan
import com.vatsalp.transitlens.core.model.NavigationStep
import com.vatsalp.transitlens.core.model.StepAction
import com.vatsalp.transitlens.core.model.TransitEdge
import com.vatsalp.transitlens.core.model.TransitGraph
import com.vatsalp.transitlens.core.model.TransitMode
import java.util.PriorityQueue

/**
 * Constraint-weighted shortest-path router over a fused GTFS + pedestrian graph.
 *
 * A plain shortest-time Dijkstra is modified so that a rider's [ConstraintProfile]
 * reshapes the graph: some edges become impassable (weight = +infinity), others
 * are penalized. No off-the-shelf routing library models accessibility
 * constraints at the edge level, which is why this is hand-rolled
 * (see docs/ADR-005-modified-dijkstra.md).
 */
class AccessibilityRouter(
    private val transferPenaltySeconds: Int = 180,
) {
    /** Route between two graph node ids. Returns null if the destination is unreachable. */
    fun route(
        originNodeId: String,
        destinationNodeId: String,
        constraints: ConstraintProfile,
        graph: TransitGraph,
    ): NavigationPlan? {
        if (graph.node(originNodeId) == null || graph.node(destinationNodeId) == null) return null
        if (originNodeId == destinationNodeId) {
            return NavigationPlan(emptyList(), 0, 0, 1f)
        }

        val dist = HashMap<String, Double>().apply { put(originNodeId, 0.0) }
        val prevEdge = HashMap<String, TransitEdge>()
        val settled = HashSet<String>()
        val frontier = PriorityQueue<Pair<String, Double>>(compareBy { it.second })
        frontier.add(originNodeId to 0.0)

        while (frontier.isNotEmpty()) {
            val (u, d) = frontier.poll()
            if (!settled.add(u)) continue
            if (u == destinationNodeId) break
            for (edge in graph.edgesFrom(u)) {
                val w = edgeWeight(edge, constraints)
                if (w.isInfinite()) continue
                val candidate = d + w
                if (candidate < (dist[edge.to] ?: Double.POSITIVE_INFINITY)) {
                    dist[edge.to] = candidate
                    prevEdge[edge.to] = edge
                    frontier.add(edge.to to candidate)
                }
            }
        }

        if (destinationNodeId !in dist) return null

        val pathEdges = ArrayList<TransitEdge>()
        var cursor = destinationNodeId
        while (cursor != originNodeId) {
            val edge = prevEdge[cursor] ?: return null
            pathEdges.add(edge)
            cursor = edge.from
        }
        pathEdges.reverse()
        return buildPlan(pathEdges, graph, constraints)
    }

    /** Route between coordinates by snapping to the nearest graph node. */
    fun route(
        origin: LatLng,
        destination: LatLng,
        constraints: ConstraintProfile,
        graph: TransitGraph,
    ): NavigationPlan? {
        val o = graph.nearestNode(origin) ?: return null
        val d = graph.nearestNode(destination) ?: return null
        return route(o.id, d.id, constraints, graph)
    }

    /**
     * Constraint-weighted edge cost. Returns [Double.POSITIVE_INFINITY] for edges
     * a rider cannot use, so Dijkstra routes around them entirely.
     */
    internal fun edgeWeight(edge: TransitEdge, c: ConstraintProfile): Double {
        // Hard exclusions.
        if (edge.requiresStairs && (c.avoidStairs || c.requireElevator)) return Double.POSITIVE_INFINITY
        if (c.wheelchairAccessible && !edge.wheelchairAccessible) return Double.POSITIVE_INFINITY
        if (edge.mode == TransitMode.WALK && edge.distanceMeters > c.maxWalkingMeters) {
            return Double.POSITIVE_INFINITY
        }

        // Soft penalties.
        var weight = edge.durationSeconds.toDouble()
        if (edge.hillGrade > c.avoidHillsAboveGrade) weight *= 3.0
        if (edge.mode == TransitMode.TRANSFER) {
            weight += if (c.cognitiveSimplification) {
                transferPenaltySeconds * 2.0
            } else {
                transferPenaltySeconds.toDouble()
            }
        }
        return weight
    }

    private fun buildPlan(
        pathEdges: List<TransitEdge>,
        graph: TransitGraph,
        constraints: ConstraintProfile,
    ): NavigationPlan {
        val steps = ArrayList<NavigationStep>()
        var previousMode: TransitMode? = null

        for ((i, edge) in pathEdges.withIndex()) {
            val toNode = graph.node(edge.to)
            val landmark = toNode?.name ?: edge.to
            when (edge.mode) {
                TransitMode.WALK -> steps.add(
                    NavigationStep(
                        action = StepAction.WALK,
                        instruction = "Walk ${edge.distanceMeters} meters toward $landmark",
                        landmarkAnchor = landmark,
                        hapticPattern = HapticPattern.CONTINUE,
                        durationSeconds = edge.durationSeconds,
                        distanceMeters = edge.distanceMeters,
                    ),
                )

                TransitMode.BUS, TransitMode.TRAIN -> {
                    if (previousMode != edge.mode) {
                        val route = edge.routeShortName ?: edge.routeId ?: "the vehicle"
                        steps.add(
                            NavigationStep(
                                action = StepAction.BOARD,
                                instruction = "Board $route",
                                landmarkAnchor = landmark,
                                hapticPattern = HapticPattern.BOARD_NOW,
                                routeShortName = edge.routeShortName,
                            ),
                        )
                    }
                    steps.add(
                        NavigationStep(
                            action = StepAction.RIDE,
                            instruction = "Ride to $landmark",
                            landmarkAnchor = landmark,
                            hapticPattern = HapticPattern.CONTINUE,
                            durationSeconds = edge.durationSeconds,
                            distanceMeters = edge.distanceMeters,
                            routeShortName = edge.routeShortName,
                        ),
                    )
                    val next = pathEdges.getOrNull(i + 1)
                    if (next == null || next.mode != edge.mode) {
                        steps.add(
                            NavigationStep(
                                action = StepAction.ALIGHT,
                                instruction = "Get off at $landmark",
                                landmarkAnchor = landmark,
                                hapticPattern = HapticPattern.ALIGHT_NOW,
                                routeShortName = edge.routeShortName,
                            ),
                        )
                    }
                }

                TransitMode.TRANSFER -> steps.add(
                    NavigationStep(
                        action = StepAction.TRANSFER,
                        instruction = "Transfer at $landmark",
                        landmarkAnchor = landmark,
                        hapticPattern = HapticPattern.CONTINUE,
                        durationSeconds = edge.durationSeconds,
                    ),
                )
            }
            previousMode = edge.mode
        }

        val destinationName = graph.node(pathEdges.last().to)?.name ?: "your destination"
        steps.add(
            NavigationStep(
                action = StepAction.ARRIVE,
                instruction = "You have arrived at $destinationName",
                landmarkAnchor = destinationName,
                hapticPattern = HapticPattern.ARRIVED,
            ),
        )

        return NavigationPlan(
            steps = steps,
            totalDurationSeconds = pathEdges.sumOf { it.durationSeconds },
            totalDistanceMeters = pathEdges.sumOf { it.distanceMeters },
            accessibilityComplianceScore = complianceScore(pathEdges, constraints),
        )
    }

    /** Fraction of path edges that fully satisfy the active constraints (no penalty incurred). */
    private fun complianceScore(pathEdges: List<TransitEdge>, c: ConstraintProfile): Float {
        if (pathEdges.isEmpty()) return 1f
        val compliant = pathEdges.count { edge ->
            val hillOk = edge.hillGrade <= c.avoidHillsAboveGrade
            val wheelOk = !c.wheelchairAccessible || edge.wheelchairAccessible
            val stairsOk = !(edge.requiresStairs && (c.avoidStairs || c.requireElevator))
            hillOk && wheelOk && stairsOk
        }
        return compliant.toFloat() / pathEdges.size
    }
}

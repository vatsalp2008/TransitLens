package com.vatsalp.transitlens.core.routing

import com.vatsalp.transitlens.core.model.TransitEdge
import com.vatsalp.transitlens.core.model.TransitGraph
import com.vatsalp.transitlens.core.model.TransitMode
import com.vatsalp.transitlens.core.model.TransitNode
import com.vatsalp.transitlens.core.model.haversineMeters

/**
 * Builds a routable pedestrian graph from transit stops by connecting any two
 * nodes within [maxEdgeMeters] with a bidirectional WALK edge. This yields a
 * usable graph from GTFS stops alone; OSM enrichment (incline, elevators, curb
 * cuts) refines edge attributes in a later step.
 */
object GraphBuilder {

    fun buildWalkingGraph(
        nodes: List<TransitNode>,
        maxEdgeMeters: Int = 400,
        walkSpeedMetersPerSecond: Double = 1.3,
    ): TransitGraph {
        val edges = ArrayList<TransitEdge>()
        for (i in nodes.indices) {
            for (j in nodes.indices) {
                if (i == j) continue
                val a = nodes[i]
                val b = nodes[j]
                val meters = haversineMeters(a.latLng, b.latLng)
                if (meters > maxEdgeMeters) continue
                edges.add(
                    TransitEdge(
                        from = a.id,
                        to = b.id,
                        mode = TransitMode.WALK,
                        durationSeconds = (meters / walkSpeedMetersPerSecond).toInt(),
                        distanceMeters = meters.toInt(),
                        hillGrade = maxOf(a.hillGrade, b.hillGrade),
                        wheelchairAccessible = a.hasWheelchairAccess && b.hasWheelchairAccess,
                    ),
                )
            }
        }
        return TransitGraph(nodes, edges)
    }
}

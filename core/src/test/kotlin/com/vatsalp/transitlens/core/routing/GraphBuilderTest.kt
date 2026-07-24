package com.vatsalp.transitlens.core.routing

import com.vatsalp.transitlens.core.model.ConstraintProfile
import com.vatsalp.transitlens.core.model.NodeType
import com.vatsalp.transitlens.core.model.TransitNode
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GraphBuilderTest {

    private fun stop(id: String, lat: Double, lon: Double, wheelchair: Boolean = true) =
        TransitNode(id, NodeType.STOP, lat, lon, hasWheelchairAccess = wheelchair, name = id)

    @Test
    fun connectsNearbyStopsAndLeavesDistantOnesIsolated() {
        val a = stop("A", 47.6100, -122.3300)
        val b = stop("B", 47.6110, -122.3300) // ~111 m north of A
        val c = stop("C", 47.6300, -122.3300) // ~2.2 km north of A

        val graph = GraphBuilder.buildWalkingGraph(listOf(a, b, c), maxEdgeMeters = 400)

        assertTrue(graph.edgesFrom("A").any { it.to == "B" })
        assertTrue(graph.edgesFrom("B").any { it.to == "A" })
        assertTrue(graph.edgesFrom("C").isEmpty())

        val plan = AccessibilityRouter().route("A", "B", ConstraintProfile.NONE, graph)
        assertNotNull(plan)
    }

    @Test
    fun walkEdgeIsWheelchairAccessibleOnlyIfBothStopsAre() {
        val a = stop("A", 47.6100, -122.3300, wheelchair = true)
        val b = stop("B", 47.6110, -122.3300, wheelchair = false)
        val graph = GraphBuilder.buildWalkingGraph(listOf(a, b), maxEdgeMeters = 400)

        val edge = graph.edgesFrom("A").first { it.to == "B" }
        assertTrue(!edge.wheelchairAccessible)

        // A wheelchair user cannot use the A<->B leg, so B is unreachable.
        val plan = AccessibilityRouter().route("A", "B", ConstraintProfile(wheelchairAccessible = true), graph)
        assertTrue(plan == null)
    }
}

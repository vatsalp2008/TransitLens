package com.vatsalp.transitlens.core.routing

import com.vatsalp.transitlens.core.model.ConstraintProfile
import com.vatsalp.transitlens.core.model.NodeType
import com.vatsalp.transitlens.core.model.StepAction
import com.vatsalp.transitlens.core.model.TransitEdge
import com.vatsalp.transitlens.core.model.TransitGraph
import com.vatsalp.transitlens.core.model.TransitMode
import com.vatsalp.transitlens.core.model.TransitNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AccessibilityRouterTest {

    private val router = AccessibilityRouter()

    private fun node(id: String) = TransitNode(id, NodeType.STOP, 0.0, 0.0, name = id)

    /** A -> C -> D is faster by time; A -> B -> D is the accessible detour. */
    private fun diamond(
        acStairs: Boolean = false,
        acWheelchair: Boolean = true,
        acHill: Float = 0f,
    ) = TransitGraph(
        nodes = listOf("A", "B", "C", "D").map(::node),
        edges = listOf(
            TransitEdge("A", "C", TransitMode.WALK, 30, 50, requiresStairs = acStairs, hillGrade = acHill, wheelchairAccessible = acWheelchair),
            TransitEdge("C", "D", TransitMode.WALK, 30, 50),
            TransitEdge("A", "B", TransitMode.WALK, 50, 90),
            TransitEdge("B", "D", TransitMode.WALK, 50, 90),
        ),
    )

    @Test
    fun findsFastestPathWithNoConstraints() {
        val plan = assertNotNull(router.route("A", "D", ConstraintProfile.NONE, diamond()))
        assertEquals(60, plan.totalDurationSeconds) // A -> C -> D
    }

    @Test
    fun avoidStairsRoutesAroundStairEdge() {
        val plan = assertNotNull(router.route("A", "D", ConstraintProfile(avoidStairs = true), diamond(acStairs = true)))
        assertEquals(100, plan.totalDurationSeconds) // A -> B -> D
    }

    @Test
    fun requireElevatorAlsoExcludesStairs() {
        val plan = assertNotNull(router.route("A", "D", ConstraintProfile(requireElevator = true), diamond(acStairs = true)))
        assertEquals(100, plan.totalDurationSeconds)
    }

    @Test
    fun wheelchairInaccessibleEdgeExcluded() {
        val plan = assertNotNull(router.route("A", "D", ConstraintProfile(wheelchairAccessible = true), diamond(acWheelchair = false)))
        assertEquals(100, plan.totalDurationSeconds)
        assertEquals(1f, plan.accessibilityComplianceScore)
    }

    @Test
    fun steepHillIsPenalizedNotForbidden() {
        val graph = diamond(acHill = 0.10f)
        val flat = assertNotNull(router.route("A", "D", ConstraintProfile.NONE, graph))
        assertEquals(60, flat.totalDurationSeconds) // hill ignored when no restriction

        val avoid = assertNotNull(router.route("A", "D", ConstraintProfile(avoidHillsAboveGrade = 0.05f), graph))
        assertEquals(100, avoid.totalDurationSeconds) // penalized hill makes detour cheaper
    }

    @Test
    fun cognitiveSimplificationPrefersFewerTransfers() {
        val graph = TransitGraph(
            nodes = listOf("A", "B", "B2", "E", "E2", "F", "F2", "D").map(::node),
            edges = listOf(
                // 1-transfer route (longer ride time)
                TransitEdge("A", "B", TransitMode.BUS, 300, 0, routeShortName = "10"),
                TransitEdge("B", "B2", TransitMode.TRANSFER, 60, 0),
                TransitEdge("B2", "D", TransitMode.BUS, 300, 0, routeShortName = "20"),
                // 2-transfer route (shorter ride time)
                TransitEdge("A", "E", TransitMode.BUS, 200, 0, routeShortName = "30"),
                TransitEdge("E", "E2", TransitMode.TRANSFER, 30, 0),
                TransitEdge("E2", "F", TransitMode.BUS, 100, 0, routeShortName = "40"),
                TransitEdge("F", "F2", TransitMode.TRANSFER, 30, 0),
                TransitEdge("F2", "D", TransitMode.BUS, 100, 0, routeShortName = "50"),
            ),
        )

        val normal = assertNotNull(router.route("A", "D", ConstraintProfile.NONE, graph))
        assertEquals(2, normal.steps.count { it.action == StepAction.TRANSFER })

        val simple = assertNotNull(router.route("A", "D", ConstraintProfile(cognitiveSimplification = true), graph))
        assertEquals(1, simple.steps.count { it.action == StepAction.TRANSFER })
    }

    @Test
    fun maxWalkingMetersExcludesLongWalkingLegs() {
        val graph = TransitGraph(
            nodes = listOf("A", "B", "C").map(::node),
            edges = listOf(
                TransitEdge("A", "B", TransitMode.WALK, 100, 600), // fast but 600 m
                TransitEdge("A", "C", TransitMode.WALK, 90, 100),
                TransitEdge("C", "B", TransitMode.WALK, 90, 100),
            ),
        )
        val limited = assertNotNull(router.route("A", "B", ConstraintProfile(maxWalkingMeters = 500), graph))
        assertEquals(180, limited.totalDurationSeconds) // A -> C -> B

        val relaxed = assertNotNull(router.route("A", "B", ConstraintProfile(maxWalkingMeters = 1000), graph))
        assertEquals(100, relaxed.totalDurationSeconds) // direct A -> B allowed
    }

    @Test
    fun busTripProducesBoardRideAlightArrive() {
        val graph = TransitGraph(
            nodes = listOf("A", "S1", "S2").map(::node),
            edges = listOf(
                TransitEdge("A", "S1", TransitMode.WALK, 60, 100),
                TransitEdge("S1", "S2", TransitMode.BUS, 300, 2000, routeShortName = "49"),
            ),
        )
        val plan = assertNotNull(router.route("A", "S2", ConstraintProfile.NONE, graph))
        assertEquals(
            listOf(StepAction.WALK, StepAction.BOARD, StepAction.RIDE, StepAction.ALIGHT, StepAction.ARRIVE),
            plan.steps.map { it.action },
        )
        assertEquals("49", plan.steps.first { it.action == StepAction.BOARD }.routeShortName)
    }

    @Test
    fun unreachableDestinationReturnsNull() {
        val graph = TransitGraph(listOf(node("A"), node("Z")), emptyList())
        assertNull(router.route("A", "Z", ConstraintProfile.NONE, graph))
    }

    @Test
    fun unknownNodeReturnsNull() {
        val graph = TransitGraph(listOf(node("A")), emptyList())
        assertNull(router.route("A", "X", ConstraintProfile.NONE, graph))
        assertNull(router.route("X", "A", ConstraintProfile.NONE, graph))
    }
}

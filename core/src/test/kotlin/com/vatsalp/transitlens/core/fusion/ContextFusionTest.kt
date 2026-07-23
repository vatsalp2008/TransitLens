package com.vatsalp.transitlens.core.fusion

import com.vatsalp.transitlens.core.model.ConstraintProfile
import com.vatsalp.transitlens.core.model.Detection
import com.vatsalp.transitlens.core.model.DetectedObject
import com.vatsalp.transitlens.core.model.GuidanceAction
import com.vatsalp.transitlens.core.model.HapticPattern
import com.vatsalp.transitlens.core.model.NavPhase
import com.vatsalp.transitlens.core.model.NavState
import com.vatsalp.transitlens.core.model.NavigationPlan
import com.vatsalp.transitlens.core.model.NavigationStep
import com.vatsalp.transitlens.core.model.SceneClass
import com.vatsalp.transitlens.core.model.SceneClassification
import com.vatsalp.transitlens.core.model.StepAction
import com.vatsalp.transitlens.core.model.TransitTextContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ContextFusionTest {

    private val engine = ContextFusionEngine()

    private fun ctx(
        scene: SceneClassification,
        objects: List<Detection> = emptyList(),
        text: TransitTextContext = TransitTextContext.EMPTY,
        navState: NavState = NavState(),
        constraints: ConstraintProfile = ConstraintProfile.NONE,
    ) = engine.fuse(scene, objects, text, navState, constraints)

    private fun boardPlan(route: String) = NavigationPlan(
        steps = listOf(
            NavigationStep(StepAction.BOARD, "Board $route", "the stop", HapticPattern.BOARD_NOW, routeShortName = route),
        ),
        totalDurationSeconds = 0,
        totalDistanceMeters = 0,
        accessibilityComplianceScore = 1f,
    )

    @Test
    fun lowConfidenceSceneCollapsesToUnknown() {
        assertEquals(SceneClass.UNKNOWN, ctx(SceneClassification(SceneClass.BUS_STOP, 0.4f)).scene.sceneClass)
    }

    @Test
    fun highConfidenceScenePreserved() {
        assertEquals(SceneClass.BUS_STOP, ctx(SceneClassification(SceneClass.BUS_STOP, 0.9f)).scene.sceneClass)
    }

    @Test
    fun lowConfidenceElevatorIsFilteredOut() {
        val fused = ctx(
            SceneClassification(SceneClass.TRANSFER_HUB, 0.9f),
            objects = listOf(Detection(DetectedObject.ELEVATOR_DOOR, 0.7f)),
        )
        assertTrue(fused.detectedObjects.isEmpty())
    }

    @Test
    fun lowConfidenceElevatorNeverTriggersElevatorGuidance() {
        // A 0.70 elevator is below the 0.80 safety threshold; even for a rider who
        // requires an elevator, guidance must not say "seek elevator".
        val fused = ctx(
            SceneClassification(SceneClass.TRANSFER_HUB, 0.9f),
            objects = listOf(Detection(DetectedObject.ELEVATOR_DOOR, 0.7f)),
            constraints = ConstraintProfile(requireElevator = true),
        )
        assertNotEquals(GuidanceAction.SEEK_ELEVATOR, engine.deriveAction(fused))
    }

    @Test
    fun confidentElevatorTriggersGuidanceWhenNeeded() {
        val fused = ctx(
            SceneClassification(SceneClass.TRANSFER_HUB, 0.9f),
            objects = listOf(Detection(DetectedObject.ELEVATOR_DOOR, 0.85f)),
            constraints = ConstraintProfile(requireElevator = true),
        )
        assertEquals(GuidanceAction.SEEK_ELEVATOR, engine.deriveAction(fused))
    }

    @Test
    fun boardsWhenSceneAndRouteMatchPlan() {
        val fused = ctx(
            SceneClassification(SceneClass.BUS_STOP, 0.9f),
            text = TransitTextContext(routeNumbers = listOf("49")),
            navState = NavState(boardPlan("49"), 0, NavPhase.WAITING_TO_BOARD),
        )
        assertEquals(GuidanceAction.BOARD, engine.deriveAction(fused))
    }

    @Test
    fun waitsWhenVisibleRouteIsNotThePlannedOne() {
        val fused = ctx(
            SceneClassification(SceneClass.BUS_STOP, 0.9f),
            text = TransitTextContext(routeNumbers = listOf("8")),
            navState = NavState(boardPlan("49"), 0, NavPhase.WAITING_TO_BOARD),
        )
        assertEquals(GuidanceAction.WAIT, engine.deriveAction(fused))
    }

    @Test
    fun crosswalkNeverAutoAdvancesToCrossNow() {
        val fused = ctx(
            SceneClassification(SceneClass.STREET_CORNER, 0.9f),
            objects = listOf(Detection(DetectedObject.CROSSWALK_MARKING, 0.9f)),
        )
        assertEquals(GuidanceAction.CROSS_WAIT, engine.deriveAction(fused))
    }
}

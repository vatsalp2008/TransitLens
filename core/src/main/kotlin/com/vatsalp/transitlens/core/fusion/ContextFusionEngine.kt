package com.vatsalp.transitlens.core.fusion

import com.vatsalp.transitlens.core.model.ActionContext
import com.vatsalp.transitlens.core.model.ConstraintProfile
import com.vatsalp.transitlens.core.model.Detection
import com.vatsalp.transitlens.core.model.DetectedObject
import com.vatsalp.transitlens.core.model.DetectionThresholds
import com.vatsalp.transitlens.core.model.GuidanceAction
import com.vatsalp.transitlens.core.model.NavPhase
import com.vatsalp.transitlens.core.model.NavState
import com.vatsalp.transitlens.core.model.SceneClass
import com.vatsalp.transitlens.core.model.SceneClassification
import com.vatsalp.transitlens.core.model.StepAction
import com.vatsalp.transitlens.core.model.TransitTextContext

/**
 * Rule-based fusion of the three ML outputs (scene, objects, text) with the
 * rider's plan and constraints into a single [ActionContext], and from there
 * into one concrete [GuidanceAction].
 *
 * Two safety rules are load-bearing:
 *  - detections are filtered through [DetectionThresholds], so a low-confidence
 *    elevator (< 0.80) never reaches [deriveAction] and can never trigger an
 *    "elevator ahead" cue.
 *  - a crossing cue is never escalated to CROSS_NOW from vision alone; without
 *    pedestrian-signal detection the safe default is CROSS_WAIT.
 */
class ContextFusionEngine(
    private val sceneConfidenceFloor: Float = 0.6f,
) {
    fun fuse(
        scene: SceneClassification,
        objects: List<Detection>,
        text: TransitTextContext,
        navState: NavState,
        constraints: ConstraintProfile,
        timestamp: Long = 0L,
    ): ActionContext {
        val effectiveScene =
            if (scene.confidence < sceneConfidenceFloor) {
                SceneClassification(SceneClass.UNKNOWN, scene.confidence)
            } else {
                scene
            }
        val validObjects = objects.filter { DetectionThresholds.passes(it) }
        return ActionContext(effectiveScene, validObjects, text, navState, constraints, timestamp)
    }

    fun deriveAction(ctx: ActionContext): GuidanceAction {
        val objects = ctx.detectedObjects // already threshold-filtered by fuse()
        val elevatorVisible = objects.any { it.label == DetectedObject.ELEVATOR_DOOR }
        val needsElevator = ctx.userConstraints.requireElevator || ctx.userConstraints.avoidStairs

        // Highest priority: guide toward an elevator when the rider needs one and one is visible.
        if (elevatorVisible && needsElevator) return GuidanceAction.SEEK_ELEVATOR

        return when (ctx.scene.sceneClass) {
            SceneClass.VEHICLE_INTERIOR ->
                if (ctx.navigationState.phase == NavPhase.ON_VEHICLE &&
                    ctx.navigationState.currentStep()?.action == StepAction.ALIGHT
                ) {
                    GuidanceAction.ALIGHT
                } else {
                    GuidanceAction.CONTINUE
                }

            SceneClass.BUS_STOP, SceneClass.TRAIN_PLATFORM -> {
                val step = ctx.navigationState.currentStep()
                val boardingRoute = step?.routeShortName
                val onCorrectRoute = boardingRoute != null &&
                    ctx.transitText.routeNumbers.any { it.equals(boardingRoute, ignoreCase = true) }
                if (step?.action == StepAction.BOARD && onCorrectRoute) {
                    GuidanceAction.BOARD
                } else {
                    GuidanceAction.WAIT
                }
            }

            SceneClass.STREET_CORNER ->
                // Safety-first: vision alone cannot confirm it is safe to cross.
                if (objects.any { it.label == DetectedObject.CROSSWALK_MARKING }) {
                    GuidanceAction.CROSS_WAIT
                } else {
                    GuidanceAction.CONTINUE
                }

            SceneClass.TRANSFER_HUB -> GuidanceAction.CONTINUE

            SceneClass.UNKNOWN ->
                if (ctx.navigationState.phase == NavPhase.WAITING_TO_BOARD) {
                    GuidanceAction.WAIT
                } else {
                    GuidanceAction.CONTINUE
                }
        }
    }
}

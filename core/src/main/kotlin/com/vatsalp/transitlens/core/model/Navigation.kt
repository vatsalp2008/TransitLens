package com.vatsalp.transitlens.core.model

/** Type of a step within a computed [NavigationPlan]. */
enum class StepAction { WALK, BOARD, RIDE, ALIGHT, TRANSFER, ARRIVE }

/** A real-time guidance cue derived from fused context, mapped to haptic + audio. */
enum class GuidanceAction {
    WAIT, BOARD, ALIGHT, TURN_LEFT, TURN_RIGHT, ARRIVED,
    SEEK_ELEVATOR, CROSS_NOW, CROSS_WAIT, CONTINUE, RECALCULATING, ALERT,
}

/** Where the rider currently is in their journey. */
enum class NavPhase { IDLE, WALKING, WAITING_TO_BOARD, ON_VEHICLE, TRANSFERRING, ARRIVED }

enum class PredictionConfidence { SCHEDULED, PREDICTED, REAL_TIME }

data class ArrivalPrediction(
    val routeId: String,
    val routeShortName: String,
    val headsign: String,
    val arrivalSeconds: Int,
    val isWheelchairAccessible: Boolean,
    val confidence: PredictionConfidence,
)

data class NavigationStep(
    val action: StepAction,
    val instruction: String,
    val landmarkAnchor: String,
    val hapticPattern: HapticPattern,
    val audioFile: String? = null,
    val durationSeconds: Int = 0,
    val distanceMeters: Int = 0,
    val gtfsArrival: ArrivalPrediction? = null,
    val routeShortName: String? = null,
)

data class NavigationPlan(
    val steps: List<NavigationStep>,
    val totalDurationSeconds: Int,
    val totalDistanceMeters: Int,
    val accessibilityComplianceScore: Float,
)

data class NavState(
    val plan: NavigationPlan? = null,
    val currentStepIndex: Int = 0,
    val phase: NavPhase = NavPhase.IDLE,
) {
    fun currentStep(): NavigationStep? = plan?.steps?.getOrNull(currentStepIndex)
    fun isFinalStep(): Boolean = plan != null && currentStepIndex >= plan.steps.lastIndex
}

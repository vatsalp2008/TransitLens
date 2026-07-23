package com.vatsalp.transitlens.core.model

/**
 * A rider's physical/cognitive routing constraints, captured during the
 * zero-text onboarding flow.
 *
 * [avoidHillsAboveGrade] is expressed as a grade fraction (0.05 == 5%).
 * The default of 1.0 means "no hill is too steep" — i.e. no restriction.
 */
data class ConstraintProfile(
    val avoidStairs: Boolean = false,
    val requireElevator: Boolean = false,
    val avoidHillsAboveGrade: Float = 1.0f,
    val maxWalkingMeters: Int = 500,
    val wheelchairAccessible: Boolean = false,
    val cognitiveSimplification: Boolean = false,
) {
    companion object {
        val NONE = ConstraintProfile()
    }
}

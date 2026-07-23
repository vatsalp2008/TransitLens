package com.vatsalp.transitlens.core.model

/**
 * The fused, single-frame understanding of the world produced by the
 * [com.vatsalp.transitlens.core.fusion.ContextFusionEngine]: what is happening
 * (scene + objects + text) combined with the rider's plan and constraints.
 */
data class ActionContext(
    val scene: SceneClassification,
    val detectedObjects: List<Detection>,
    val transitText: TransitTextContext,
    val navigationState: NavState,
    val userConstraints: ConstraintProfile,
    val timestamp: Long,
)

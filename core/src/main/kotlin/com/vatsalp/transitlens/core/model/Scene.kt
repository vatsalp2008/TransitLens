package com.vatsalp.transitlens.core.model

/** Transit scene classes produced by the on-device scene classifier (see spec Model 1). */
enum class SceneClass(val index: Int) {
    BUS_STOP(0),
    TRAIN_PLATFORM(1),
    STREET_CORNER(2),
    VEHICLE_INTERIOR(3),
    TRANSFER_HUB(4),
    UNKNOWN(5);

    companion object {
        fun fromIndex(i: Int): SceneClass = entries.firstOrNull { it.index == i } ?: UNKNOWN
    }
}

data class SceneClassification(
    val sceneClass: SceneClass,
    val confidence: Float,
)

/** Object classes produced by the on-device object detector (see spec Model 2). */
enum class DetectedObject {
    BUS,
    TRAIN_CAR,
    ELEVATOR_DOOR,
    ESCALATOR,
    CROSSWALK_MARKING,
    WHEELCHAIR_RAMP,
    TACTILE_PAVING,
    ACCESSIBILITY_SIGN,
    PEDESTRIAN,
}

/** Normalized [0,1] bounding box in image space. */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class Detection(
    val label: DetectedObject,
    val confidence: Float,
    val box: BoundingBox? = null,
)

/**
 * Per-class confidence thresholds for accepting a detection.
 *
 * ELEVATOR_DOOR uses a higher bar than the default: a false "elevator open"
 * has physical-safety consequences (a user could step into a shaft), so the
 * cost of a false positive is far higher than a false negative here.
 * See docs/ADR-006-elevator-safety-threshold.md.
 */
object DetectionThresholds {
    const val DEFAULT = 0.65f
    const val ELEVATOR_DOOR = 0.80f

    fun thresholdFor(label: DetectedObject): Float = when (label) {
        DetectedObject.ELEVATOR_DOOR -> ELEVATOR_DOOR
        else -> DEFAULT
    }

    fun passes(detection: Detection): Boolean =
        detection.confidence >= thresholdFor(detection.label)
}

package com.vatsalp.transitlens.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DetectionThresholdsTest {

    @Test
    fun elevatorDoorUsesHigherThresholdThanDefault() {
        assertEquals(0.80f, DetectionThresholds.thresholdFor(DetectedObject.ELEVATOR_DOOR))
        assertEquals(0.65f, DetectionThresholds.thresholdFor(DetectedObject.BUS))
    }

    @Test
    fun elevatorBelowSafetyThresholdIsRejected() {
        // Safety-critical: a false "elevator open" could lead to a fall.
        assertFalse(DetectionThresholds.passes(Detection(DetectedObject.ELEVATOR_DOOR, 0.79f)))
        assertTrue(DetectionThresholds.passes(Detection(DetectedObject.ELEVATOR_DOOR, 0.80f)))
        assertTrue(DetectionThresholds.passes(Detection(DetectedObject.ELEVATOR_DOOR, 0.95f)))
    }

    @Test
    fun defaultThresholdAppliesToOtherClasses() {
        assertFalse(DetectionThresholds.passes(Detection(DetectedObject.BUS, 0.64f)))
        assertTrue(DetectionThresholds.passes(Detection(DetectedObject.BUS, 0.65f)))
    }
}

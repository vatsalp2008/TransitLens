package com.vatsalp.transitlens.core.haptic

import com.vatsalp.transitlens.core.model.GuidanceAction
import com.vatsalp.transitlens.core.model.HapticPattern
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HapticPatternCatalogTest {

    @Test
    fun everyPatternHasWellFormedWaveform() {
        for (pattern in HapticPattern.entries) {
            assertTrue(pattern.timings.isNotEmpty(), "${pattern.name} has no timings")
            assertEquals(
                pattern.timings.size,
                pattern.amplitudes.size,
                "${pattern.name} timings/amplitudes length mismatch",
            )
            assertEquals(0, pattern.amplitudes.first(), "${pattern.name} must start with the motor off")
            assertTrue(pattern.timings.all { it >= 0 }, "${pattern.name} has a negative timing")
            assertTrue(pattern.amplitudes.all { it in 0..255 }, "${pattern.name} amplitude out of 0..255")
        }
    }

    @Test
    fun everyGuidanceActionMapsToAPattern() {
        for (action in GuidanceAction.entries) {
            HapticPatternCatalog.patternFor(action) // must not throw
        }
        assertEquals(HapticPattern.ELEVATOR_AHEAD, HapticPatternCatalog.patternFor(GuidanceAction.SEEK_ELEVATOR))
        assertEquals(HapticPattern.BOARD_NOW, HapticPatternCatalog.patternFor(GuidanceAction.BOARD))
        assertEquals(HapticPattern.ALIGHT_NOW, HapticPatternCatalog.patternFor(GuidanceAction.ALIGHT))
    }

    @Test
    fun leftAndRightTurnsAreDistinguishable() {
        assertTrue(!HapticPattern.TURN_LEFT.timings.contentEquals(HapticPattern.TURN_RIGHT.timings))
    }

    @Test
    fun waitPatternLoops() {
        assertEquals(0, HapticPattern.WAIT.repeat)
        assertEquals(-1, HapticPattern.CONTINUE.repeat)
    }
}

package com.vatsalp.transitlens.core.haptic

import com.vatsalp.transitlens.core.model.GuidanceAction
import com.vatsalp.transitlens.core.model.HapticPattern

/** Maps a real-time [GuidanceAction] to the [HapticPattern] that expresses it. */
object HapticPatternCatalog {

    fun patternFor(action: GuidanceAction): HapticPattern = when (action) {
        GuidanceAction.WAIT -> HapticPattern.WAIT
        GuidanceAction.BOARD -> HapticPattern.BOARD_NOW
        GuidanceAction.ALIGHT -> HapticPattern.ALIGHT_NOW
        GuidanceAction.TURN_LEFT -> HapticPattern.TURN_LEFT
        GuidanceAction.TURN_RIGHT -> HapticPattern.TURN_RIGHT
        GuidanceAction.ARRIVED -> HapticPattern.ARRIVED
        GuidanceAction.SEEK_ELEVATOR -> HapticPattern.ELEVATOR_AHEAD
        GuidanceAction.CROSS_NOW -> HapticPattern.CROSSING_SAFE
        GuidanceAction.CROSS_WAIT -> HapticPattern.WAIT
        GuidanceAction.CONTINUE -> HapticPattern.CONTINUE
        GuidanceAction.RECALCULATING -> HapticPattern.RECALCULATING
        GuidanceAction.ALERT -> HapticPattern.ALERT
    }

    val all: List<HapticPattern> get() = HapticPattern.entries.toList()
}

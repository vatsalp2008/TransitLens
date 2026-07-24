package com.vatsalp.transitlens.guidance

import com.vatsalp.transitlens.core.model.GuidanceAction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the haptic + audio channels for a [GuidanceAction]. Debounces so a
 * cue fires on change (or after [REPEAT_MS] for a persistent state) rather than
 * every camera frame.
 */
@Singleton
class GuidanceOrchestrator @Inject constructor(
    private val haptics: HapticEngine,
    private val audio: AudioEngine,
) {
    private var lastAction: GuidanceAction? = null
    private var lastEmittedMs = 0L

    fun onAction(action: GuidanceAction) {
        val now = System.currentTimeMillis()
        val changed = action != lastAction
        if (!changed && now - lastEmittedMs < REPEAT_MS) return
        lastAction = action
        lastEmittedMs = now
        haptics.play(action)
        audio.speak(spokenFor(action))
    }

    fun spokenFor(action: GuidanceAction): String = when (action) {
        GuidanceAction.BOARD -> "Board now"
        GuidanceAction.ALIGHT -> "This is your stop"
        GuidanceAction.WAIT -> "Please wait"
        GuidanceAction.SEEK_ELEVATOR -> "Elevator ahead"
        GuidanceAction.CROSS_WAIT -> "Wait to cross"
        GuidanceAction.CROSS_NOW -> "Cross now"
        GuidanceAction.TURN_LEFT -> "Turn left"
        GuidanceAction.TURN_RIGHT -> "Turn right"
        GuidanceAction.ARRIVED -> "You have arrived"
        GuidanceAction.RECALCULATING -> "Recalculating route"
        GuidanceAction.ALERT -> "Attention"
        GuidanceAction.CONTINUE -> "Continue straight"
    }

    companion object {
        private const val REPEAT_MS = 5_000L
    }
}

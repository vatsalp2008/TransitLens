package com.vatsalp.transitlens.guidance

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.vatsalp.transitlens.core.haptic.HapticPatternCatalog
import com.vatsalp.transitlens.core.model.GuidanceAction
import com.vatsalp.transitlens.core.model.HapticPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Plays the :core [HapticPattern] vocabulary through the device vibrator. */
@Singleton
class HapticEngine @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun play(pattern: HapticPattern) {
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, pattern.repeat)
        vibrator.vibrate(effect)
    }

    fun play(action: GuidanceAction) = play(HapticPatternCatalog.patternFor(action))

    fun cancel() = vibrator.cancel()
}

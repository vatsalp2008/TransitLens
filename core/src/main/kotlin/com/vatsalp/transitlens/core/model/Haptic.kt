package com.vatsalp.transitlens.core.model

/**
 * The haptic navigation vocabulary — a learnable, distinguishable set of
 * vibration patterns that convey navigation meaning without text or audio.
 *
 * [timings] and [amplitudes] mirror Android's
 * `VibrationEffect.createWaveform(timings, amplitudes, repeat)` contract:
 *  - the two arrays must be the same length
 *  - amplitudes are in 0..255 (0 = motor off)
 *  - index 0 is the initial "off" delay, so its amplitude is 0
 *  - repeat == -1 plays once; repeat >= 0 loops from that index
 *
 * A single-motor phone cannot render spatial left/right, so TURN_LEFT and
 * TURN_RIGHT are distinguished temporally (short-short-long vs long-short-short)
 * rather than spatially.
 */
enum class HapticPattern(
    val timings: LongArray,
    val amplitudes: IntArray,
    val repeat: Int = -1,
) {
    TURN_LEFT(longArrayOf(0, 80, 60, 80, 60, 260), intArrayOf(0, 200, 0, 200, 0, 255)),
    TURN_RIGHT(longArrayOf(0, 260, 60, 80, 60, 80), intArrayOf(0, 255, 0, 200, 0, 200)),
    CONTINUE(longArrayOf(0, 140), intArrayOf(0, 160)),
    BOARD_NOW(longArrayOf(0, 90, 60, 90, 60, 90), intArrayOf(0, 255, 0, 255, 0, 255)),
    ALIGHT_NOW(longArrayOf(0, 280, 120, 280), intArrayOf(0, 255, 0, 255)),
    WAIT(longArrayOf(0, 150, 850), intArrayOf(0, 130, 0), repeat = 0),
    ARRIVED(longArrayOf(0, 300, 100, 90, 100, 300), intArrayOf(0, 255, 0, 200, 0, 255)),
    CROSSING_SAFE(longArrayOf(0, 55, 35, 55, 35, 55, 35, 55), intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)),
    ELEVATOR_AHEAD(
        longArrayOf(0, 90, 60, 90, 300, 90, 60, 90, 300, 90, 60, 90),
        intArrayOf(0, 220, 0, 220, 0, 220, 0, 220, 0, 220, 0, 220),
    ),
    ALERT(longArrayOf(0, 200, 80, 90, 80, 250, 80, 120), intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)),
    RECALCULATING(longArrayOf(0, 110), intArrayOf(0, 110)),
    SINGLE_PULSE(longArrayOf(0, 60), intArrayOf(0, 180)),
}

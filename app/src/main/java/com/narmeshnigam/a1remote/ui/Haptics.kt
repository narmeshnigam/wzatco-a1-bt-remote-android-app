package com.narmeshnigam.a1remote.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** BUILD_SPEC §5: a 12 ms tick on key-down. Short enough to feel like a click, not a buzz. */
private const val TICK_MS = 12L

/** The confirming second tick of the power long-press. */
private const val CONFIRM_GAP_MS = 60L

/**
 * The remote's haptics.
 *
 * In a dark room the tick is most of the feedback the user gets — it fires on key-**down**, not
 * on release, so it lands at the moment the report goes out rather than after it.
 */
class Haptics(private val vibrator: Vibrator?) {

    /** One 12 ms tick. Key-down. */
    fun tick() {
        val effect = VibrationEffect.createOneShot(TICK_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator?.vibrate(effect)
    }

    /** Two ticks: the power key confirming that the 600 ms hold has completed. */
    fun confirm() {
        val timings = longArrayOf(0, TICK_MS, CONFIRM_GAP_MS, TICK_MS)
        val amplitudes = intArrayOf(0, DEFAULT_AMPLITUDE, 0, DEFAULT_AMPLITUDE)
        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    private companion object {
        const val DEFAULT_AMPLITUDE = VibrationEffect.DEFAULT_AMPLITUDE
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context) { Haptics(context.vibrator()) }
}

private fun Context.vibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    getSystemService(VibratorManager::class.java)?.defaultVibrator
} else {
    @Suppress("DEPRECATION")
    getSystemService(Vibrator::class.java)
}

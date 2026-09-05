package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Key-down / key-up as a modifier.
 *
 * [onUp] runs from a `finally`, so it fires on release, on gesture cancellation and when the
 * finger leaves the key — the guarantee BUILD_SPEC §4 asks for, at the gesture level.
 */
fun Modifier.pressGesture(enabled: Boolean, onDown: () -> Unit, onUp: () -> Unit): Modifier = pointerInput(enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        onDown()
        try {
            waitForUpOrCancellation()
        } finally {
            onUp()
        }
    }
}

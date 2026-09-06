package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.narmeshnigam.a1remote.service.LinkStage
import com.narmeshnigam.a1remote.service.LinkState
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Icons
import com.narmeshnigam.a1remote.ui.theme.A1Type
import com.narmeshnigam.a1remote.ui.theme.dashedBorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** BUILD_SPEC §5: power needs a deliberate 600 ms hold, never a stray tap. */
private const val POWER_HOLD_MS = 600L
private const val DISABLED_ALPHA = 0.45f

/**
 * The status row: link dot, host name over link state, power key at the right.
 *
 * A long press anywhere on the host name opens the diagnostics sheet (DESIGN_SPEC, wire log).
 */
@Composable
fun StatusRow(
    state: LinkState,
    powerVerified: Boolean,
    onPower: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = A1Dimens.ScreenPadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val holdStart = System.currentTimeMillis()
                        waitForUpOrCancellation()
                        if (System.currentTimeMillis() - holdStart >= POWER_HOLD_MS) onOpenDiagnostics()
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            A1LinkDot(live = state.isConnected)
            Column {
                BasicText(
                    text = state.hostName ?: "Not connected",
                    style = A1Type.HostName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(text = linkLabel(state), style = A1Type.StatusSubLabel)
            }
        }
        PowerKey(
            // Confirmed power needs a live link; unverified power only routes to Fix Keys, so it
            // stays tappable with no host.
            enabled = state.isConnected || !powerVerified,
            verified = powerVerified,
            onFire = onPower,
        )
    }
}

/**
 * The power key. When confirmed it fires only after [POWER_HOLD_MS] of continuous hold, because a
 * projector turning itself off mid-film is not a recoverable mistake from the phone. While it is
 * still unverified — no power code is known to work on the A1 — a plain tap opens Fix Keys for
 * Power instead, so the dead key becomes the way to fix it.
 */
@Composable
private fun PowerKey(enabled: Boolean, verified: Boolean, onFire: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    var armed by remember { mutableStateOf(false) }
    val tint = if (verified) A1Colors.Paper else A1Colors.UnverifiedLabel
    val setArmed: (Boolean) -> Unit = { armed = it }

    Box(
        modifier = modifier
            .size(A1Dimens.PowerKey)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(if (armed) A1Colors.Pressed else Color.Transparent, RectangleShape)
            .then(
                if (verified) {
                    Modifier.border(A1Dimens.Hairline, A1Colors.KeyBorder, RectangleShape)
                } else {
                    Modifier.dashedBorder(A1Colors.UnverifiedBorder, A1Dimens.Hairline)
                },
            )
            .then(
                if (verified) {
                    Modifier.powerHold(enabled, scope, haptics, setArmed, onFire)
                } else {
                    Modifier.powerTap(enabled, haptics, setArmed, onFire)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = A1Icons.Power,
            contentDescription = "Power",
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

/** The confirmed power gesture: a deliberate 600 ms hold, with a second haptic tick as it fires. */
private fun Modifier.powerHold(
    enabled: Boolean,
    scope: CoroutineScope,
    haptics: Haptics,
    setArmed: (Boolean) -> Unit,
    onFire: () -> Unit,
): Modifier = pointerInput(enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var fired = false
        haptics.tick()
        val job = scope.launch {
            delay(POWER_HOLD_MS)
            setArmed(true)
            fired = true
            // BUILD_SPEC §5: the second tick is the only signal that the hold completed, so it
            // fires before the report, not after it.
            haptics.confirm()
            onFire()
        }
        try {
            waitForUpOrCancellation()
        } finally {
            job.cancel()
            if (fired) {
                scope.launch {
                    delay(POWER_HOLD_MS / 4)
                    setArmed(false)
                }
            } else {
                setArmed(false)
            }
        }
    }
}

/** The unverified power gesture: a plain tap that routes to Fix Keys, never a power-off. */
private fun Modifier.powerTap(
    enabled: Boolean,
    haptics: Haptics,
    setArmed: (Boolean) -> Unit,
    onFire: () -> Unit,
): Modifier = pointerInput(enabled) {
    if (!enabled) return@pointerInput
    detectTapGestures(
        onPress = {
            setArmed(true)
            tryAwaitRelease()
            setArmed(false)
        },
        onTap = {
            haptics.tick()
            onFire()
        },
    )
}

private fun linkLabel(state: LinkState): String = when (state.stage) {
    LinkStage.STOPPED -> "HID · stopped"
    LinkStage.PERMISSION_DENIED -> "HID · permission denied"
    LinkStage.NO_BLUETOOTH -> "HID · no adapter"
    LinkStage.BLUETOOTH_OFF -> "HID · bluetooth off"
    LinkStage.ACQUIRING_PROXY -> "HID · acquiring profile"
    LinkStage.PROXY_REFUSED -> "HID · profile refused"
    LinkStage.REGISTERING -> "HID · registering"
    LinkStage.REGISTRATION_REFUSED -> "HID · registration refused"
    LinkStage.REGISTERED -> "HID · registered, no host"
    LinkStage.CONNECTING -> "HID · connecting…"
    LinkStage.CONNECTED -> "HID · connected"
}

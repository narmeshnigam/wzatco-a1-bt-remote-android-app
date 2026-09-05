package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.narmeshnigam.a1remote.service.LinkStage
import com.narmeshnigam.a1remote.service.LinkState
import com.narmeshnigam.a1remote.service.WireLogEntry
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * The diagnostic panel, reached by long-pressing the status row (DESIGN_SPEC, wire log).
 *
 * It shows the result of every platform call on the way to a registration, so a ROM-level
 * refusal reads as itself, plus the last 50 reports the app tried to transmit.
 */
@Composable
fun DiagnosticsSheet(
    state: LinkState,
    wireLog: List<WireLogEntry>,
    onRegister: () -> Unit,
    onUnregister: () -> Unit,
    onClearLog: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(A1Colors.Field, RectangleShape)
            .padding(horizontal = A1Dimens.ScreenPadding, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BasicText(text = "Diagnostics", style = A1Type.ScreenTitle)

        A1Panel(modifier = Modifier.fillMaxWidth()) {
            Column {
                A1DetailRow("Stage", stageLabel(state.stage))
                A1DetailRow("BluetoothManager", flag(state.bluetoothManagerAcquired))
                A1DetailRow("Adapter", flag(state.adapterAcquired))
                A1DetailRow("Adapter enabled", flag(state.adapterEnabled))
                A1DetailRow("getProfileProxy()", flag(state.proxyRequestAccepted))
                A1DetailRow("Proxy connected", flag(state.proxyConnected))
                A1DetailRow("registerApp()", flag(state.registerAppReturned))
                A1DetailRow("onAppStatusChanged", flag(state.appStatusRegistered))
                A1DetailRow("Host address", state.hostAddress ?: "—")
            }
        }

        state.message?.let { message -> BasicText(text = message, style = A1Type.Body) }

        Row(horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter)) {
            A1Key("Register", onRegister, Modifier.weight(1f).height(A1Dimens.KeyHeight))
            A1Key("Unregister", onUnregister, Modifier.weight(1f).height(A1Dimens.KeyHeight))
            A1Key("Clear log", onClearLog, Modifier.weight(1f).height(A1Dimens.KeyHeight))
        }

        BasicText(text = "WIRE LOG", style = A1Type.StatusSubLabel)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            reverseLayout = true,
        ) {
            items(wireLog) { entry ->
                BasicText(
                    text = "${clock(entry.atMillis)}  ${entry.label}  ${entry.detail}",
                    style = A1Type.Mono,
                )
            }
        }

        A1Key(
            label = "Close",
            style = KeyStyle.PRIMARY,
            onPress = onClose,
            modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
        )
    }
}

private fun flag(value: Boolean?): String = when (value) {
    null -> "—"
    true -> "true"
    false -> "FALSE"
}

private fun stageLabel(stage: LinkStage): String = when (stage) {
    LinkStage.STOPPED -> "Stopped"
    LinkStage.PERMISSION_DENIED -> "Permission denied"
    LinkStage.NO_BLUETOOTH -> "No Bluetooth adapter"
    LinkStage.BLUETOOTH_OFF -> "Bluetooth off"
    LinkStage.ACQUIRING_PROXY -> "Acquiring HID profile"
    LinkStage.PROXY_REFUSED -> "HID profile refused"
    LinkStage.REGISTERING -> "Registering"
    LinkStage.REGISTRATION_REFUSED -> "Registration refused"
    LinkStage.REGISTERED -> "Registered, no host"
    LinkStage.CONNECTING -> "Connecting"
    LinkStage.CONNECTED -> "Connected"
}

/** `mm:ss`, the wire-log timestamp of DESIGN_SPEC. */
private fun clock(atMillis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(atMillis)
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

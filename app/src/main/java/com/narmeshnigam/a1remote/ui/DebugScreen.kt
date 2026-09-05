package com.narmeshnigam.a1remote.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.narmeshnigam.a1remote.service.LinkStage
import com.narmeshnigam.a1remote.service.LinkState
import com.narmeshnigam.a1remote.service.SendResult
import com.narmeshnigam.a1remote.service.WireLogEntry
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Gate 1's whole user interface: the result of every platform call on the way to a registration,
 * one button that sends Arrow Down, and the wire log.
 *
 * This is a diagnostic screen, not the remote. The keypad of DESIGN_SPEC arrives at Gate 2.
 */
@Composable
fun DebugScreen(
    state: LinkState,
    wireLog: List<WireLogEntry>,
    lastSendResult: SendResult?,
    notificationsDenied: Boolean,
    onRegister: () -> Unit,
    onUnregister: () -> Unit,
    onSendArrowDown: () -> Unit,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = A1Dimens.ScreenPadding, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            A1LinkDot(live = state.isConnected, modifier = Modifier.padding(end = 10.dp))
            Column {
                BasicText(text = state.hostName ?: "No host", style = A1Type.HostName)
                BasicText(text = stageLabel(state.stage), style = A1Type.StatusSubLabel)
            }
        }

        A1Panel(modifier = Modifier.fillMaxWidth()) {
            Column {
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

        state.message?.let { message ->
            BasicText(text = message, style = A1Type.Body)
        }

        if (notificationsDenied) {
            BasicText(
                text = "Notification permission refused — the service notification is hidden and " +
                    "the system will stop the link sooner.",
                style = A1Type.Hint,
            )
        }

        A1Key(
            label = "Send arrow down",
            onClick = onSendArrowDown,
            enabled = state.isConnected,
            filled = true,
            modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
        )

        lastSendResult?.let { result ->
            BasicText(text = "Last send: ${result.name}", style = A1Type.Hint)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter)) {
            A1Key(
                label = "Register",
                onClick = onRegister,
                modifier = Modifier.weight(1f).height(A1Dimens.KeyHeight),
            )
            A1Key(
                label = "Unregister",
                onClick = onUnregister,
                modifier = Modifier.weight(1f).height(A1Dimens.KeyHeight),
            )
            A1Key(
                label = "Clear log",
                onClick = onClearLog,
                modifier = Modifier.weight(1f).height(A1Dimens.KeyHeight),
            )
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
    LinkStage.CONNECTED -> "Connected"
}

/** `mm:ss`, the wire-log timestamp of DESIGN_SPEC. */
private fun clock(atMillis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(atMillis)
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

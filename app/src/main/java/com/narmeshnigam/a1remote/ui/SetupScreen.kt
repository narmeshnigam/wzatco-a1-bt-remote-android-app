package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.narmeshnigam.a1remote.service.BondedHost
import com.narmeshnigam.a1remote.service.LinkState
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type

/** The three steps, in order. The active one is derived from the link, and the user can tap back. */
private val STEPS = listOf("Register", "Pair", "Connect")

/** Rows of the paired-device list visible before it scrolls; keeps the primary action on screen. */
private const val VISIBLE_HOSTS = 4

private val STEP_TAB_HEIGHT = 50.dp
private val BADGE = 20.dp

/**
 * Setup as a three-step flow (BUILD_SPEC §6): Register → Pair → Connect, one step on screen at a
 * time. The active step follows the link's own progress and the user can step back to any of them.
 *
 * Beyond the flow this screen is the phone's Bluetooth control panel: switch the radio on, off or
 * restart it (restarting clears a stuck registration), and refresh the paired list by hand.
 */
@Composable
fun SetupScreen(
    state: LinkState,
    bondedHosts: List<BondedHost>,
    bluetoothOn: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onConnectHost: (BondedHost) -> Unit,
    onTurnOnBluetooth: () -> Unit,
    onTurnOffBluetooth: () -> Unit,
    onRestartBluetooth: () -> Unit,
    onRefreshDevices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val autoStep = when {
        state.isConnected || state.isConnecting -> 2
        state.isRegistered -> 1
        else -> 0
    }
    var step by rememberSaveable { mutableIntStateOf(autoStep) }
    // The flow advances itself as the link progresses; a manual tap holds until the next change.
    LaunchedEffect(autoStep) { step = autoStep }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = A1Dimens.ScreenPadding)
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicText(text = "Setup", style = A1Type.ScreenTitle)
        StepperHeader(current = step, furthest = autoStep, onSelect = { step = it })

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (step) {
                0 -> RegisterStep(
                    registered = state.isRegistered,
                    bluetoothOn = bluetoothOn,
                    onConnect = onConnect,
                    onTurnOn = onTurnOnBluetooth,
                    onTurnOff = onTurnOffBluetooth,
                    onRestart = onRestartBluetooth,
                )

                1 -> PairStep(
                    registered = state.isRegistered,
                    bondedHosts = bondedHosts,
                    hostAddress = state.hostAddress,
                    onMakeDiscoverable = onMakeDiscoverable,
                    onRefresh = onRefreshDevices,
                    onConnectHost = onConnectHost,
                )

                else -> ConnectStep(state = state, onConnect = onConnect, onDisconnect = onDisconnect)
            }
        }
    }
}

/** The stepper: three cells flowing left to right, the active filled, reached ones outlined. */
@Composable
private fun StepperHeader(current: Int, furthest: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(STEP_TAB_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        STEPS.forEachIndexed { index, label ->
            StepTab(
                number = index + 1,
                label = label,
                active = index == current,
                reached = index <= furthest,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun StepTab(
    number: Int,
    label: String,
    active: Boolean,
    reached: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val border = if (active || reached) A1Colors.Accent else A1Colors.KeyBorder
    Row(
        modifier = modifier
            .background(if (active) A1Colors.Accent else Color.Transparent, RectangleShape)
            .border(A1Dimens.Hairline, border, RectangleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(BADGE)
                .border(A1Dimens.Hairline, if (active) A1Colors.Paper else border, RectangleShape),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text = "$number", style = A1Type.StepLabel.copy(color = A1Colors.Paper))
        }
        BasicText(text = label.uppercase(), style = A1Type.KeyLabel)
    }
}

/** Step 1: registration status and the phone's Bluetooth radio controls. */
@Composable
private fun RegisterStep(
    registered: Boolean,
    bluetoothOn: Boolean,
    onConnect: () -> Unit,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxHeight()) {
        StepCard(
            step = "Registration",
            body = if (registered) {
                "Registered as “WZATCO A1 Remote”."
            } else {
                "Not registered. Turn Bluetooth on, then tap Register."
            },
        )

        A1Panel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    A1LinkDot(live = bluetoothOn)
                    BasicText(
                        text = if (bluetoothOn) "PHONE BLUETOOTH · ON" else "PHONE BLUETOOTH · OFF",
                        style = A1Type.StatusSubLabel,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter), modifier = Modifier.fillMaxWidth()) {
                    A1Key(
                        "On",
                        onTurnOn,
                        enabled = !bluetoothOn,
                        modifier = Modifier.weight(1f).height(A1Dimens.MinTouch),
                    )
                    A1Key(
                        "Off",
                        onTurnOff,
                        enabled = bluetoothOn,
                        modifier = Modifier.weight(1f).height(A1Dimens.MinTouch),
                    )
                    A1Key(
                        "Restart",
                        onRestart,
                        enabled = bluetoothOn,
                        modifier = Modifier.weight(1f).height(A1Dimens.MinTouch),
                    )
                }
                BasicText(
                    text = "Restart cycles the radio off and on — the surest fix for a stuck registration. " +
                        "If the system asks, allow it.",
                    style = A1Type.Hint,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (!registered) {
            A1Key(
                label = "Register",
                style = KeyStyle.PRIMARY,
                onPress = onConnect,
                enabled = bluetoothOn,
                modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
            )
        }
    }
}

/** Step 2: pair from the phone, refresh the list, and pick the device to connect. */
@Composable
private fun PairStep(
    registered: Boolean,
    bondedHosts: List<BondedHost>,
    hostAddress: String?,
    onMakeDiscoverable: () -> Unit,
    onRefresh: () -> Unit,
    onConnectHost: (BondedHost) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxHeight()) {
        StepCard(
            step = "Pair",
            body = "Pair from the phone's Bluetooth settings (Settings → Bluetooth → the A1), then " +
                "tap it below. Tap Refresh if a device you just paired is missing.",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter), modifier = Modifier.fillMaxWidth()) {
            A1Key(
                "Make discoverable",
                onMakeDiscoverable,
                enabled = registered,
                modifier = Modifier.weight(1f).height(A1Dimens.MinTouch),
            )
            A1Key("Refresh", onRefresh, modifier = Modifier.weight(1f).height(A1Dimens.MinTouch))
        }

        BasicText(text = "PAIRED DEVICES · TAP TO CONNECT", style = A1Type.StepLabel)
        if (bondedHosts.isEmpty()) {
            BasicText(text = "No paired devices yet.", style = A1Type.Hint)
        }
        Column(
            modifier = Modifier
                .heightIn(max = A1Dimens.MinTouch * VISIBLE_HOSTS + A1Dimens.Gutter * (VISIBLE_HOSTS - 1))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
        ) {
            bondedHosts.forEach { host ->
                A1Key(
                    label = host.name,
                    onPress = { onConnectHost(host) },
                    enabled = registered && hostAddress != host.address,
                    modifier = Modifier.fillMaxWidth().height(A1Dimens.MinTouch),
                    tapToClick = true,
                )
            }
        }
    }
}

/** Step 3: connection status, the standby unknown, and the connect/disconnect action. */
@Composable
private fun ConnectStep(state: LinkState, onConnect: () -> Unit, onDisconnect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxHeight()) {
        StepCard(
            step = "Connect",
            body = when {
                state.isConnected -> "Connected to ${state.hostName ?: "the A1"}."
                state.isConnecting -> "Connecting to ${state.hostName ?: "the A1"}…"
                else -> "Not connected. The app reaches for your last device on start; or pick it under Pair."
            },
        )
        StepCard(
            step = "Standby test",
            body = "Unverified: whether the A1 keeps Bluetooth alive in standby.",
            dashed = true,
            bodyColor = true,
        )

        Spacer(modifier = Modifier.weight(1f))

        A1Key(
            label = if (state.isRegistered) "Disconnect" else "Connect to A1",
            style = KeyStyle.PRIMARY,
            onPress = if (state.isRegistered) onDisconnect else onConnect,
            modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
        )
    }
}

@Composable
private fun StepCard(step: String, body: String, dashed: Boolean = false, bodyColor: Boolean = false) {
    A1Panel(modifier = Modifier.fillMaxWidth(), dashed = dashed) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(text = step.uppercase(), style = A1Type.StepLabel)
            BasicText(
                text = body,
                style = if (bodyColor) A1Type.Body.copy(color = A1Colors.UnverifiedLabel) else A1Type.Body,
            )
        }
    }
}

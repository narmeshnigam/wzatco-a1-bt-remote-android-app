package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.narmeshnigam.a1remote.service.LinkState
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type

/**
 * Setup: three numbered steps and the connect/disconnect primary action (BUILD_SPEC §6).
 *
 * The standby-test card is dashed because its answer is genuinely unknown — open question 2.
 */
@Composable
fun SetupScreen(
    state: LinkState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = A1Dimens.ScreenPadding)
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            BasicText(text = "Setup", style = A1Type.ScreenTitle)
            BasicText(
                text = "The phone registers itself as a Bluetooth remote. The projector does the pairing.",
                style = A1Type.Hint,
            )
        }

        StepCard(
            step = "Step 1 · Register",
            body = if (state.isRegistered) {
                "HID device profile registered as “WZATCO A1 Remote”."
            } else {
                "Not registered yet. Tap Connect below and grant Bluetooth if asked."
            },
        )

        StepCard(
            step = "Step 2 · Pair from the projector",
            body = "Tap Make discoverable, then on the A1: Settings → Bluetooth → pick this phone.",
        )

        StepCard(
            step = "Step 3 · Standby test",
            body = "Unverified: whether the A1 keeps the Bluetooth host alive in standby.",
            dashed = true,
            bodyColor = true,
        )

        Spacer(modifier = Modifier.weight(1f))

        A1Key(
            label = "Make discoverable",
            onPress = onMakeDiscoverable,
            enabled = state.isRegistered,
            modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
        )

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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(text = step.uppercase(), style = A1Type.StepLabel)
            BasicText(
                text = body,
                style = if (bodyColor) {
                    A1Type.Body.copy(color = A1Colors.UnverifiedLabel)
                } else {
                    A1Type.Body
                },
            )
        }
    }
}

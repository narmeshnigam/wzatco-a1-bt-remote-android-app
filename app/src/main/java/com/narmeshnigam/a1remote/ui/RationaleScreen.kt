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
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type

/**
 * The permission rationale of BUILD_SPEC §7: says what is needed and why before anything is
 * asked for, and stays honest when a permission has been refused.
 *
 * @param permanentlyDenied true once the system has stopped showing the dialog, so the only
 *   remaining route is app settings
 * @param notificationsDenied true when the optional notification permission was refused; the
 *   app still works, and says exactly what that costs
 */
@Composable
fun RationaleScreen(
    permanentlyDenied: Boolean,
    notificationsDenied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = A1Dimens.ScreenPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BasicText(text = "Before we start", style = A1Type.ScreenTitle)

        A1Panel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(text = "NEARBY DEVICES", style = A1Type.StatusSubLabel)
                BasicText(
                    text = "The phone has to register itself with the Bluetooth stack as a keyboard " +
                        "and mouse, and stay connected to the projector. Android calls that " +
                        "permission Nearby devices. Nothing is scanned for and nothing is shared.",
                    style = A1Type.Body,
                )
            }
        }

        A1Panel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(text = "NOTIFICATIONS", style = A1Type.StatusSubLabel)
                BasicText(
                    text = "The remote runs as a foreground service with a permanent notification. " +
                        "That notification is what stops the system treating the link as idle " +
                        "background work and killing it. You can refuse it and the remote still " +
                        "works, but it will be shut down more often.",
                    style = A1Type.Body,
                )
            }
        }

        BasicText(
            text = "This app has no internet permission and sends nothing anywhere.",
            style = A1Type.Hint,
        )

        if (notificationsDenied) {
            BasicText(
                text = "Notifications refused. The link will still register, but expect the system " +
                    "to stop the service sooner.",
                style = A1Type.Hint,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (permanentlyDenied) {
            BasicText(
                text = "Android will not ask again. Grant Nearby devices from app settings.",
                style = A1Type.Body,
            )
            A1Key(
                label = "Open app settings",
                onPress = onOpenSettings,
                style = KeyStyle.PRIMARY,
                modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
            )
        } else {
            A1Key(
                label = "Grant permissions",
                onPress = onGrant,
                style = KeyStyle.PRIMARY,
                modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
            )
        }
    }
}

package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type

/**
 * Says what refusing the notification permission actually costs, rather than nagging.
 */
@Composable
fun NotificationWarning(modifier: Modifier = Modifier) {
    BasicText(
        text = "Notifications are off — the link will be stopped by the system sooner.",
        style = A1Type.Hint,
        modifier = modifier.padding(horizontal = A1Dimens.ScreenPadding, vertical = 6.dp),
    )
}

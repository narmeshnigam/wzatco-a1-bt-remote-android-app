package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type

private const val DISABLED_ALPHA = 0.45f

/**
 * A key: square corners, hairline border, [A1Colors.Pressed] fill while held, never below the
 * minimum touch target of DESIGN_SPEC. The one button shape this app has.
 */
@Composable
fun A1Key(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background = when {
        pressed -> A1Colors.Pressed
        filled -> A1Colors.Accent
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = A1Dimens.MinTouch, minHeight = A1Dimens.MinTouch)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(background, RectangleShape)
            .border(A1Dimens.Hairline, A1Colors.KeyBorder, RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = label.uppercase(), style = A1Type.KeyLabel)
    }
}

/** A hairline-bordered block: the card shape of DESIGN_SPEC. */
@Composable
fun A1Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .border(A1Dimens.Hairline, A1Colors.KeyBorder, RectangleShape)
            .padding(12.dp),
    ) {
        content()
    }
}

/** One diagnostic line: muted uppercase label on the left, value on the right. */
@Composable
fun A1DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(text = label.uppercase(), style = A1Type.StatusSubLabel, modifier = Modifier.weight(1f))
        BasicText(text = value, style = A1Type.Body)
    }
}

/** The link dot of the status row: live, or dead. */
@Composable
fun A1LinkDot(live: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(A1Dimens.LinkDot)
            .background(if (live) A1Colors.LinkLive else A1Colors.LinkDead, RectangleShape),
    )
}

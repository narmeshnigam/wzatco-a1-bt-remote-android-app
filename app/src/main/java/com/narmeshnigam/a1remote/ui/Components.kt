package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type
import com.narmeshnigam.a1remote.ui.theme.dashedBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** DESIGN_SPEC: disabled keys sit at 45 % opacity and do not react. */
private const val DISABLED_ALPHA = 0.45f

/** DESIGN_SPEC: the cell fills with the press colour for 90 ms. */
private const val PRESS_FLASH_MS = 90L

/** How a key should be drawn, which is the app's statement about how much it knows. */
enum class KeyStyle {
    /** Standard usage, proven on an Android host. Hairline border, paper label. */
    VERIFIED,

    /** Not yet confirmed on the A1. Dashed border, accent-300 label. */
    UNVERIFIED,

    /** The one primary action on the screen. Accent fill. */
    PRIMARY,
}

/**
 * A key. Square corners, hairline or dashed border, [A1Colors.Pressed] fill on press, never
 * below the minimum touch target.
 *
 * The press is dispatched on key-**down**, not on release: the remote is used without looking,
 * and auto-repeat (Gate 3) only makes sense on a held key.
 */
@Composable
fun A1Key(
    label: String,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: KeyStyle = KeyStyle.VERIFIED,
    enabled: Boolean = true,
    iconSize: Dp = A1Dimens.KeyIcon,
    showLabel: Boolean = true,
    labelStyle: TextStyle = A1Type.KeyLabel,
) {
    val scope = rememberCoroutineScope()
    var held by remember { mutableStateOf(false) }
    var flashing by remember { mutableStateOf(false) }
    val content = contentColor(style)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = A1Dimens.MinTouch, minHeight = A1Dimens.MinTouch)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(backgroundColor(style, pressed = held || flashing), RectangleShape)
            .keyBorder(style)
            .pressGesture(
                enabled = enabled,
                onDown = {
                    held = true
                    onPress()
                    scope.launch {
                        flashing = true
                        delay(PRESS_FLASH_MS)
                        flashing = false
                    }
                },
                onUp = { held = false },
            )
            .semantics {
                role = Role.Button
                contentDescription = label
                if (!enabled) disabled()
                onClick(label = label) {
                    onPress()
                    true
                }
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (icon != null) {
                Image(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    colorFilter = ColorFilter.tint(content),
                )
            }
            if (showLabel) {
                BasicText(
                    text = label.uppercase(),
                    style = labelStyle.copy(color = content, textAlign = TextAlign.Center),
                )
            }
        }
    }
}

private fun contentColor(style: KeyStyle): Color =
    if (style == KeyStyle.UNVERIFIED) A1Colors.UnverifiedLabel else A1Colors.Paper

private fun backgroundColor(style: KeyStyle, pressed: Boolean): Color = when {
    pressed -> A1Colors.Pressed
    style == KeyStyle.PRIMARY -> A1Colors.Accent
    else -> Color.Transparent
}

private fun Modifier.keyBorder(style: KeyStyle): Modifier = when (style) {
    KeyStyle.UNVERIFIED -> dashedBorder(A1Colors.UnverifiedBorder, A1Dimens.Hairline)
    KeyStyle.PRIMARY -> border(A1Dimens.Hairline, A1Colors.Accent, RectangleShape)
    KeyStyle.VERIFIED -> border(A1Dimens.Hairline, A1Colors.KeyBorder, RectangleShape)
}

/** A hairline- or dashed-bordered block: the card shape of DESIGN_SPEC. */
@Composable
fun A1Panel(modifier: Modifier = Modifier, dashed: Boolean = false, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .then(
                if (dashed) {
                    Modifier.dashedBorder(A1Colors.KeyBorder, A1Dimens.Hairline)
                } else {
                    Modifier.border(A1Dimens.Hairline, A1Colors.KeyBorder, RectangleShape)
                },
            )
            .padding(14.dp),
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
            .background(if (live) A1Colors.LinkLive else A1Colors.LinkDead, CircleShape),
    )
}

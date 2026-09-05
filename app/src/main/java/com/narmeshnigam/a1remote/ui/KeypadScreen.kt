package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.KeyStatus
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Icons
import com.narmeshnigam.a1remote.ui.theme.A1Type

/**
 * The keypad of DESIGN_SPEC: a 258 dp D-pad block over four rows of three.
 *
 * The geometry is fixed and never scrolls or reflows — in an unlit room, position is the only
 * cue a thumb has. Whether a key is drawn verified comes from the live key map, so a Key Lab
 * confirmation flips it without a rebuild.
 */
@Composable
fun KeypadScreen(
    bindings: Map<RemoteFunction, KeyBinding>,
    connected: Boolean,
    onPress: (RemoteFunction) -> Unit,
    onOpenCursor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = A1Dimens.ScreenPadding)
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DPadBlock(
            bindings = bindings,
            connected = connected,
            onPress = onPress,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(max = A1Dimens.DpadBlock),
        )

        KeyRow {
            FunctionKey(RemoteFunction.BACK, "Back", A1Icons.Back, bindings, connected, onPress)
            FunctionKey(RemoteFunction.HOME, "Home", A1Icons.Home, bindings, connected, onPress)
            FunctionKey(RemoteFunction.MENU, "Menu", A1Icons.Menu, bindings, connected, onPress)
        }

        KeyRow {
            FunctionKey(RemoteFunction.VOLUME_DOWN, "Vol −", A1Icons.VolumeDown, bindings, connected, onPress)
            FunctionKey(RemoteFunction.MUTE, "Mute", A1Icons.Mute, bindings, connected, onPress)
            FunctionKey(RemoteFunction.VOLUME_UP, "Vol +", A1Icons.VolumeUp, bindings, connected, onPress)
        }

        KeyRow {
            FunctionKey(RemoteFunction.FOCUS_DOWN, "Focus −", A1Icons.FocusDown, bindings, connected, onPress)
            FunctionKey(RemoteFunction.FOCUS_UP, "Focus +", A1Icons.FocusUp, bindings, connected, onPress)
            FunctionKey(RemoteFunction.SOURCE, "Source", A1Icons.Source, bindings, connected, onPress)
        }

        KeyRow {
            FunctionKey(RemoteFunction.SCREEN_FLIP, "Flip", A1Icons.Flip, bindings, connected, onPress)
            FunctionKey(RemoteFunction.KEYSTONE, "Keystone", A1Icons.Keystone, bindings, connected, onPress)
            A1Key(
                label = "Cursor",
                icon = A1Icons.Cursor,
                onPress = onOpenCursor,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
        horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
        content = content,
    )
}

@Composable
private fun RowScope.FunctionKey(
    function: RemoteFunction,
    label: String,
    icon: ImageVector,
    bindings: Map<RemoteFunction, KeyBinding>,
    connected: Boolean,
    onPress: (RemoteFunction) -> Unit,
) {
    A1Key(
        label = label,
        icon = icon,
        style = styleOf(bindings[function]),
        enabled = connected,
        onPress = { onPress(function) },
        modifier = Modifier.weight(1f).fillMaxHeight(),
    )
}

/**
 * The 3 × 3 D-pad. Corner cells are empty on purpose: they are the negative space that makes
 * the four arrows findable by feel.
 */
@Composable
private fun DPadBlock(
    bindings: Map<RemoteFunction, KeyBinding>,
    connected: Boolean,
    onPress: (RemoteFunction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
    ) {
        DPadRow {
            Spacer(Modifier.weight(1f))
            DPadKey(RemoteFunction.UP, "Up", A1Icons.ChevronUp, bindings, connected, onPress)
            Spacer(Modifier.weight(1f))
        }
        DPadRow {
            DPadKey(RemoteFunction.LEFT, "Left", A1Icons.ChevronLeft, bindings, connected, onPress)
            A1Key(
                label = "OK",
                style = KeyStyle.PRIMARY,
                enabled = connected,
                onPress = { onPress(RemoteFunction.OK) },
                labelStyle = A1Type.OkGlyph,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            DPadKey(RemoteFunction.RIGHT, "Right", A1Icons.ChevronRight, bindings, connected, onPress)
        }
        DPadRow {
            Spacer(Modifier.weight(1f))
            DPadKey(RemoteFunction.DOWN, "Down", A1Icons.ChevronDown, bindings, connected, onPress)
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ColumnScope.DPadRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
        content = content,
    )
}

@Composable
private fun RowScope.DPadKey(
    function: RemoteFunction,
    label: String,
    icon: ImageVector,
    bindings: Map<RemoteFunction, KeyBinding>,
    connected: Boolean,
    onPress: (RemoteFunction) -> Unit,
) {
    A1Key(
        label = label,
        icon = icon,
        showLabel = false,
        iconSize = A1Dimens.DpadIcon,
        style = styleOf(bindings[function]),
        enabled = connected,
        onPress = { onPress(function) },
        modifier = Modifier.weight(1f).fillMaxHeight(),
    )
}

/** DESIGN_SPEC: anything short of confirmed is drawn unverified. */
private fun styleOf(binding: KeyBinding?): KeyStyle =
    if (binding?.status == KeyStatus.CONFIRMED) KeyStyle.VERIFIED else KeyStyle.UNVERIFIED

package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type
import com.narmeshnigam.a1remote.vm.CursorViewModel

/** DESIGN_SPEC: a disabled surface sits at 45 % opacity and does not react. */
private const val DISABLED_ALPHA = 0.45f

/** The hint line wraps to roughly three lines at this width, as in the prototype. */
private val HINT_WIDTH = 230.dp

/**
 * The cursor screen of DESIGN_SPEC: a full-height dashed drag surface, Left click and Back as
 * two equal keys, then a full-width return to the keypad.
 *
 * Nothing draws a pointer on the phone — the host owns the cursor, and a second one here would
 * only disagree with it.
 */
@Composable
fun CursorScreen(
    onReturnToKeypad: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CursorViewModel = viewModel(),
) {
    val state by viewModel.link.collectAsStateWithLifecycle()
    CursorScreenContent(
        connected = state.isConnected,
        onBeginDrag = viewModel::beginDrag,
        onMove = viewModel::move,
        onLeftClick = viewModel::leftClick,
        onBack = viewModel::back,
        onReturnToKeypad = onReturnToKeypad,
        modifier = modifier,
    )
}

@Composable
private fun CursorScreenContent(
    connected: Boolean,
    onBeginDrag: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onLeftClick: () -> Unit,
    onBack: () -> Unit,
    onReturnToKeypad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = A1Dimens.ScreenPadding)
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        A1Panel(
            dashed = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .alpha(if (connected) 1f else DISABLED_ALPHA)
                .dragSurface(
                    enabled = connected,
                    onBeginDrag = onBeginDrag,
                    onMove = onMove,
                    onTap = onLeftClick,
                    onTwoFingerTap = onBack,
                ),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BasicText(text = "Trackpad", style = A1Type.ScreenTitle)
                    BasicText(
                        text = "Drag to send cursor movement. Single tap is left click, " +
                            "two-finger tap is Back.",
                        style = A1Type.Hint.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.widthIn(max = HINT_WIDTH),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
            horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
        ) {
            A1Key(
                label = "Left click",
                enabled = connected,
                onPress = onLeftClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            A1Key(
                label = "Back",
                enabled = connected,
                onPress = onBack,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        A1Key(
            label = "Back to keypad",
            onPress = onReturnToKeypad,
            modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
        )
    }
}

/**
 * The trackpad gesture.
 *
 * One finger drags the host's pointer and, if it never travels past the touch slop, clicks. A
 * second finger turns the whole gesture into Back and suppresses both — a two-finger tap that
 * also moved the pointer and clicked would be worse than no gesture at all.
 */
private fun Modifier.dragSurface(
    enabled: Boolean,
    onBeginDrag: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onTap: () -> Unit,
    onTwoFingerTap: () -> Unit,
): Modifier = pointerInput(enabled) {
    if (!enabled) return@pointerInput
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)
        onBeginDrag()

        var maxPointers = 1
        var travel = 0f
        var stillDown = 1
        while (stillDown > 0) {
            val event = awaitPointerEvent()
            stillDown = event.changes.count { it.pressed }
            maxPointers = maxOf(maxPointers, stillDown)

            val primary = event.changes.firstOrNull { it.id == first.id }
            if (primary == null || !primary.pressed) continue
            val delta = primary.positionChange()
            travel += delta.getDistance()
            if (maxPointers == 1) onMove(delta.x, delta.y)
        }

        if (travel <= slop) {
            if (maxPointers >= 2) onTwoFingerTap() else onTap()
        }
    }
}

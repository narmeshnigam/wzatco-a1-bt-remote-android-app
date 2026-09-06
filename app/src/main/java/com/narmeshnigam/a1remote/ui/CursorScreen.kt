package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type
import com.narmeshnigam.a1remote.vm.CursorViewModel

/** DESIGN_SPEC: a disabled surface sits at 45 % opacity and does not react. */
private const val DISABLED_ALPHA = 0.45f

/** The hint line wraps to roughly two lines at this width. */
private val HINT_WIDTH = 250.dp

/** The scroll strip, expanded and collapsed. Wide enough to drag, narrow enough to stay a strip. */
private val STRIP_WIDTH = 46.dp
private val STRIP_HANDLE_WIDTH = 26.dp

/**
 * The trackpad screen (DESIGN_SPEC): a full-height drag surface with a collapsible scroll strip
 * down its right edge, then one compact row of pointer actions.
 *
 * Nothing draws a pointer on the phone — the host owns the cursor, and a second one here would
 * only disagree with it.
 */
@Composable
fun CursorScreen(modifier: Modifier = Modifier, viewModel: CursorViewModel = viewModel()) {
    val state by viewModel.link.collectAsStateWithLifecycle()
    CursorScreenContent(
        connected = state.isConnected,
        onBeginDrag = viewModel::beginDrag,
        onMove = viewModel::move,
        onLeftClick = viewModel::leftClick,
        onDoubleClick = viewModel::doubleClick,
        onBack = viewModel::back,
        onBeginScroll = viewModel::beginScroll,
        onScroll = viewModel::scroll,
        modifier = modifier,
    )
}

@Composable
private fun CursorScreenContent(
    connected: Boolean,
    onBeginDrag: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onLeftClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onBack: () -> Unit,
    onBeginScroll: () -> Unit,
    onScroll: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scrollOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = A1Dimens.ScreenPadding)
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).alpha(if (connected) 1f else DISABLED_ALPHA),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TrackpadSurface(
                connected = connected,
                onBeginDrag = onBeginDrag,
                onMove = onMove,
                onTap = onLeftClick,
                onTwoFingerTap = onBack,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            ScrollStrip(
                open = scrollOpen,
                connected = connected,
                onToggle = { scrollOpen = !scrollOpen },
                onBeginScroll = onBeginScroll,
                onScroll = onScroll,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(A1Dimens.MinTouch),
            horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
        ) {
            A1Key(
                label = "Left click",
                enabled = connected,
                onPress = onLeftClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            A1Key(
                label = "Double click",
                enabled = connected,
                onPress = onDoubleClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            A1Key(
                label = "Back",
                enabled = connected,
                onPress = onBack,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

/** The drag surface: a dashed panel with a subtle centred hint, so it reads as a trackpad. */
@Composable
private fun TrackpadSurface(
    connected: Boolean,
    onBeginDrag: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onTap: () -> Unit,
    onTwoFingerTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    A1Panel(
        dashed = true,
        modifier = modifier.dragSurface(
            enabled = connected,
            onBeginDrag = onBeginDrag,
            onMove = onMove,
            onTap = onTap,
            onTwoFingerTap = onTwoFingerTap,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BasicText(
                text = "Trackpad\nDrag to move · tap to click · two-finger tap is Back",
                style = A1Type.Hint.copy(textAlign = TextAlign.Center),
                modifier = Modifier.widthIn(max = HINT_WIDTH),
            )
        }
    }
}

/**
 * The scroll strip down the right edge: collapsed to a thin handle by default, expanded to a
 * drag lane on tap. Dragging the lane sends wheel reports; it is the one thing the D-pad and the
 * two-finger gestures cannot reach.
 */
@Composable
private fun ScrollStrip(
    open: Boolean,
    connected: Boolean,
    onToggle: () -> Unit,
    onBeginScroll: () -> Unit,
    onScroll: (Float) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    if (!open) {
        Box(
            modifier = Modifier
                .width(STRIP_HANDLE_WIDTH)
                .fillMaxHeight()
                .border(A1Dimens.Hairline, A1Colors.KeyBorder, RectangleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text = "‹\n↕", style = A1Type.Hint.copy(textAlign = TextAlign.Center))
        }
        return
    }

    Column(
        modifier = Modifier
            .width(STRIP_WIDTH)
            .fillMaxHeight()
            .alpha(if (connected) 1f else DISABLED_ALPHA)
            .border(A1Dimens.Hairline, A1Colors.KeyBorder, RectangleShape),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text = "›", style = A1Type.Body.copy(textAlign = TextAlign.Center))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .scrollGesture(enabled = connected, onBegin = onBeginScroll, onScroll = onScroll),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text = "↕", style = A1Type.Hint)
        }
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

/**
 * The scroll-strip gesture: the vertical component of a one-finger drag, sent as wheel motion.
 *
 * The delta is consumed so the parent Column does not also read it — the strip owns its lane.
 */
private fun Modifier.scrollGesture(enabled: Boolean, onBegin: () -> Unit, onScroll: (Float) -> Unit): Modifier =
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val first = awaitFirstDown(requireUnconsumed = false)
            onBegin()
            var stillDown = 1
            while (stillDown > 0) {
                val event = awaitPointerEvent()
                stillDown = event.changes.count { it.pressed }
                val primary = event.changes.firstOrNull { it.id == first.id }
                if (primary == null || !primary.pressed) continue
                val dy = primary.positionChange().y
                if (dy != 0f) {
                    onScroll(dy)
                    primary.consume()
                }
            }
        }
    }

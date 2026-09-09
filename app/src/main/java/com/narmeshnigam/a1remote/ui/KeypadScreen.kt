package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.KeyStatus
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Icons
import com.narmeshnigam.a1remote.ui.theme.A1Type
import com.narmeshnigam.a1remote.ui.theme.dashedBorder
import com.narmeshnigam.a1remote.vm.AutoRepeat
import com.narmeshnigam.a1remote.vm.RepeatBehaviour
import com.narmeshnigam.a1remote.vm.repeatBehaviour
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The gap between the dial and the block of keys under it.
 *
 * Wider than the 6 dp gutter inside the block on purpose: it is what separates the two things
 * a thumb is choosing between, so it has to be felt as a break rather than as another gutter.
 */
private val DIAL_GAP = 28.dp

/** The OK hub's radius as a share of the dial's own radius. */
private const val HUB_RADIUS_FRACTION = 0.40f

/** Each of the four arrow segments owns a quadrant of the ring. */
private const val SEGMENT_SWEEP = 90f

/**
 * Where each segment starts, in Canvas degrees — 0° is 3 o'clock and angles run clockwise.
 *
 * Written out rather than derived from the enum order because the geometry is the thing being
 * stated here, and a reader has to be able to check it against the drawing.
 */
private val SEGMENTS = listOf(
    RemoteFunction.UP to 225f,
    RemoteFunction.RIGHT to 315f,
    RemoteFunction.DOWN to 45f,
    RemoteFunction.LEFT to 135f,
)

/** DESIGN_SPEC: the unverified dash, matching `Modifier.dashedBorder`. */
private val DASH = 3.dp
private val GAP = 3.dp

/** One arrow of the dial: what it sends, and where on the ring it sits. */
private class DialArrow(
    val function: RemoteFunction,
    val label: String,
    val icon: ImageVector,
    val alignment: Alignment,
    /** The inset that pushes the arrow off its edge and onto the middle of the ring. */
    val edge: (Dp) -> PaddingValues,
)

private val DIAL_ARROWS = listOf(
    DialArrow(RemoteFunction.UP, "Up", A1Icons.ChevronUp, Alignment.TopCenter) { PaddingValues(top = it) },
    DialArrow(RemoteFunction.LEFT, "Left", A1Icons.ChevronLeft, Alignment.CenterStart) { PaddingValues(start = it) },
    DialArrow(RemoteFunction.RIGHT, "Right", A1Icons.ChevronRight, Alignment.CenterEnd) { PaddingValues(end = it) },
    DialArrow(RemoteFunction.DOWN, "Down", A1Icons.ChevronDown, Alignment.BottomCenter) { PaddingValues(bottom = it) },
)

/** How one segment of the ring is painted: its press fill, if any, and its outline. */
private class SegmentPaint(val fill: Color?, val stroke: Color, val dashed: Boolean)

/**
 * The keypad of DESIGN_SPEC: a circular D-pad over a volume rocker and three function keys.
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = A1Dimens.ScreenPadding)
            .padding(bottom = 4.dp),
        // Centred rather than top-aligned: the dial is capped at its spec diameter, so on a tall
        // phone the spare height belongs above and below the pad, not under it.
        verticalArrangement = Arrangement.spacedBy(DIAL_GAP, Alignment.CenterVertically),
    ) {
        DPadDial(
            bindings = bindings,
            connected = connected,
            onPress = onPress,
            // fill = false lets the dial take at most its specified 258 dp on a tall screen and
            // shrink on a short one, so the geometry never scrolls and never clips.
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = A1Dimens.DpadBlock),
        )

        Row(
            // Two rows of keys and the gutter between them: the volume rocker spans both.
            modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight * 2 + A1Dimens.Gutter),
            horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
        ) {
            VolumeRocker(
                bindings = bindings,
                connected = connected,
                onPress = onPress,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            KeyColumn {
                FunctionKey(RemoteFunction.HOME, "Home", A1Icons.Home, bindings, connected, onPress)
                FunctionKey(RemoteFunction.MENU, "Menu", A1Icons.Menu, bindings, connected, onPress)
            }
            KeyColumn {
                FunctionKey(RemoteFunction.BACK, "Back", A1Icons.Back, bindings, connected, onPress)
                FunctionKey(RemoteFunction.MUTE, "Mute", A1Icons.Mute, bindings, connected, onPress)
            }
        }
    }
}

/** One column of the lower block: two keys, one above the other, beside the volume rocker. */
@Composable
private fun RowScope.KeyColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
        content = content,
    )
}

@Composable
private fun FunctionKey(
    function: RemoteFunction,
    label: String,
    icon: ImageVector,
    bindings: Map<RemoteFunction, KeyBinding>,
    connected: Boolean,
    onPress: (RemoteFunction) -> Unit,
) {
    // An unverified key has no code that works on the A1 yet. It says so with a "SET UP" caption,
    // does not auto-repeat, and its press is routed (by the caller) to Fix Keys rather than sent.
    val unverified = styleOf(bindings[function]) == KeyStyle.UNVERIFIED
    A1Key(
        label = label,
        icon = icon,
        // Icon only. The label stays as the key's name in the accessibility tree — a key with
        // nothing to call it is unreachable by screen reader — it is simply not drawn.
        showLabel = false,
        style = styleOf(bindings[function]),
        // A confirmed key needs a live link to send; an unverified key only routes to Fix Keys,
        // so it stays tappable even with no host — it is never a dead button.
        enabled = connected || unverified,
        onPress = { onPress(function) },
        caption = if (unverified) "Set up" else null,
        repeating = !unverified && function.repeatBehaviour() == RepeatBehaviour.REPEATING,
        modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
    )
}

/**
 * Volume as one rocker: a single outline with Vol + above Vol − and a hairline between them.
 *
 * It is one key the height of two, which is what makes it findable without looking — the only
 * control on the lower block that a thumb can identify by size alone.
 */
@Composable
private fun VolumeRocker(
    bindings: Map<RemoteFunction, KeyBinding>,
    connected: Boolean,
    onPress: (RemoteFunction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val up = styleOf(bindings[RemoteFunction.VOLUME_UP])
    val down = styleOf(bindings[RemoteFunction.VOLUME_DOWN])
    // The shared outline can only tell one truth, so it tells the weaker of the two: if either
    // half is unproven on the A1, the whole rocker is drawn unverified.
    val outline = if (up == KeyStyle.VERIFIED && down == KeyStyle.VERIFIED) KeyStyle.VERIFIED else KeyStyle.UNVERIFIED

    Column(
        modifier = modifier
            .fillMaxWidth()
            .keyOutline(outline),
    ) {
        RockerHalf(RemoteFunction.VOLUME_UP, "Vol +", A1Icons.VolumeUp, bindings, connected, onPress)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(A1Dimens.Hairline)
                .background(borderColor(outline)),
        )
        RockerHalf(RemoteFunction.VOLUME_DOWN, "Vol −", A1Icons.VolumeDown, bindings, connected, onPress)
    }
}

@Composable
private fun ColumnScope.RockerHalf(
    function: RemoteFunction,
    label: String,
    icon: ImageVector,
    bindings: Map<RemoteFunction, KeyBinding>,
    connected: Boolean,
    onPress: (RemoteFunction) -> Unit,
) {
    val unverified = styleOf(bindings[function]) == KeyStyle.UNVERIFIED
    A1Key(
        label = label,
        icon = icon,
        showLabel = false,
        style = styleOf(bindings[function]),
        enabled = connected || unverified,
        onPress = { onPress(function) },
        caption = if (unverified) "Set up" else null,
        repeating = !unverified && function.repeatBehaviour() == RepeatBehaviour.REPEATING,
        // The rocker draws the outline; each half draws only its own press fill.
        bordered = false,
        modifier = Modifier.fillMaxWidth().weight(1f),
    )
}

/**
 * The circular D-pad: a ring of four arrow segments around a filled OK hub.
 *
 * The dial is one touch surface with the four arrows resolved by angle, not four buttons that
 * happen to be curved — which is why the arrows are findable by feel: anywhere on the upper
 * arc is Up, and the thumb never has to land on a target it cannot see. The per-arrow overlays
 * carry only labels for the accessibility tree; they consume nothing, so a press lands on the
 * geometry underneath either way.
 */
@Composable
private fun DPadDial(
    bindings: Map<RemoteFunction, KeyBinding>,
    connected: Boolean,
    onPress: (RemoteFunction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    var pressed by remember { mutableStateOf<RemoteFunction?>(null) }
    val repeatJob = remember { mutableStateOf<Job?>(null) }

    // A finger that leaves with the composable takes its auto-repeat with it.
    DisposableEffect(Unit) {
        onDispose {
            repeatJob.value?.cancel()
            repeatJob.value = null
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .alpha(if (connected) 1f else DISABLED_ALPHA)
                .pointerInput(connected) {
                    if (!connected) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val function = functionAt(
                            x = down.position.x,
                            y = down.position.y,
                            width = size.width.toFloat(),
                            height = size.height.toFloat(),
                        )
                        if (function != null) {
                            pressed = function
                            haptics.tick()
                            onPress(function)
                            if (function.repeatBehaviour() == RepeatBehaviour.REPEATING) {
                                repeatJob.value = scope.launch { AutoRepeat.run { onPress(function) } }
                            }
                        }
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            pressed = null
                            repeatJob.value?.cancel()
                            repeatJob.value = null
                        }
                    }
                },
        ) {
            // The distance from the dial's edge to the middle of the ring: where an arrow sits.
            val ringInset = maxWidth * (1f - HUB_RADIUS_FRACTION) / 4f
            val edgeInset = (ringInset - A1Dimens.MinTouch / 2).coerceAtLeast(0.dp)

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawDial(bindings, pressed)
            }

            DIAL_ARROWS.forEach { arrow ->
                ArrowKey(
                    arrow = arrow,
                    style = styleOf(bindings[arrow.function]),
                    connected = connected,
                    onPress = onPress,
                    modifier = Modifier.align(arrow.alignment).padding(arrow.edge(edgeInset)),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(maxWidth * HUB_RADIUS_FRACTION)
                    .dialSemantics("OK", connected) { onPress(RemoteFunction.OK) },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(text = "OK", style = A1Type.OkGlyph)
            }
        }
    }
}

/** One arrow of the ring: an icon at the ring's mid-radius, with the label the tree needs. */
@Composable
private fun ArrowKey(
    arrow: DialArrow,
    style: KeyStyle,
    connected: Boolean,
    onPress: (RemoteFunction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(A1Dimens.MinTouch)
            .dialSemantics(arrow.label, connected) { onPress(arrow.function) },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = arrow.icon,
            contentDescription = null,
            modifier = Modifier.size(A1Dimens.DpadIcon),
            colorFilter = ColorFilter.tint(labelColor(style)),
        )
    }
}

/**
 * The label, role and click action for one region of the dial.
 *
 * Semantics only — no pointer input — so a real touch falls through to the dial's own geometry
 * and is resolved by where it landed, not by which invisible box it happened to be inside.
 */
private fun Modifier.dialSemantics(label: String, enabled: Boolean, onPress: () -> Unit): Modifier = semantics {
    role = Role.Button
    contentDescription = label
    if (enabled) {
        onClick(label = label) {
            onPress()
            true
        }
    } else {
        disabled()
    }
}

/** Ring, dividers and hub. The pressed region — if any — is filled rather than outlined. */
private fun DrawScope.drawDial(bindings: Map<RemoteFunction, KeyBinding>, pressed: RemoteFunction?) {
    val hairline = A1Dimens.Hairline.toPx()
    val outer = size.minDimension / 2f - hairline / 2f
    val inner = size.minDimension / 2f * HUB_RADIUS_FRACTION

    SEGMENTS.forEach { (function, startAngle) ->
        val style = styleOf(bindings[function])
        drawSegment(
            outer = outer,
            inner = inner,
            startAngle = startAngle,
            paint = SegmentPaint(
                fill = if (pressed == function) A1Colors.Pressed else null,
                stroke = borderColor(style),
                dashed = style == KeyStyle.UNVERIFIED,
            ),
            hairline = hairline,
        )
    }

    // OK is the one primary action on this screen, so the hub carries the accent fill.
    drawCircle(
        color = if (pressed == RemoteFunction.OK) A1Colors.Pressed else A1Colors.Accent,
        radius = inner,
        center = center,
    )
}

/** One annular sector: the fill, its two arcs, and the two radial edges that close it. */
private fun DrawScope.drawSegment(
    outer: Float,
    inner: Float,
    startAngle: Float,
    paint: SegmentPaint,
    hairline: Float,
) {
    val fill = paint.fill
    if (fill != null) {
        val mid = (outer + inner) / 2f
        drawArc(
            color = fill,
            startAngle = startAngle,
            sweepAngle = SEGMENT_SWEEP,
            useCenter = false,
            topLeft = Offset(center.x - mid, center.y - mid),
            size = Size(mid * 2f, mid * 2f),
            style = Stroke(width = outer - inner),
        )
    }

    val effect = if (paint.dashed) PathEffect.dashPathEffect(floatArrayOf(DASH.toPx(), GAP.toPx()), 0f) else null
    listOf(outer, inner).forEach { radius ->
        drawArc(
            color = paint.stroke,
            startAngle = startAngle,
            sweepAngle = SEGMENT_SWEEP,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = hairline, pathEffect = effect),
        )
    }
    listOf(startAngle, startAngle + SEGMENT_SWEEP).forEach { angle ->
        val radians = Math.toRadians(angle.toDouble())
        val x = cos(radians).toFloat()
        val y = sin(radians).toFloat()
        drawLine(
            color = paint.stroke,
            start = Offset(center.x + x * inner, center.y + y * inner),
            end = Offset(center.x + x * outer, center.y + y * outer),
            strokeWidth = hairline,
            pathEffect = effect,
        )
    }
}

/**
 * Which part of the dial [position] landed on, or null for a miss outside the ring.
 *
 * Pure geometry, so the quadrant boundaries can be checked by hand: 0° is 3 o'clock and the
 * quadrants are hinged on the diagonals, which is what puts the whole top arc under Up.
 */
internal fun functionAt(x: Float, y: Float, width: Float, height: Float): RemoteFunction? {
    val radius = minOf(width, height) / 2f
    val dx = x - width / 2f
    val dy = y - height / 2f
    val distance = hypot(dx, dy)
    if (distance > radius) return null
    if (distance <= radius * HUB_RADIUS_FRACTION) return RemoteFunction.OK

    val degrees = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + FULL_TURN) % FULL_TURN
    return when {
        degrees < 45.0 || degrees >= 315.0 -> RemoteFunction.RIGHT
        degrees < 135.0 -> RemoteFunction.DOWN
        degrees < 225.0 -> RemoteFunction.LEFT
        else -> RemoteFunction.UP
    }
}

private const val FULL_TURN = 360.0

/** The hairline or dashed outline of a key-shaped block. */
private fun Modifier.keyOutline(style: KeyStyle): Modifier = if (style == KeyStyle.UNVERIFIED) {
    dashedBorder(A1Colors.UnverifiedBorder, A1Dimens.Hairline)
} else {
    border(A1Dimens.Hairline, A1Colors.KeyBorder, RectangleShape)
}

private fun borderColor(style: KeyStyle): Color =
    if (style == KeyStyle.UNVERIFIED) A1Colors.UnverifiedBorder else A1Colors.KeyBorder

private fun labelColor(style: KeyStyle): Color =
    if (style == KeyStyle.UNVERIFIED) A1Colors.UnverifiedLabel else A1Colors.Paper

/** DESIGN_SPEC: anything short of confirmed is drawn unverified. */
private fun styleOf(binding: KeyBinding?): KeyStyle =
    if (binding?.status == KeyStatus.CONFIRMED) KeyStyle.VERIFIED else KeyStyle.UNVERIFIED

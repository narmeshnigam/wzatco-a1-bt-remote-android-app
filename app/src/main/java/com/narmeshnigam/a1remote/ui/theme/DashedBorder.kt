package com.narmeshnigam.a1remote.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A dashed hairline rectangle: the unverified key of DESIGN_SPEC.
 *
 * Compose has no dashed `border`, and this style is load-bearing — it is the app saying it has
 * not proved this key on the A1 — so it gets drawn explicitly rather than approximated.
 */
fun Modifier.dashedBorder(color: Color, width: Dp = 1.dp, dash: Dp = 3.dp, gap: Dp = 3.dp): Modifier = drawBehind {
    val strokeWidth = width.toPx()
    val effect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()), 0f)
    val inset = strokeWidth / 2f
    drawRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        style = Stroke(width = strokeWidth, pathEffect = effect),
    )
}

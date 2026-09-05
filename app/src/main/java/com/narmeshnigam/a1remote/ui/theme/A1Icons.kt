package com.narmeshnigam.a1remote.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The key glyphs, transcribed from the SVG paths in
 * `docs/design/WZATCO A1 Remote - Prototype.dc.html`.
 *
 * Stroked, 24 × 24 viewport, 1.5 stroke width with round caps and joins, exactly as
 * DESIGN_SPEC specifies. They are drawn white and tinted at the call site.
 */
object A1Icons {
    val ChevronUp = strokeIcon("ChevronUp", "m18 15-6-6-6 6")
    val ChevronDown = strokeIcon("ChevronDown", "m6 9 6 6 6-6")
    val ChevronLeft = strokeIcon("ChevronLeft", "m15 18-6-6 6-6")
    val ChevronRight = strokeIcon("ChevronRight", "m9 18 6-6-6-6")

    val Power = strokeIcon("Power", "M12 3v9", "M18.4 6.6a9 9 0 1 1-12.8 0")

    val Back = strokeIcon("Back", "M9 14 4 9l5-5", "M4 9h10a6 6 0 0 1 0 12h-3")
    val Home = strokeIcon("Home", "m3 10 9-7 9 7v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1z")
    val Menu = strokeIcon("Menu", "M4 6h16", "M4 12h16", "M4 18h16")

    private const val SPEAKER = "M11 5 6 9H3v6h3l5 4z"
    val VolumeDown = strokeIcon("VolumeDown", SPEAKER, "M16 12h5")
    val VolumeUp = strokeIcon("VolumeUp", SPEAKER, "M16 12h5", "M18.5 9.5v5")
    val Mute = strokeIcon("Mute", SPEAKER, "m17 9 4 6", "m21 9-4 6")

    private const val LENS = "M11 4a7 7 0 1 0 0 14 7 7 0 1 0 0-14"
    val FocusDown = strokeIcon("FocusDown", LENS, "m20 20-4.5-4.5", "M8 11h6")
    val FocusUp = strokeIcon("FocusUp", LENS, "m20 20-4.5-4.5", "M8 11h6", "M11 8v6")

    val Source = strokeIcon("Source", "M4 7h13l-3-3", "M20 17H7l3 3")
    val Flip = strokeIcon("Flip", "M12 3v18", "M4 8h5v8H4z", "M15 8h5v8h-5z")
    val Keystone = strokeIcon("Keystone", "M6 5h12l3 14H3z")
    val Cursor = strokeIcon("Cursor", "m4 3 7 17 2-6 6-2z")

    private fun strokeIcon(name: String, vararg paths: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { data ->
            addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
}

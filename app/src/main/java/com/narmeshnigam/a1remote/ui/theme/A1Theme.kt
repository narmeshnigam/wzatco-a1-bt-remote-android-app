package com.narmeshnigam.a1remote.ui.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The palette of DESIGN_SPEC. These ten values are the whole palette — there are no others,
 * no gradients, no shadows and no rounded corners anywhere in this app.
 */
object A1Colors {
    /** Every remote screen background. */
    val Field = Color(0xFF1D2D3D)

    /** Primary text and icons on the field. */
    val Paper = Color(0xFFF2F2F3)

    /** The single primary action per screen. */
    val Accent = Color(0xFF5980A6)

    /** Key press fill. */
    val Pressed = Color(0xFF416180)

    /** Confirmed key outline, 1 dp. */
    val KeyBorder = Paper.copy(alpha = 0.35f)

    /** Keys not yet confirmed on the A1, dashed 1 dp. */
    val UnverifiedBorder = Color(0xFF94BCE3)

    /** Text and icon of an unverified key. */
    val UnverifiedLabel = Color(0xFFB5D9FD)

    /** Status sub-labels and hints. */
    val MutedLabel = Paper.copy(alpha = 0.45f)

    /** Link dot when a host is connected. */
    val LinkLive = Color(0xFF94BCE3)

    /** Link dot when registered with no host. */
    val LinkDead = Color(0xFF7A7A7D)

    /** Fill of the active bottom tab. */
    val TabActive = Color(0xFF2C455D)

    /** The 1 dp dividers between tab cells. */
    val TabDivider = Paper.copy(alpha = 0.15f)
}

/** The metrics of DESIGN_SPEC. */
object A1Dimens {
    val ScreenPadding = 18.dp
    val Gutter = 6.dp
    val KeyHeight = 62.dp
    val DpadBlock = 258.dp
    val DpadCell = 86.dp
    val PowerKey = 54.dp
    val TabBar = 58.dp

    /** Minimum touch target, both axes. */
    val MinTouch = 58.dp
    val Hairline = 1.dp
    val LinkDot = 9.dp
    val KeyIcon = 21.dp
    val DpadIcon = 26.dp
}

/** The type scale of DESIGN_SPEC. */
object A1Type {
    /**
     * Roboto Condensed where the platform exposes it by name (API 31+), the platform sans
     * elsewhere. DESIGN_SPEC allows either.
     */
    val Condensed: FontFamily =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            FontFamily(Font(DeviceFontFamilyName("sans-serif-condensed")))
        } else {
            FontFamily.SansSerif
        }

    val ScreenTitle = TextStyle(fontFamily = Condensed, fontSize = 25.sp, color = A1Colors.Paper)
    val HostName = TextStyle(fontFamily = Condensed, fontSize = 19.sp, color = A1Colors.Paper)
    val OkGlyph = TextStyle(
        fontFamily = Condensed,
        fontSize = 22.sp,
        letterSpacing = 0.1.em,
        color = A1Colors.Paper,
    )
    val KeyLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        letterSpacing = 0.1.em,
        color = A1Colors.Paper,
    )
    val StatusSubLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        letterSpacing = 0.12.em,
        color = A1Colors.MutedLabel,
    )
    val Body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, color = A1Colors.Paper)
    val Hint = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, color = A1Colors.MutedLabel)
    val Mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = A1Colors.MutedLabel)
    val TabLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 10.sp,
        letterSpacing = 0.12.em,
        color = A1Colors.Paper,
    )
    val StepLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 10.sp,
        letterSpacing = 0.14.em,
        color = A1Colors.MutedLabel,
    )

    /**
     * The function under test on the Key Lab screen: condensed 22 sp.
     *
     * Not [OkGlyph], which is the same size but tracked out 0.1 em — that spacing is what makes
     * a three-letter glyph read as a key, and it would make a function name read as a sign.
     */
    val LabTitle = TextStyle(fontFamily = Condensed, fontSize = 22.sp, color = A1Colors.Paper)
}

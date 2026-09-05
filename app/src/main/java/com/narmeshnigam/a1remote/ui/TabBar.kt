package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type

/**
 * The bottom tab bar of DESIGN_SPEC: 58 dp tall, four equal cells, 1 dp top border and 1 dp
 * dividers, active cell filled.
 */
@Composable
fun A1TabBar(current: A1Screen, onSelect: (A1Screen) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(A1Dimens.TabBar),
    ) {
        A1Screen.entries.forEachIndexed { index, screen ->
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (screen == current) A1Colors.TabActive else Color.Transparent,
                        RectangleShape,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelect(screen) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(text = screen.tabLabel.uppercase(), style = A1Type.TabLabel)
            }
            if (index < A1Screen.entries.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(A1Dimens.Hairline)
                        .height(A1Dimens.TabBar)
                        .background(A1Colors.TabDivider, RectangleShape),
                )
            }
        }
    }
}

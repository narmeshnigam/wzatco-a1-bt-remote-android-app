package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type

/**
 * A screen that exists in the tab bar but is not built yet.
 *
 * The tab bar's geometry is fixed by DESIGN_SPEC, so the cells are there from the start; this
 * says plainly which gate fills them rather than pretending they work.
 */
@Composable
fun PlaceholderScreen(title: String, body: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = A1Dimens.ScreenPadding),
        contentAlignment = Alignment.Center,
    ) {
        A1Panel(dashed = true) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicText(text = title, style = A1Type.ScreenTitle)
                BasicText(text = body, style = A1Type.Hint.copy(textAlign = TextAlign.Center))
            }
        }
    }
}

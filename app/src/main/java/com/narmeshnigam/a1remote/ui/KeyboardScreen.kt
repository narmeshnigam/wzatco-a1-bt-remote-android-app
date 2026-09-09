package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.narmeshnigam.a1remote.hid.TextTyping
import com.narmeshnigam.a1remote.service.SendResult
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type
import com.narmeshnigam.a1remote.vm.TextViewModel
import com.narmeshnigam.a1remote.vm.TypingProgress

/**
 * The draft box never shrinks past this, however little room the soft keyboard leaves.
 *
 * It takes the screen's spare height instead of a fixed size: with the keyboard up there is
 * barely any, and the box giving way is what keeps Send on screen — a Send key behind the
 * keyboard would make this screen useless.
 */
private val DRAFT_MIN_HEIGHT = 58.dp

/**
 * The keyboard screen: type on the phone, then send it into whatever field the projector has
 * focused (BUILD_SPEC §6).
 *
 * Nothing is transmitted as it is typed. The user writes the whole string, reads it back on a
 * screen that is in their hand rather than across the room, and then sends it — which is the
 * only way to check a password before it goes into a field that shows dots.
 */
@Composable
fun KeyboardScreen(modifier: Modifier = Modifier, viewModel: TextViewModel = viewModel()) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val link by viewModel.link.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()

    val untypable = viewModel.untypable
    val typing = progress != null
    val connected = link.isConnected
    val canSend = connected && draft.isNotEmpty() && untypable.isEmpty() && !typing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = A1Dimens.ScreenPadding)
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            BasicText(text = "Keyboard", style = A1Type.ScreenTitle)
            BasicText(
                text = "Focus a field on the projector first. Nothing goes out until you tap Send; " +
                    "Backspace, Space and Enter go straight there.",
                style = A1Type.Hint,
            )
        }

        DraftBox(
            draft = draft,
            enabled = !typing,
            onChange = viewModel::setDraft,
            modifier = Modifier.weight(1f).heightIn(min = DRAFT_MIN_HEIGHT),
        )

        BasicText(text = draftHint(draft, untypable), style = A1Type.Hint.copy(color = A1Colors.UnverifiedLabel))

        Row(
            modifier = Modifier.fillMaxWidth().height(A1Dimens.MinTouch),
            horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
        ) {
            A1Key(
                label = "Backspace",
                onPress = { viewModel.sendKeystroke(TextTyping.BACKSPACE) },
                enabled = connected && !typing,
                modifier = Modifier.weight(1f).height(A1Dimens.MinTouch),
            )
            A1Key(
                label = "Space",
                onPress = { viewModel.sendKeystroke(TextTyping.SPACE) },
                enabled = connected && !typing,
                modifier = Modifier.weight(1f).height(A1Dimens.MinTouch),
            )
            A1Key(
                label = "Enter",
                onPress = { viewModel.sendKeystroke(TextTyping.ENTER) },
                enabled = connected && !typing,
                modifier = Modifier.weight(1f).height(A1Dimens.MinTouch),
            )
        }

        BasicText(text = statusLine(note, progress, lastResult, connected), style = A1Type.Hint)

        Row(
            modifier = Modifier.fillMaxWidth().height(A1Dimens.KeyHeight),
            horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
        ) {
            A1Key(
                label = "Clear",
                onPress = viewModel::clearDraft,
                enabled = draft.isNotEmpty() && !typing,
                modifier = Modifier.weight(1f).height(A1Dimens.KeyHeight),
            )
            A1Key(
                label = if (typing) "Stop" else "Send text",
                style = KeyStyle.PRIMARY,
                onPress = { if (typing) viewModel.stop() else viewModel.sendDraft() },
                enabled = typing || canSend,
                modifier = Modifier.weight(2f).height(A1Dimens.KeyHeight),
            )
        }
    }
}

/** The draft itself. Multi-line, because a password is easier to check whole than scrolled. */
@Composable
private fun DraftBox(draft: String, enabled: Boolean, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(A1Dimens.Hairline, A1Colors.KeyBorder, RectangleShape)
            .padding(12.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onChange,
            enabled = enabled,
            textStyle = A1Type.Body,
            cursorBrush = SolidColor(A1Colors.UnverifiedLabel),
            // No autocorrect and no auto-capitalisation: a password the phone helpfully
            // "corrected" would be typed into the projector wrong, with nothing on screen to say so.
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Default,
            ),
            modifier = Modifier.fillMaxSize(),
            decorationBox = { field ->
                Box {
                    if (draft.isEmpty()) {
                        BasicText(text = "Type what the projector should receive", style = A1Type.Hint)
                    }
                    field()
                }
            },
        )
    }
}

/** The line under the box: how much there is to send, or what cannot be sent and why. */
private fun draftHint(draft: String, untypable: List<Char>): String = when {
    untypable.isNotEmpty() ->
        "Cannot type ${untypable.joinToString(" ") { "'$it'" }} — this keyboard sends US-layout " +
            "usages, and there is no key for that. Nothing is sent until it is gone."

    draft.isEmpty() ->
        "Letters, digits and US punctuation. What the A1 shows for punctuation is " +
            "its own layout's business — open question 19."

    else -> "${draft.length} ${if (draft.length == 1) "character" else "characters"} ready."
}

/** What happened to the last thing the user asked for, in one line. */
private fun statusLine(note: String?, progress: TypingProgress?, result: SendResult?, connected: Boolean): String =
    when {
        progress != null -> "Typing ${progress.sent} of ${progress.total}…"
        note != null -> note
        !connected -> "No host connected. Nothing can be typed until the A1 is on the other end."
        result == null -> "Nothing sent yet."
        result == SendResult.SENT -> "Sent. Watch the projector."
        result == SendResult.NO_SERVICE -> "Not sent: the HID service is not running."
        result == SendResult.NOT_CONNECTED -> "Not sent: no host is connected."
        result == SendResult.UNMAPPED -> "Not sent: nothing is mapped."
        result == SendResult.PERMISSION_DENIED -> "Not sent: a Bluetooth runtime permission is missing."
        else -> "Not sent: the stack refused the report."
    }

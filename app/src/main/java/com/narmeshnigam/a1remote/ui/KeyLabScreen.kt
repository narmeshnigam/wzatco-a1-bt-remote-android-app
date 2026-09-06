package com.narmeshnigam.a1remote.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.narmeshnigam.a1remote.data.Finding
import com.narmeshnigam.a1remote.data.Verdict
import com.narmeshnigam.a1remote.hid.KeyLabCandidates
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.hid.ReportKind
import com.narmeshnigam.a1remote.service.SendResult
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.ui.theme.A1Type
import com.narmeshnigam.a1remote.vm.KeyLabMode
import com.narmeshnigam.a1remote.vm.KeyLabState
import com.narmeshnigam.a1remote.vm.KeyLabViewModel

/** The name the export dialog opens with. */
private const val EXPORT_FILE = "a1-key-lab-findings.json"

/** The button-picker chips: shorter than a key, so the picker costs one compact row. */
private val CHIP_HEIGHT = 36.dp

/** The same wording as the keypad keys, so the operator is testing the key they can see. */
private fun labelOf(function: RemoteFunction): String = when (function) {
    RemoteFunction.FOCUS_UP -> "Focus +"
    RemoteFunction.FOCUS_DOWN -> "Focus −"
    RemoteFunction.SOURCE -> "Source"
    RemoteFunction.SCREEN_FLIP -> "Screen flip"
    RemoteFunction.KEYSTONE -> "Keystone"
    RemoteFunction.POWER -> "Power off"
    else -> function.name
}

/**
 * Key Lab: send a candidate usage, watch the projector, record what it did (BUILD_SPEC §6).
 *
 * Every verdict key is dead while the link is down. A verdict recorded against a report that
 * never left the phone would be a fact this app made up, and the findings file is the one
 * artefact of this project that has to be trustworthy.
 */
@Composable
fun KeyLabScreen(
    modifier: Modifier = Modifier,
    initialFunction: RemoteFunction? = null,
    onFunctionConsumed: () -> Unit = {},
    viewModel: KeyLabViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val findings by viewModel.findings.collectAsStateWithLifecycle()
    val link by viewModel.link.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()

    // Arriving from a "Set up" tap on the keypad: jump straight to that button.
    LaunchedEffect(initialFunction) {
        if (initialFunction != null) {
            viewModel.selectFunction(initialFunction)
            onFunctionConsumed()
        }
    }

    var exportNote by remember { mutableStateOf<String?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        exportNote = uri?.let { target -> writeFindings(context, target, viewModel.exportJson()) }
    }

    val armed = link.isConnected && state.candidate != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = A1Dimens.ScreenPadding)
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            BasicText(text = "Fix Keys", style = A1Type.ScreenTitle)
            BasicText(
                text = "Some A1 buttons ignore the standard codes. Pick a button, send codes until " +
                    "the projector reacts, then tap It worked.",
                style = A1Type.Hint,
            )
        }

        ButtonPicker(current = state.function, onSelect = viewModel::selectFunction)
        FunctionCard(state)
        ModeRow(state = state, connected = link.isConnected, viewModel = viewModel)
        VerdictList(findings, modifier = Modifier.weight(1f))
        StatusHint(result = lastResult, exportNote = exportNote)

        Row(horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter), modifier = Modifier.fillMaxWidth()) {
            A1Key(
                label = "Did something else",
                onPress = { viewModel.record(Verdict.SIDE_EFFECT) },
                enabled = armed,
                modifier = Modifier.weight(1f),
            )
            A1Key(
                label = "Export JSON",
                onPress = { export.launch(EXPORT_FILE) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter), modifier = Modifier.fillMaxWidth()) {
            A1Key(
                label = "Send",
                style = KeyStyle.PRIMARY,
                onPress = {
                    exportNote = null
                    viewModel.send()
                },
                enabled = armed,
                modifier = Modifier.weight(1f).height(A1Dimens.KeyHeight),
            )
            A1Key(
                label = "It worked",
                onPress = { viewModel.record(Verdict.MAPPED) },
                enabled = armed,
                modifier = Modifier.weight(1f).height(A1Dimens.KeyHeight),
            )
            A1Key(
                label = "No effect",
                onPress = { viewModel.record(Verdict.NO_EFFECT) },
                enabled = armed,
                modifier = Modifier.weight(1f).height(A1Dimens.KeyHeight),
            )
        }
    }
}

/** The horizontal picker of buttons to fix; the selected one is filled with the accent. */
@Composable
private fun ButtonPicker(current: RemoteFunction, onSelect: (RemoteFunction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
    ) {
        KeyLabCandidates.FUNCTIONS.forEach { function ->
            FixKeyChip(
                label = labelOf(function),
                selected = function == current,
                onClick = { onSelect(function) },
            )
        }
    }
}

/** One compact selector chip: bordered, accent-filled when picked. Shorter than a full key. */
@Composable
private fun FixKeyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(CHIP_HEIGHT)
            .background(if (selected) A1Colors.Accent else Color.Transparent, RectangleShape)
            .border(
                A1Dimens.Hairline,
                if (selected) A1Colors.Accent else A1Colors.KeyBorder,
                RectangleShape,
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = label.uppercase(), style = A1Type.KeyLabel)
    }
}

/** The button under test: solid hairline card, name in condensed 22 sp, position below. */
@Composable
private fun FunctionCard(state: KeyLabState) {
    A1Panel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = "Button · ${state.functionIndex + 1} of ${state.functionCount}".uppercase(),
                style = A1Type.StepLabel,
            )
            BasicText(text = labelOf(state.function), style = A1Type.LabTitle)
            BasicText(
                text = state.candidate
                    ?.let { candidate -> "${state.positionLabel} · ${candidate.describe()}" }
                    ?: state.positionLabel,
                style = A1Type.Body.copy(color = A1Colors.UnverifiedLabel),
            )
        }
    }
}

/** Candidates / Manual / Sweep, and whatever controls the chosen mode needs. */
@Composable
private fun ModeRow(state: KeyLabState, connected: Boolean, viewModel: KeyLabViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(A1Dimens.Gutter)) {
        Row(horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter), modifier = Modifier.fillMaxWidth()) {
            KeyLabMode.entries.forEach { mode ->
                A1Key(
                    label = mode.label,
                    style = if (state.mode == mode) KeyStyle.PRIMARY else KeyStyle.VERIFIED,
                    onPress = { viewModel.setMode(mode) },
                    enabled = mode != KeyLabMode.SWEEP || state.hasSweep,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        when (state.mode) {
            KeyLabMode.MANUAL -> ManualControls(state, viewModel)
            KeyLabMode.SWEEP -> SweepControls(state, connected, viewModel)
            KeyLabMode.CANDIDATES -> Unit
        }
    }
}

/**
 * Manual entry: pick the report, type the usage in hex.
 *
 * The consumer ceiling is printed on screen and not merely enforced, because an operator who
 * types 0x0500 and sees nothing happen deserves to know the app refused it, rather than
 * concluding that the projector ignored it.
 */
@Composable
private fun ManualControls(state: KeyLabState, viewModel: KeyLabViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(A1Dimens.Gutter)) {
        Row(horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter), modifier = Modifier.fillMaxWidth()) {
            ReportKind.entries.forEach { kind ->
                A1Key(
                    label = kind.wireName,
                    style = if (state.manual.kind == kind) KeyStyle.PRIMARY else KeyStyle.VERIFIED,
                    onPress = { viewModel.setManualKind(kind) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(A1Dimens.MinTouch)
                .border(A1Dimens.Hairline, A1Colors.KeyBorder, RectangleShape)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = state.manual.usageText,
                onValueChange = viewModel::setManualUsage,
                singleLine = true,
                textStyle = A1Type.Body,
                cursorBrush = SolidColor(A1Colors.UnverifiedLabel),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (state.manual.usageText.isEmpty()) {
                            BasicText(text = "Usage in hex, e.g. 022D", style = A1Type.Hint)
                        }
                        field()
                    }
                },
            )
        }
        BasicText(
            text = state.manual.error
                ?: "Keyboard 0x00–0xFF · consumer 0x0000–0x03FF, the range the descriptor declares.",
            style = A1Type.Hint.copy(color = A1Colors.UnverifiedLabel),
        )
    }
}

/** The walk: one key to start and stop it, and the index the operator calls out against. */
@Composable
private fun SweepControls(state: KeyLabState, connected: Boolean, viewModel: KeyLabViewModel) {
    val sweep = state.sweep ?: return
    Row(
        horizontalArrangement = Arrangement.spacedBy(A1Dimens.Gutter),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        A1Key(
            label = if (sweep.running) "Stop sweep" else "Start sweep",
            style = if (sweep.running) KeyStyle.PRIMARY else KeyStyle.VERIFIED,
            onPress = viewModel::toggleSweep,
            enabled = connected,
            modifier = Modifier.weight(1f),
        )
        BasicText(
            text = "700 ms between presses. Stop it the moment the projector reacts.",
            style = A1Type.Hint,
            modifier = Modifier.weight(1f),
        )
    }
}

/** The verdict list: dashed box, newest last, function · usage left and verdict right. */
@Composable
private fun VerdictList(findings: List<Finding>, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()

    // Newest last, so the operator's eye stays at the bottom where the next row will appear.
    LaunchedEffect(findings.size) { scroll.animateScrollTo(scroll.maxValue) }

    A1Panel(modifier = modifier.fillMaxWidth(), dashed = true) {
        Column(
            modifier = Modifier.fillMaxHeight().verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (findings.isEmpty()) {
                BasicText(text = "Nothing recorded yet.", style = A1Type.Hint)
            }
            findings.forEach { finding ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = "${labelOf(finding.function)} · ${finding.report.wireName} ${finding.usageHex}",
                        style = A1Type.Hint,
                        modifier = Modifier.weight(1f),
                    )
                    BasicText(text = finding.verdict.label, style = A1Type.Hint.copy(color = A1Colors.UnverifiedLabel))
                }
            }
        }
    }
}

/** Says what the last Send did, so "no effect" is only ever recorded against a report that went. */
@Composable
private fun StatusHint(result: SendResult?, exportNote: String?) {
    val text = exportNote ?: when (result) {
        null -> "Nothing sent yet."
        SendResult.SENT -> "Sent. Watch the projector."
        SendResult.NO_SERVICE -> "Not sent: the HID service is not running."
        SendResult.NOT_CONNECTED -> "Not sent: no host is connected."
        SendResult.UNMAPPED -> "Not sent: nothing is mapped."
        SendResult.PERMISSION_DENIED -> "Not sent: a Bluetooth runtime permission is missing."
        SendResult.FAILED -> "Not sent: the stack refused the report."
    }
    BasicText(text = text, style = A1Type.Hint)
}

/**
 * Writes the findings to the location the operator picked, and says what happened.
 *
 * `CreateDocument` needs no permission and no network — there is no `INTERNET` permission in
 * this manifest and there is not going to be one.
 */
private fun writeFindings(context: Context, uri: Uri, json: String): String = runCatching {
    val stream = context.contentResolver.openOutputStream(uri, "wt")
        ?: error("the chosen location could not be opened for writing")
    stream.use { out -> out.write(json.toByteArray()) }
    "Findings written."
}.getOrElse { failure -> "Export failed: ${failure.message ?: failure::class.simpleName}" }

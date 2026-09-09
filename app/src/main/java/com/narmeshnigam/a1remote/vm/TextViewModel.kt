package com.narmeshnigam.a1remote.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.narmeshnigam.a1remote.hid.Keystroke
import com.narmeshnigam.a1remote.hid.TextTyping
import com.narmeshnigam.a1remote.service.HidLink
import com.narmeshnigam.a1remote.service.LinkState
import com.narmeshnigam.a1remote.service.SendResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * How far a run of typing has got. Null when nothing is being typed.
 *
 * @param sent keystrokes already on the wire
 * @param total keystrokes in the run
 */
data class TypingProgress(val sent: Int, val total: Int)

/**
 * The keyboard screen's view of the link (BUILD_SPEC §6).
 *
 * The draft is held here rather than in the composable because a run of typing outlives a
 * recomposition and has to be stoppable: it is a coroutine walking a string one report at a
 * time, and the user has to be able to end it the moment the projector shows the wrong thing.
 *
 * A plain [ViewModel], not an `AndroidViewModel`: nothing here needs a `Context`, and without
 * one the typing loop is testable on virtual time with a fake transport underneath it.
 */
class TextViewModel : ViewModel() {

    val link: StateFlow<LinkState> = HidLink.state

    private val _draft = MutableStateFlow("")

    /** What the user has typed on the phone. Nothing here has been transmitted. */
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _progress = MutableStateFlow<TypingProgress?>(null)

    /** Where a run of typing has got to, or null when the app is not typing. */
    val progress: StateFlow<TypingProgress?> = _progress.asStateFlow()

    private val _lastResult = MutableStateFlow<SendResult?>(null)
    val lastResult: StateFlow<SendResult?> = _lastResult.asStateFlow()

    private val _note = MutableStateFlow<String?>(null)

    /** The one line under the keys: what happened to the last thing the user asked for. */
    val note: StateFlow<String?> = _note.asStateFlow()

    private var run: Job? = null

    /** The characters of the draft this keyboard has no key for. Empty when it can type it all. */
    val untypable: List<Char> get() = TextTyping.untypable(_draft.value)

    fun setDraft(text: String) {
        _draft.value = text
        _note.value = null
    }

    fun clearDraft() {
        _draft.value = ""
        _note.value = null
        _lastResult.value = null
    }

    /** One named key — Backspace, Space, Enter, Tab — straight onto the wire. */
    fun sendKeystroke(keystroke: Keystroke) {
        _lastResult.value = HidLink.sendPress(keystroke.report, keystroke.label)
        _note.value = null
    }

    /**
     * Types the draft into whatever the projector currently has focused, one keystroke at a time.
     *
     * The run stops at the first report the stack refuses rather than carrying on: the user needs
     * to know the field holds a partial string, and finishing the run would only hide it.
     */
    fun sendDraft() {
        if (run?.isActive == true) return
        val text = _draft.value
        val keystrokes = TextTyping.keystrokesFor(text)
        if (text.isEmpty() || keystrokes == null) return

        run = viewModelScope.launch {
            _note.value = null
            _progress.value = TypingProgress(sent = 0, total = keystrokes.size)
            keystrokes.forEachIndexed { index, keystroke ->
                val result = HidLink.sendPress(keystroke.report, keystroke.label)
                _lastResult.value = result
                if (result != SendResult.SENT) {
                    _note.value = "Stopped after $index of ${keystrokes.size}. The field holds only that much."
                    _progress.value = null
                    return@launch
                }
                _progress.value = TypingProgress(sent = index + 1, total = keystrokes.size)
                // The host has to see each key-up before the next key-down or it coalesces them.
                // 40 ms is this app's chosen default, not a measurement of the A1 — how fast the
                // projector really keeps up is open question 11.
                delay(KEYSTROKE_GAP_MS)
            }
            _progress.value = null
            _note.value = "Typed ${keystrokes.size} ${if (keystrokes.size == 1) "character" else "characters"}."
        }
    }

    /** Ends a run part-way. What already went out has already gone; nothing is undone. */
    fun stop() {
        if (run?.isActive != true) return
        run?.cancel()
        val sent = _progress.value?.sent ?: 0
        _progress.value = null
        _note.value = "Stopped. $sent went out; nothing sent can be taken back."
    }

    override fun onCleared() {
        run?.cancel()
        super.onCleared()
    }

    private companion object {
        /** Gap between one key-up and the next key-down. */
        const val KEYSTROKE_GAP_MS = 40L
    }
}

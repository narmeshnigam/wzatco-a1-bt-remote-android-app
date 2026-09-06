package com.narmeshnigam.a1remote.vm

import com.narmeshnigam.a1remote.data.FindingsStore
import com.narmeshnigam.a1remote.data.KeyMap
import com.narmeshnigam.a1remote.data.Verdict
import com.narmeshnigam.a1remote.data.finding
import com.narmeshnigam.a1remote.hid.KeyLabCandidate
import com.narmeshnigam.a1remote.hid.KeyLabCandidates
import com.narmeshnigam.a1remote.hid.KeyLabSweep
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.hid.ReportKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Files what the projector did and, on a hit, promotes the usage into the key map at once.
 *
 * The promotion is immediate so the keypad key flips from unverified to verified while the
 * operator is still standing in front of the projector — that flip is the app saying it now
 * knows something it did not know a second ago.
 *
 * Top-level rather than a method on [KeyLabViewModel] because the hit path is the one thing in
 * Key Lab that must not be wrong, and a view model needs an `Application` that a unit test does
 * not have.
 */
internal suspend fun fileVerdict(
    function: RemoteFunction,
    candidate: KeyLabCandidate,
    verdict: Verdict,
    keyMap: KeyMap,
    store: FindingsStore,
) {
    store.add(candidate.finding(function, verdict))
    if (verdict == Verdict.MAPPED) {
        keyMap.promote(function, candidate.report, candidate.usageName)
    }
}

/** The three ways of choosing what Send transmits, all three required by KEY_LAB.md. */
enum class KeyLabMode(val label: String) {
    /** Walk the candidate list for this function, in the order KEY_LAB.md gives it. */
    CANDIDATES("Suggested"),

    /** Type any usage by hand. */
    MANUAL("Type code"),

    /** Walk a declared range with a fixed gap, watching for a reaction. */
    SWEEP("Auto-scan"),
}

/**
 * A usage the operator typed.
 *
 * The range check is not a formality. The report descriptor declares consumer usages
 * 0x0000–0x03FF and nothing wider, because a full 16-bit range makes the host's parser give up
 * on the whole descriptor — see the KDoc on `HidDescriptor.CONSUMER_USAGE_MAX`. A usage above
 * the ceiling cannot be sent, so it is refused here with the reason said out loud rather than
 * thrown as an exception from the report builder.
 */
data class ManualEntry(val kind: ReportKind = ReportKind.CONSUMER, val usageText: String = "") {

    /** The typed usage, or null when the text is not a usage this descriptor can carry. */
    val usage: Int? = usageText.trim().removePrefix(HEX_PREFIX).removePrefix(HEX_PREFIX_UPPER)
        .toIntOrNull(radix = HEX_RADIX)
        ?.takeIf { it in 0..kind.usageMax }

    /** Why the text was refused, in words meant for the operator. Null when it is usable. */
    val error: String? = when {
        usageText.isBlank() -> null
        usage != null -> null
        kind == ReportKind.KEYBOARD -> "Keyboard usages are 0x00–0xFF."
        else ->
            "Consumer usages are 0x0000–0x03FF. The descriptor declares no more than that: a " +
                "wider range stops the host parsing it at all, so a higher usage cannot be sent."
    }

    /** What Send would transmit, or null when there is nothing valid typed. */
    val candidate: KeyLabCandidate? = usage?.let { KeyLabCandidate(kind, it, "manual entry") }

    private companion object {
        const val HEX_PREFIX = "0x"
        const val HEX_PREFIX_UPPER = "0X"
        const val HEX_RADIX = 16
    }
}

/** Where a sweep has got to, and whether it is currently stepping. */
data class SweepState(val sweep: KeyLabSweep, val index: Int, val running: Boolean) {
    val candidate: KeyLabCandidate get() = sweep.candidateAt(index)
    val isLast: Boolean get() = index == sweep.size - 1
}

/**
 * Everything the Key Lab screen draws, and everything a test needs to assert.
 *
 * [candidate] is the single source of what Send transmits, whichever mode is showing, so the
 * verdict the operator records is always about the usage that actually went out.
 */
data class KeyLabState(
    val function: RemoteFunction,
    val functionIndex: Int,
    val functionCount: Int,
    val mode: KeyLabMode,
    val candidateIndex: Int,
    val candidates: List<KeyLabCandidate>,
    val manual: ManualEntry,
    val sweep: SweepState?,
) {
    /** The usage Send would transmit, or null when this mode has nothing ready. */
    val candidate: KeyLabCandidate?
        get() = when (mode) {
            KeyLabMode.CANDIDATES -> candidates.getOrNull(candidateIndex)
            KeyLabMode.MANUAL -> manual.candidate
            KeyLabMode.SWEEP -> sweep?.candidate
        }

    /** `Code 2 of 4` — the left half of the line under the button name. */
    val positionLabel: String
        get() = when (mode) {
            KeyLabMode.CANDIDATES -> if (candidates.isEmpty()) {
                "No suggested codes"
            } else {
                "Code ${candidateIndex + 1} of ${candidates.size}"
            }

            KeyLabMode.MANUAL -> "Typed code"
            KeyLabMode.SWEEP -> sweep?.let { "Scan ${it.index + 1} of ${it.sweep.size} · ${it.sweep.describe()}" }
                ?: "No scan for this button"
        }

    /** True when this function has a range KEY_LAB.md says to sweep. */
    val hasSweep: Boolean get() = sweep != null
}

/**
 * The Key Lab protocol of KEY_LAB.md as a state machine, with no Android in it.
 *
 * The whole of "what is under test and what happens to a verdict" lives here so it can be
 * tested without a projector, a phone or a view model. Sending, promoting and persisting are
 * the view model's job; deciding what moves where is this.
 */
class KeyLabSession(private val order: List<RemoteFunction> = KeyLabCandidates.FUNCTIONS) {

    init {
        require(order.isNotEmpty()) { "Key Lab needs at least one function under test" }
    }

    private val _state = MutableStateFlow(stateFor(functionIndex = 0))
    val state: StateFlow<KeyLabState> = _state.asStateFlow()

    /** Jumps straight to [function], back at its first candidate. */
    fun selectFunction(function: RemoteFunction) {
        val index = order.indexOf(function)
        require(index >= 0) { "$function is not under test" }
        _state.value = stateFor(index)
    }

    /**
     * Moves to the next function and starts it from its first candidate.
     *
     * This is what a hit does, and what an exhausted list does. It wraps, so a second pass over
     * a function that stayed unmapped costs no navigation; the `Function n of m` line means the
     * operator can always see where the pass has got to.
     */
    fun nextFunction() {
        _state.value = stateFor((_state.value.functionIndex + 1) % order.size)
    }

    /**
     * Moves past the candidate that just failed.
     *
     * In candidate mode that is the next entry of the list, then the function's sweep if
     * KEY_LAB.md gives it one, then the next function. In sweep mode it is the next index. In
     * manual mode nothing moves: the operator chooses what to type next.
     */
    fun nextCandidate() {
        val current = _state.value
        _state.value = when (current.mode) {
            KeyLabMode.MANUAL -> current
            KeyLabMode.SWEEP ->
                current.sweep
                    ?.takeIf { !it.isLast }
                    ?.let { current.copy(sweep = it.copy(index = it.index + 1)) }
                    ?: return nextFunction()

            KeyLabMode.CANDIDATES -> when {
                current.candidateIndex + 1 < current.candidates.size ->
                    current.copy(candidateIndex = current.candidateIndex + 1)

                current.hasSweep -> current.copy(mode = KeyLabMode.SWEEP)
                else -> return nextFunction()
            }
        }
    }

    /**
     * Moves past the candidate a [verdict] was just recorded against, and hands it back so the
     * caller can file it and, on a hit, promote it.
     *
     * The decision of what a verdict *means* for the pass lives here rather than in the view
     * model so it can be tested without a device: a hit ends this function, and anything else —
     * nothing at all, or the projector doing something that was not the function under test —
     * moves on to the next candidate.
     */
    fun advancePast(verdict: Verdict): KeyLabCandidate? {
        val candidate = _state.value.candidate ?: return null
        if (verdict == Verdict.MAPPED) nextFunction() else nextCandidate()
        return candidate
    }

    /** Switches mode. A sweep always restarts from its low end and is never left running. */
    fun setMode(mode: KeyLabMode) {
        val current = _state.value
        if (mode == KeyLabMode.SWEEP && !current.hasSweep) return
        _state.value = current.copy(
            mode = mode,
            sweep = current.sweep?.copy(index = 0, running = false),
        )
    }

    fun setManualKind(kind: ReportKind) {
        _state.value = _state.value.let { it.copy(manual = it.manual.copy(kind = kind)) }
    }

    fun setManualUsage(text: String) {
        _state.value = _state.value.let { it.copy(manual = it.manual.copy(usageText = text)) }
    }

    /** Starts or stops the walk. The view model is what actually paces it. */
    fun setSweepRunning(running: Boolean) {
        _state.value = _state.value.let { it.copy(sweep = it.sweep?.copy(running = running)) }
    }

    /**
     * Steps the sweep on by one.
     *
     * Returns false at the top of the range, having stopped the walk — the operator has seen
     * every usage in it and the next thing to happen is a decision, not another report.
     */
    fun advanceSweep(): Boolean {
        val sweep = _state.value.sweep ?: return false
        if (sweep.isLast) {
            setSweepRunning(false)
            return false
        }
        _state.value = _state.value.copy(sweep = sweep.copy(index = sweep.index + 1))
        return true
    }

    private fun stateFor(functionIndex: Int): KeyLabState {
        val function = order[functionIndex]
        val sweep = KeyLabCandidates.sweepFor(function)
        val candidates = KeyLabCandidates.listFor(function)
        return KeyLabState(
            function = function,
            functionIndex = functionIndex,
            functionCount = order.size,
            mode = if (candidates.isEmpty() && sweep != null) KeyLabMode.SWEEP else KeyLabMode.CANDIDATES,
            candidateIndex = 0,
            candidates = candidates,
            manual = ManualEntry(),
            sweep = sweep?.let { SweepState(it, index = 0, running = false) },
        )
    }
}

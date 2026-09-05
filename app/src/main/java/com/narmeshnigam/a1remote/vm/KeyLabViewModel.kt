package com.narmeshnigam.a1remote.vm

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.narmeshnigam.a1remote.BuildConfig
import com.narmeshnigam.a1remote.data.DataStoreFindingsStore
import com.narmeshnigam.a1remote.data.Finding
import com.narmeshnigam.a1remote.data.Findings
import com.narmeshnigam.a1remote.data.FindingsJson
import com.narmeshnigam.a1remote.data.FindingsStore
import com.narmeshnigam.a1remote.data.KeyMaps
import com.narmeshnigam.a1remote.data.Verdict
import com.narmeshnigam.a1remote.data.finding
import com.narmeshnigam.a1remote.hid.KeyLabCandidates
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.hid.ReportKind
import com.narmeshnigam.a1remote.service.HidLink
import com.narmeshnigam.a1remote.service.LinkState
import com.narmeshnigam.a1remote.service.SendResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** KEY_LAB.md: a sweep walks its range with a 700 ms gap. */
private const val SWEEP_GAP_MS = 700L

/** The `host` field of the findings file. The projector this app exists for. */
private const val HOST = "WZATCO A1"

/**
 * Key Lab: try a usage, watch the projector, record what it did (BUILD_SPEC §3).
 *
 * The state machine is [KeyLabSession] and is testable on its own. What is here is the part
 * that needs a device: transmitting, promoting a hit into the process-wide key map so the
 * keypad key flips to verified while the operator is still looking at the projector, and
 * keeping the findings.
 */
class KeyLabViewModel(application: Application) : AndroidViewModel(application) {

    private val keyMap = KeyMaps.get(application)
    private val store: FindingsStore = DataStoreFindingsStore(application)
    private val session = KeyLabSession()
    private var sweepJob: Job? = null

    val state: StateFlow<KeyLabState> = session.state
    val link: StateFlow<LinkState> = HidLink.state

    val findings: StateFlow<List<Finding>> =
        store.findings.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _lastResult = MutableStateFlow<SendResult?>(null)

    /**
     * What the last Send actually did.
     *
     * Shown on the screen because a verdict is only worth recording if the report went out. An
     * operator who marks "no effect" on a press that never left the phone has poisoned the
     * findings file with a fact that is not one.
     */
    val lastResult: StateFlow<SendResult?> = _lastResult.asStateFlow()

    /** Transmits the current candidate as one key-down/key-up pair, and nothing else. */
    fun send() {
        val candidate = session.state.value.candidate ?: return
        _lastResult.value = HidLink.sendPress(candidate.report, candidate.describe())
    }

    /**
     * Records what the operator saw and moves on.
     *
     * A hit is promoted into the key map immediately — the operator has just watched it work,
     * which is the only evidence this app accepts — and the pass moves to the next function. A
     * miss or a side effect moves to the next candidate: a side effect means the projector did
     * *something*, which is worth keeping, but it was not the function under test.
     */
    fun record(verdict: Verdict) {
        stopSweep()
        val function = session.state.value.function
        val candidate = session.advancePast(verdict) ?: return
        viewModelScope.launch { fileVerdict(function, candidate, verdict, keyMap, store) }
    }

    fun selectFunction(function: RemoteFunction) {
        stopSweep()
        session.selectFunction(function)
    }

    fun setMode(mode: KeyLabMode) {
        stopSweep()
        session.setMode(mode)
    }

    fun setManualKind(kind: ReportKind) = session.setManualKind(kind)

    fun setManualUsage(text: String) = session.setManualUsage(text)

    /** Starts or stops the walk. Each step is one press, then [SWEEP_GAP_MS] of watching. */
    fun toggleSweep() {
        if (session.state.value.sweep?.running == true) {
            stopSweep()
            return
        }
        session.setSweepRunning(true)
        sweepJob = viewModelScope.launch {
            while (session.state.value.sweep?.running == true) {
                send()
                delay(SWEEP_GAP_MS)
                if (!session.advanceSweep()) return@launch
            }
        }
    }

    /** The findings file, ready to write. */
    fun exportJson(): String = FindingsJson.encode(
        Findings(
            appVersion = BuildConfig.VERSION_NAME,
            phone = "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}",
            host = HOST,
            recordedAt = OffsetDateTime.now()
                .truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            results = findings.value + untested,
        ),
    )

    /**
     * An `untested` row for every function nobody has tried yet.
     *
     * So the file always carries a verdict for all of them: a function missing from the results
     * is indistinguishable from one that was tested and forgotten, and "nobody has tried this"
     * is itself a finding.
     */
    private val untested: List<Finding>
        get() {
            val tested = findings.value.map(Finding::function).toSet()
            return KeyLabCandidates.FUNCTIONS
                .filterNot(tested::contains)
                .mapNotNull { function ->
                    KeyLabCandidates.listFor(function).firstOrNull()?.finding(function, Verdict.UNTESTED)
                }
        }

    private fun stopSweep() {
        sweepJob?.cancel()
        sweepJob = null
        session.setSweepRunning(false)
    }
}

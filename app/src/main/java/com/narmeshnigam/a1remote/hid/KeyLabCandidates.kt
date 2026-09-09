package com.narmeshnigam.a1remote.hid

/**
 * One thing Key Lab can try: a report, the usage inside it, and the usage's name.
 *
 * Nothing here is a claim about the A1. These are USB HID Usage Tables entries and the guesses
 * KEY_LAB.md records as guesses; whether the projector honours any of them is exactly what the
 * operator is about to find out.
 */
data class KeyLabCandidate(val kind: ReportKind, val usage: Int, val usageName: String) {

    /** The report Send transmits, built once so a candidate can be compared by value. */
    val report: HidReport = kind.report(usage)

    /** `Consumer 0x022D Zoom In` — the line the screen and the wire log both show. */
    fun describe(): String = "${kind.wireName.replaceFirstChar(Char::titlecase)} ${hexUsage()} $usageName"

    /** The usage as the findings file writes it. */
    fun hexUsage(): String = kind.hex(usage)
}

/**
 * A contiguous run of usages the operator walks with a fixed gap, watching the projector.
 *
 * KEY_LAB.md lists two of these among the candidates. They are not candidates in the same
 * sense — nobody expects a particular entry to be the answer, and there are twelve and sixteen
 * of them — so they are walked by index rather than listed one press at a time.
 */
class KeyLabSweep(val kind: ReportKind, val first: Int, val last: Int, private val nameOf: (Int) -> String) {

    init {
        require(first <= last) { "sweep runs upward: ${kind.hex(first)}..${kind.hex(last)}" }
        require(last <= kind.usageMax) {
            "sweep past the declared ${kind.wireName} range: ${kind.hex(last)} > ${kind.hex(kind.usageMax)}"
        }
    }

    /** How many usages the sweep covers, both ends included. */
    val size: Int get() = last - first + 1

    /** The candidate at [index], counting from the low end of the range. */
    fun candidateAt(index: Int): KeyLabCandidate {
        require(index in 0 until size) { "sweep index $index outside 0..${size - 1}" }
        val usage = first + index
        return KeyLabCandidate(kind, usage, nameOf(usage))
    }

    /** `0x0180–0x018F` — the range as the screen labels it. */
    fun describe(): String = "${kind.hex(first)}–${kind.hex(last)}"
}

/**
 * The candidate lists of KEY_LAB.md, transcribed in the order that file gives them.
 *
 * The order matters: the operator works down a list, and the first entry of each is the guess
 * the shipped key map already carries, so trying it first either confirms the default or rules
 * it out before anything else is touched.
 */
object KeyLabCandidates {

    /**
     * The functions Key Lab exists to resolve, in the order the operator works through them.
     *
     * Power off is the last one left. Focus ±, Source, Screen flip and Keystone were taken off
     * the remote because nothing the app shipped for them moved the projector; most of their
     * candidate codes were never tried, and what is still unknown about them lives in
     * `docs/OPEN_QUESTIONS.md` rather than in a button.
     */
    val FUNCTIONS: List<RemoteFunction> = listOf(RemoteFunction.POWER)

    /** Keyboard 0x68..0x73. KEY_LAB.md names this range F13–F24, so the entries are named that. */
    val HIGH_FUNCTION_KEY_SWEEP = KeyLabSweep(ReportKind.KEYBOARD, 0x68, 0x73) { usage -> "F${usage - 0x68 + 13}" }

    /**
     * Keyboard 0x3A..0x45 — F1–F12.
     *
     * The range worth walking when a usage reaches the A1 and no handler wants it: a vendor
     * function bound to a plain function key. F1–F12 is inert on Android otherwise, so the
     * sweep cannot type, navigate or switch anything off while it runs.
     */
    val LOW_FUNCTION_KEY_SWEEP = KeyLabSweep(ReportKind.KEYBOARD, 0x3A, 0x45) { usage -> "F${usage - 0x3A + 1}" }

    /**
     * Consumer 0x0180..0x018F.
     *
     * KEY_LAB.md gives this range no names, so neither does this: an entry is called by its
     * code. Inventing usage names for it would be inventing the very facts the sweep exists to
     * discover.
     */
    val CONSUMER_SWEEP = KeyLabSweep(ReportKind.CONSUMER, 0x0180, 0x018F) { usage -> "Consumer 0x%04X".format(usage) }

    private val lists: Map<RemoteFunction, List<KeyLabCandidate>> = mapOf(
        RemoteFunction.POWER to listOf(
            consumer(0x0030, "Power"),
            consumer(0x0032, "Sleep"),
            keyboard(0x66, "Power"),
        ),
    )

    /**
     * Which function gets which sweep — empty, and deliberately so.
     *
     * The four functions that had sweeps are gone, and KEY_LAB.md refuses Power one for a
     * reason worth keeping: a walk that stepped onto a working power-off would take the
     * projector down before the operator could say which code did it. The ranges above and the
     * machinery that walks them stay, so attaching a sweep to a new function is one line.
     */
    private val sweeps: Map<RemoteFunction, KeyLabSweep> = emptyMap()

    /** The named candidates for [function], in order. Empty for the ones already confirmed. */
    fun listFor(function: RemoteFunction): List<KeyLabCandidate> = lists[function].orEmpty()

    /** The sweep KEY_LAB.md attaches to [function], or null when it names none. */
    fun sweepFor(function: RemoteFunction): KeyLabSweep? = sweeps[function]

    private fun keyboard(usage: Int, name: String) = KeyLabCandidate(ReportKind.KEYBOARD, usage, name)

    private fun consumer(usage: Int, name: String) = KeyLabCandidate(ReportKind.CONSUMER, usage, name)
}

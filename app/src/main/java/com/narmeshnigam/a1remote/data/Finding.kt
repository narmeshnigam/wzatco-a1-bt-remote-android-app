package com.narmeshnigam.a1remote.data

import com.narmeshnigam.a1remote.hid.KeyLabCandidate
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.hid.ReportKind

/**
 * What the operator saw when one candidate was sent to the projector.
 *
 * The vocabulary is fixed by KEY_LAB.md, and there is no fifth value on purpose: "probably" is
 * not a finding.
 */
enum class Verdict(val wireName: String, val label: String) {
    /** The projector did the function under test. This one is the answer. */
    MAPPED("mapped", "Worked"),

    /** The projector did nothing at all. */
    NO_EFFECT("no_effect", "No effect"),

    /** The projector did something, but not this function — always worth recording. */
    SIDE_EFFECT("side_effect", "Side effect"),

    /** Carried by a candidate nobody has tried yet. */
    UNTESTED("untested", "Untested"),
    ;

    companion object {
        fun byWireName(name: String): Verdict? = entries.firstOrNull { it.wireName == name }
    }
}

/**
 * One row of the findings file: a function, the exact usage that was sent, and what happened.
 *
 * Field names mirror the JSON schema in KEY_LAB.md one for one, so the encoder has nothing to
 * translate and a reader of either can find the other.
 */
data class Finding(
    val function: RemoteFunction,
    val report: ReportKind,
    val usage: Int,
    val usageName: String,
    val verdict: Verdict,
    val note: String? = null,
) {
    /** The usage as the findings file writes it: `0x022D`, or `0x3C` for a keyboard usage. */
    val usageHex: String get() = report.hex(usage)
}

/** Records what [candidate] did, so a verdict is always about a usage that was actually sent. */
fun KeyLabCandidate.finding(function: RemoteFunction, verdict: Verdict, note: String? = null): Finding =
    Finding(function, kind, usage, usageName, verdict, note)

/**
 * A whole findings file.
 *
 * [recordedAt] is ISO-8601 with an offset, as in the schema. It is a string rather than a
 * timestamp because it is written once at export and never arithmetic'd, and because keeping it
 * a string leaves this file free of any platform clock.
 */
data class Findings(
    val appVersion: String,
    val phone: String,
    val host: String,
    val recordedAt: String,
    val results: List<Finding>,
)

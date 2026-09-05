package com.narmeshnigam.a1remote.hid

/**
 * Turns one press into the key-down/key-up pair BUILD_SPEC §4 requires, and guarantees the
 * key-up.
 *
 * This exists as a pure unit rather than as three lines inside the service so the guarantee is
 * testable without a projector. A key left held down on the A1 cannot be released from the
 * phone — the report has already gone — so the up has to survive the down failing, throwing, or
 * the caller being cancelled.
 *
 * @param sink transmits one report and returns whether the stack accepted it
 */
class KeyPressSender(private val sink: (HidReport) -> Boolean) {

    /** The outcome of one press. [down] is what the caller cares about; [up] is the guarantee. */
    data class Outcome(val down: Boolean, val up: Boolean)

    /**
     * Sends [report] and then its matching release.
     *
     * If [sink] throws on the key-down the release is still sent and the exception propagates:
     * the caller should hear about the failure, but not before the projector is safe.
     */
    fun press(report: HidReport): Outcome {
        val release = HidReports.releaseFor(report)
        var down = false
        var up = false
        try {
            down = sink(report)
        } finally {
            up = runCatching { sink(release) }.getOrDefault(false)
        }
        return Outcome(down, up)
    }
}

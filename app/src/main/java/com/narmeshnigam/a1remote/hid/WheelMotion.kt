package com.narmeshnigam.a1remote.hid

/**
 * A drag along the scroll strip, translated into relative wheel reports.
 *
 * The same problem as [CursorMotion] in one dimension: the strip delivers small floating-point
 * deltas, one per touch frame, and the wire takes whole 8-bit signed numbers. Truncating each
 * frame independently would throw away every sub-unit frame, so a slow scroll would send nothing;
 * the fraction is carried in [residue] and spent on a later frame instead.
 *
 * Direction: a downward drag on the strip (positive `dy`) must scroll the content down. On the
 * A1 that is a positive wheel value — the negative sign the HID convention suggests scrolled
 * the wrong way on hardware (2026-09-06), so the sign here follows the projector, not the spec.
 *
 * An instance is stateful and belongs to one strip. Call [reset] when a gesture begins.
 *
 * @param gain how many wheel units a pixel of strip travel is worth (BUILD_SPEC §6 sensitivity).
 */
class WheelMotion(private val gain: Float = SCROLL_GAIN) {

    private var residue = 0f

    /** Drops the carried fraction. Call at the start of a gesture. */
    fun reset() {
        residue = 0f
    }

    /**
     * Accelerates one touch-frame delta and returns the wheel report that carries it, or null
     * when the accumulated travel is still under a whole unit — nothing is lost, it waits in the
     * residue until it is worth a report.
     */
    fun step(dy: Float): HidReport? {
        // Pointer input never produces these, but a NaN residue would poison every later frame.
        if (!dy.isFinite()) return null

        residue += dy * gain
        val ticks = residue.toInt()
        residue -= ticks

        if (ticks == 0) return null
        return HidReports.mouse(wheel = ticks)
    }

    companion object {
        /**
         * Strip travel to wheel units. Deliberately gentle — on the A1 the wheel moves several
         * lines per tick, so a small gain keeps a full-strip drag from overshooting the list.
         */
        const val SCROLL_GAIN = 0.045f
    }
}

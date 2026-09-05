package com.narmeshnigam.a1remote.hid

import kotlin.math.abs

/**
 * One drag, translated into relative mouse reports (BUILD_SPEC §6).
 *
 * A drag arrives as a stream of small floating-point deltas, one per touch frame, and the wire
 * takes whole 8-bit signed numbers. Two things go wrong if that conversion is naive:
 *
 * - Truncating each frame independently throws the fraction away every time, so a slow, precise
 *   drag — every frame under one pixel of gained travel — produces no motion at all. The
 *   fraction is therefore carried in [residueX] / [residueY] and spent on a later frame.
 * - A fast flick can gain more than ±127 in a single frame. Clamping would silently shorten the
 *   movement, and repeatedly, so the projector's pointer would fall behind the thumb and stay
 *   behind. Such a frame is instead **split** across several reports whose deltas sum to exactly
 *   the requested travel (see [split]).
 *
 * An instance is stateful and belongs to one drag surface. Call [reset] when a gesture begins so
 * a new drag does not inherit the previous one's leftover fraction.
 *
 * @param gain the acceleration factor of BUILD_SPEC §6
 */
class CursorMotion(private val gain: Float = ACCELERATION) {

    private var residueX = 0f
    private var residueY = 0f

    /** Drops the carried fraction. Call at the start of a gesture. */
    fun reset() {
        residueX = 0f
        residueY = 0f
    }

    /**
     * Accelerates one touch-frame delta and returns the reports that carry it.
     *
     * Empty when the accumulated travel is still under a whole unit on both axes — nothing is
     * lost, it is held in the residue until it is worth a report.
     */
    fun step(dx: Float, dy: Float): List<HidReport> {
        // Pointer input never produces these, but a NaN residue would poison every later frame.
        if (!dx.isFinite() || !dy.isFinite()) return emptyList()

        residueX += dx * gain
        residueY += dy * gain

        val travelX = residueX.toInt()
        val travelY = residueY.toInt()
        residueX -= travelX
        residueY -= travelY

        if (travelX == 0 && travelY == 0) return emptyList()
        return split(travelX, travelY)
    }

    /**
     * Splits travel that overflows the declared axis range into reports of at most
     * [HidDescriptor.MOUSE_AXIS_MAX] each.
     *
     * Both axes are advanced towards their share of the total on every report, so a split flick
     * follows the same straight line the finger drew rather than an L.
     */
    private fun split(travelX: Int, travelY: Int): List<HidReport> {
        val reports = maxOf(reportsFor(travelX), reportsFor(travelY))
        if (reports <= 1) return listOf(HidReports.mouse(dx = travelX, dy = travelY))

        val out = ArrayList<HidReport>(reports)
        var sentX = 0
        var sentY = 0
        for (index in 1..reports) {
            val targetX = (travelX.toLong() * index / reports).toInt()
            val targetY = (travelY.toLong() * index / reports).toInt()
            out += HidReports.mouse(dx = targetX - sentX, dy = targetY - sentY)
            sentX = targetX
            sentY = targetY
        }
        return out
    }

    private fun reportsFor(travel: Int): Int =
        (abs(travel) + HidDescriptor.MOUSE_AXIS_MAX - 1) / HidDescriptor.MOUSE_AXIS_MAX

    companion object {
        /** BUILD_SPEC §6: relative mouse deltas at 1.6× acceleration. */
        const val ACCELERATION = 1.6f
    }
}

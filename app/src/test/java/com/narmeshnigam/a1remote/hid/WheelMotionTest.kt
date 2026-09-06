package com.narmeshnigam.a1remote.hid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WheelMotionTest {

    /** Strip travel worth exactly one wheel tick at the current gain. */
    private val oneTick = 1f / WheelMotion.SCROLL_GAIN

    private fun HidReport.wheel(): Int = data[3].toInt()

    @Test
    fun `a downward strip drag scrolls down as a negative wheel value`() {
        val report = WheelMotion().step(dy = oneTick)!!
        assertEquals(HidDescriptor.REPORT_ID_MOUSE, report.id)
        assertEquals(-1, report.wheel())
    }

    @Test
    fun `an upward drag scrolls the other way`() {
        assertEquals(1, WheelMotion().step(dy = -oneTick)!!.wheel())
    }

    @Test
    fun `a wheel report carries no button and no motion`() {
        val report = WheelMotion().step(dy = oneTick)!!
        assertArrayEquals(byteArrayOf(MouseButton.NONE.toByte(), 0, 0, -1), report.data)
    }

    @Test
    fun `a frame worth less than one tick transmits nothing yet`() {
        assertNull(WheelMotion().step(dy = oneTick * 0.4f))
    }

    @Test
    fun `the fraction of a sub-tick frame is carried, not truncated`() {
        val wheel = WheelMotion()
        assertNull(wheel.step(dy = oneTick * 0.5f))
        // The carried half plus another half crosses one whole tick.
        assertEquals(-1, wheel.step(dy = oneTick * 0.5f)!!.wheel())
    }

    @Test
    fun `a reset drops the carried fraction`() {
        val wheel = WheelMotion()
        wheel.step(dy = oneTick * 0.5f)
        wheel.reset()
        assertNull(wheel.step(dy = oneTick * 0.5f))
    }

    @Test
    fun `a non-finite delta is ignored`() {
        assertNull(WheelMotion().step(dy = Float.NaN))
    }
}

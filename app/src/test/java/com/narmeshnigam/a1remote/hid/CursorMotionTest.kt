package com.narmeshnigam.a1remote.hid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CursorMotionTest {

    @Test
    fun `a frame is scaled by the acceleration factor`() {
        val reports = CursorMotion().step(dx = 10f, dy = 0f)
        assertEquals(1, reports.size)
        assertEquals(16, reports.x())
    }

    @Test
    fun `acceleration applies to both axes and keeps their signs`() {
        val reports = CursorMotion().step(dx = -20f, dy = 5f)
        assertEquals(-32, reports.x())
        assertEquals(8, reports.y())
    }

    @Test
    fun `a move report is a mouse report with no button held`() {
        val report = CursorMotion().step(dx = 3f, dy = 3f).single()
        assertEquals(HidDescriptor.REPORT_ID_MOUSE, report.id)
        assertArrayEquals(byteArrayOf(MouseButton.NONE.toByte(), 4, 4, 0), report.data)
    }

    @Test
    fun `a frame worth less than one unit transmits nothing yet`() {
        assertTrue(CursorMotion().step(dx = 0.4f, dy = 0.4f).isEmpty())
    }

    @Test
    fun `the fraction of a sub-unit frame is carried, not truncated`() {
        val motion = CursorMotion()
        // 0.4 px × 1.6 = 0.64 per frame: nothing until the carried fraction crosses one unit.
        assertTrue(motion.step(dx = 0.4f, dy = 0f).isEmpty())
        assertEquals(1, motion.step(dx = 0.4f, dy = 0f).x())
    }

    @Test
    fun `a slow drag eventually moves the full accelerated distance`() {
        val motion = CursorMotion()
        var moved = 0
        repeat(100) { moved += motion.step(dx = 0.3f, dy = 0f).x() }
        // 100 frames × 0.3 px × 1.6 = 48 units, minus at most the one unit still in residue.
        assertTrue("moved $moved units", moved in 47..48)
    }

    @Test
    fun `a slow drag on the diagonal carries both axes`() {
        val motion = CursorMotion()
        var movedX = 0
        var movedY = 0
        repeat(50) {
            val reports = motion.step(dx = 0.25f, dy = -0.25f)
            movedX += reports.x()
            movedY += reports.y()
        }
        assertTrue("x $movedX", movedX in 19..20)
        assertTrue("y $movedY", movedY in -20..-19)
    }

    @Test
    fun `reset drops the residue so a new gesture starts clean`() {
        val motion = CursorMotion()
        motion.step(dx = 0.4f, dy = 0.4f)
        motion.reset()
        assertTrue(motion.step(dx = 0.4f, dy = 0.4f).isEmpty())
    }

    @Test
    fun `travel inside the signed 8-bit range is one report`() {
        val reports = CursorMotion(gain = 1f).step(dx = 127f, dy = -127f)
        assertEquals(1, reports.size)
        assertEquals(127, reports.x())
        assertEquals(-127, reports.y())
    }

    @Test
    fun `travel past the signed 8-bit range is split rather than clamped away`() {
        val reports = CursorMotion(gain = 1f).step(dx = 128f, dy = 0f)
        assertEquals(2, reports.size)
        assertEquals(128, reports.x())
    }

    @Test
    fun `a fast flick keeps its full distance across the split`() {
        val reports = CursorMotion().step(dx = 200f, dy = 0f)
        // 200 px × 1.6 = 320 units, which needs three reports.
        assertEquals(3, reports.size)
        assertEquals(320, reports.x())
    }

    @Test
    fun `no report in a split exceeds what the descriptor declares`() {
        val reports = CursorMotion().step(dx = 900f, dy = -640f)
        reports.forEach { report ->
            assertTrue(report.data[1].toInt() in HidDescriptor.MOUSE_AXIS_MIN..HidDescriptor.MOUSE_AXIS_MAX)
            assertTrue(report.data[2].toInt() in HidDescriptor.MOUSE_AXIS_MIN..HidDescriptor.MOUSE_AXIS_MAX)
        }
        assertEquals(1440, reports.x())
        assertEquals(-1024, reports.y())
    }

    @Test
    fun `a split flick advances both axes on every report rather than one then the other`() {
        val reports = CursorMotion(gain = 1f).step(dx = 400f, dy = 200f)
        assertTrue("expected a split", reports.size > 1)
        reports.forEach { report ->
            assertTrue("x stalled in ${report.hex()}", report.data[1].toInt() != 0)
            assertTrue("y stalled in ${report.hex()}", report.data[2].toInt() != 0)
        }
    }

    @Test
    fun `a negative flick splits the same way as a positive one`() {
        val reports = CursorMotion(gain = 1f).step(dx = -400f, dy = 0f)
        assertEquals(4, reports.size)
        assertEquals(-400, reports.x())
    }

    @Test
    fun `the residue never accumulates beyond a single unit`() {
        val motion = CursorMotion()
        repeat(1_000) { motion.step(dx = 0.7f, dy = 0.7f) }
        // A carried fraction that grew would show up as a phantom jump on the next tiny frame.
        assertTrue(motion.step(dx = 0.01f, dy = 0.01f).sumOf { it.data[1].toInt() } <= 1)
    }

    @Test
    fun `a non-finite frame is ignored and does not poison the residue`() {
        val motion = CursorMotion()
        assertTrue(motion.step(dx = Float.NaN, dy = 0f).isEmpty())
        assertTrue(motion.step(dx = Float.POSITIVE_INFINITY, dy = 0f).isEmpty())
        assertEquals(16, motion.step(dx = 10f, dy = 0f).x())
    }

    private fun List<HidReport>.x(): Int = sumOf { it.data[1].toInt() }

    private fun List<HidReport>.y(): Int = sumOf { it.data[2].toInt() }
}

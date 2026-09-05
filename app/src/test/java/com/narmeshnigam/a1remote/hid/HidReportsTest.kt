package com.narmeshnigam.a1remote.hid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HidReportsTest {

    @Test
    fun `keyboard report carries modifiers, a reserved zero byte and up to six keys`() {
        val report = HidReports.keyboard(modifiers = 0x02, keys = intArrayOf(0x04, 0x05))
        assertEquals(HidDescriptor.REPORT_ID_KEYBOARD, report.id)
        assertArrayEquals(
            byteArrayOf(0x02, 0x00, 0x04, 0x05, 0x00, 0x00, 0x00, 0x00),
            report.data,
        )
    }

    @Test
    fun `keyboard report is always eight bytes`() {
        assertEquals(HidDescriptor.KEYBOARD_REPORT_SIZE, HidReports.keyboard().data.size)
        assertEquals(
            HidDescriptor.KEYBOARD_REPORT_SIZE,
            HidReports.keyboard(keys = intArrayOf(1, 2, 3, 4, 5, 6)).data.size,
        )
    }

    @Test
    fun `key builds a single-usage keyboard report`() {
        assertEquals(HidReports.keyboard(keys = intArrayOf(KeyboardUsage.DOWN_ARROW)), HidReports.key(0x51))
    }

    @Test
    fun `arrow down is the usage KEY_LAB specifies`() {
        assertArrayEquals(
            byteArrayOf(0x00, 0x00, 0x51, 0x00, 0x00, 0x00, 0x00, 0x00),
            HidReports.key(KeyboardUsage.DOWN_ARROW).data,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `keyboard rejects a seventh key rather than dropping it`() {
        HidReports.keyboard(keys = intArrayOf(1, 2, 3, 4, 5, 6, 7))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `keyboard rejects a usage above the declared range`() {
        HidReports.key(0x100)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `keyboard rejects a negative usage`() {
        HidReports.key(-1)
    }

    @Test
    fun `consumer report is one little-endian 16-bit usage`() {
        val report = HidReports.consumer(0x0224)
        assertEquals(HidDescriptor.REPORT_ID_CONSUMER, report.id)
        assertArrayEquals(byteArrayOf(0x24, 0x02), report.data)
    }

    @Test
    fun `consumer report accepts the top of the declared range`() {
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0x03), HidReports.consumer(0x03FF).data)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `consumer rejects a usage the descriptor does not declare`() {
        HidReports.consumer(HidDescriptor.CONSUMER_USAGE_MAX + 1)
    }

    @Test
    fun `mouse report is buttons then relative x, y and wheel`() {
        val report = HidReports.mouse(buttons = MouseButton.LEFT, dx = -5, dy = 7, wheel = -1)
        assertEquals(HidDescriptor.REPORT_ID_MOUSE, report.id)
        assertArrayEquals(byteArrayOf(0x01, (-5).toByte(), 0x07, (-1).toByte()), report.data)
    }

    @Test
    fun `mouse clamps deltas instead of wrapping them`() {
        assertArrayEquals(byteArrayOf(0x00, 127, -127, 0x00), HidReports.mouse(dx = 5000, dy = -5000).data)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `mouse rejects a button bit the descriptor does not declare`() {
        HidReports.mouse(buttons = 0x08)
    }

    @Test
    fun `release reports are all zero and of the right length`() {
        assertTrue(HidReports.keyboardRelease().isRelease)
        assertTrue(HidReports.consumerRelease().isRelease)
        assertTrue(HidReports.mouseRelease().isRelease)
        assertEquals(HidDescriptor.KEYBOARD_REPORT_SIZE, HidReports.keyboardRelease().data.size)
        assertEquals(HidDescriptor.CONSUMER_REPORT_SIZE, HidReports.consumerRelease().data.size)
        assertEquals(HidDescriptor.MOUSE_REPORT_SIZE, HidReports.mouseRelease().data.size)
    }

    @Test
    fun `releaseFor matches the report id it is given`() {
        assertEquals(HidReports.keyboardRelease(), HidReports.releaseFor(HidReports.key(0x51)))
        assertEquals(HidReports.consumerRelease(), HidReports.releaseFor(HidReports.consumer(0x00E9)))
        assertEquals(HidReports.mouseRelease(), HidReports.releaseFor(HidReports.mouse(dx = 1)))
    }

    @Test
    fun `every release is the same length as the press it follows`() {
        listOf(
            HidReports.key(KeyboardUsage.ENTER),
            HidReports.consumer(ConsumerUsage.MUTE),
            HidReports.mouse(buttons = MouseButton.LEFT),
        ).forEach { press ->
            val release = HidReports.releaseFor(press)
            assertEquals(press.id, release.id)
            assertEquals(press.data.size, release.data.size)
            assertFalse(press.isRelease)
        }
    }

    @Test
    fun `hex renders the bytes a wire log has to show`() {
        assertEquals("00 00 51 00 00 00 00 00", HidReports.key(KeyboardUsage.DOWN_ARROW).hex())
    }

    @Test
    fun `reports compare by id and content, not identity`() {
        assertEquals(HidReports.consumer(0x00E9), HidReports.consumer(0x00E9))
        assertEquals(HidReports.consumer(0x00E9).hashCode(), HidReports.consumer(0x00E9).hashCode())
        assertFalse(HidReports.consumer(0x00E9) == HidReports.consumer(0x00EA))
        // Same payload, different collection, must not be equal.
        assertFalse(HidReports.consumerRelease() == HidReport(HidDescriptor.REPORT_ID_MOUSE, ByteArray(2)))
    }
}

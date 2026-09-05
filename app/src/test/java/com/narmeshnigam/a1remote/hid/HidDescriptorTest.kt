package com.narmeshnigam.a1remote.hid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The descriptor is the one thing in this app a host parses byte for byte. These tests pin every
 * byte of every collection, so a change to it can only ever be deliberate.
 */
class HidDescriptorTest {

    @Test
    fun `keyboard collection matches the spec byte for byte`() {
        assertArrayEquals(
            bytes(
                0x05, 0x01,
                0x09, 0x06,
                0xA1, 0x01,
                0x85, 0x01,
                0x05, 0x07,
                0x19, 0xE0,
                0x29, 0xE7,
                0x15, 0x00,
                0x25, 0x01,
                0x75, 0x01,
                0x95, 0x08,
                0x81, 0x02,
                0x95, 0x01,
                0x75, 0x08,
                0x81, 0x01,
                0x95, 0x06,
                0x75, 0x08,
                0x15, 0x00,
                0x26, 0xFF, 0x00,
                0x05, 0x07,
                0x19, 0x00,
                0x2A, 0xFF, 0x00,
                0x81, 0x00,
                0xC0,
            ),
            HidDescriptor.keyboardCollection(),
        )
    }

    @Test
    fun `consumer collection matches the spec byte for byte`() {
        assertArrayEquals(
            bytes(
                0x05, 0x0C,
                0x09, 0x01,
                0xA1, 0x01,
                0x85, 0x02,
                0x15, 0x00,
                0x26, 0xFF, 0x03,
                0x19, 0x00,
                0x2A, 0xFF, 0x03,
                0x75, 0x10,
                0x95, 0x01,
                0x81, 0x00,
                0xC0,
            ),
            HidDescriptor.consumerCollection(),
        )
    }

    @Test
    fun `mouse collection matches the spec byte for byte`() {
        assertArrayEquals(
            bytes(
                0x05, 0x01,
                0x09, 0x02,
                0xA1, 0x01,
                0x85, 0x03,
                0x09, 0x01,
                0xA1, 0x00,
                0x05, 0x09,
                0x19, 0x01,
                0x29, 0x03,
                0x15, 0x00,
                0x25, 0x01,
                0x75, 0x01,
                0x95, 0x03,
                0x81, 0x02,
                0x75, 0x05,
                0x95, 0x01,
                0x81, 0x01,
                0x05, 0x01,
                0x09, 0x30,
                0x09, 0x31,
                0x09, 0x38,
                0x15, 0x81,
                0x25, 0x7F,
                0x75, 0x08,
                0x95, 0x03,
                0x81, 0x06,
                0xC0,
                0xC0,
            ),
            HidDescriptor.mouseCollection(),
        )
    }

    @Test
    fun `descriptor is the three collections in order`() {
        val expected = HidDescriptor.keyboardCollection() +
            HidDescriptor.consumerCollection() +
            HidDescriptor.mouseCollection()
        assertArrayEquals(expected, HidDescriptor.bytes())
    }

    @Test
    fun `descriptor accessors hand out copies so a caller cannot corrupt the descriptor`() {
        val first = HidDescriptor.bytes()
        assertNotSame(first, HidDescriptor.bytes())
        first[0] = 0x00
        assertEquals(0x05.toByte(), HidDescriptor.bytes()[0])
    }

    @Test
    fun `collections open and close in balance`() {
        // 0xA1 opens a collection, 0xC0 closes one; the descriptor must end at depth zero.
        var depth = 0
        var index = 0
        val descriptor = HidDescriptor.bytes()
        while (index < descriptor.size) {
            val item = descriptor[index].toInt() and 0xFF
            when (item) {
                0xC0 -> depth--
                0xA1 -> depth++
            }
            assertTrue("collection depth went negative at byte $index", depth >= 0)
            // Bits 0..1 of a short item header are bSize, where 3 encodes four data bytes.
            val bSize = item and 0x03
            index += 1 + if (bSize == 3) 4 else bSize
        }
        assertEquals(0, depth)
    }

    @Test
    fun `report ids are distinct so the three collections cannot collide`() {
        val ids = setOf(
            HidDescriptor.REPORT_ID_KEYBOARD,
            HidDescriptor.REPORT_ID_CONSUMER,
            HidDescriptor.REPORT_ID_MOUSE,
        )
        assertEquals(3, ids.size)
    }

    @Test
    fun `consumer usage ceiling stays inside the host parser's usage budget`() {
        // The Linux and Android HID parsers expand a usage range entry by entry and cap the
        // total. A full 16-bit range would make the whole descriptor fail to parse on the host.
        assertEquals(0x03FF, HidDescriptor.CONSUMER_USAGE_MAX)
        assertTrue(HidDescriptor.CONSUMER_USAGE_MAX + 1 <= 4096)
    }

    private fun bytes(vararg item: Int): ByteArray = ByteArray(item.size) { item[it].toByte() }
}

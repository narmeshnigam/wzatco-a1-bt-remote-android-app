package com.narmeshnigam.a1remote.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The key-up guarantee of BUILD_SPEC §4. A key left held on the projector cannot be released
 * from the phone, so these tests are the whole reason this class exists.
 */
class KeyPressSenderTest {

    @Test
    fun `a press is a key-down followed by its matching release`() {
        val sent = mutableListOf<HidReport>()
        val outcome = KeyPressSender { report ->
            sent += report
            true
        }.press(HidReports.key(KeyboardUsage.DOWN_ARROW))

        assertEquals(listOf(HidReports.key(KeyboardUsage.DOWN_ARROW), HidReports.keyboardRelease()), sent)
        assertTrue(outcome.down)
        assertTrue(outcome.up)
    }

    @Test
    fun `the release matches the collection of the press`() {
        val sent = mutableListOf<HidReport>()
        val sender = KeyPressSender { report ->
            sent += report
            true
        }
        sender.press(HidReports.consumer(ConsumerUsage.MUTE))
        sender.press(HidReports.mouse(buttons = MouseButton.LEFT))

        assertEquals(HidReports.consumerRelease(), sent[1])
        assertEquals(HidReports.mouseRelease(), sent[3])
    }

    @Test
    fun `the release still goes out when the key-down is refused`() {
        val sent = mutableListOf<HidReport>()
        val outcome = KeyPressSender { report ->
            sent += report
            false
        }.press(HidReports.key(KeyboardUsage.ENTER))

        assertEquals(2, sent.size)
        assertTrue(sent[1].isRelease)
        assertFalse(outcome.down)
    }

    @Test
    fun `the release still goes out when the key-down throws, and the failure propagates`() {
        val sent = mutableListOf<HidReport>()
        val sender = KeyPressSender { report ->
            if (!report.isRelease) error("stack went away")
            sent += report
            true
        }

        val thrown = runCatching { sender.press(HidReports.key(KeyboardUsage.UP_ARROW)) }

        assertTrue("the caller must hear about the failure", thrown.isFailure)
        assertEquals("but only after the release went out", 1, sent.size)
        assertTrue(sent.single().isRelease)
    }

    @Test
    fun `a release that itself fails is reported rather than thrown`() {
        val outcome = KeyPressSender { report ->
            if (report.isRelease) error("stack went away mid-press")
            true
        }.press(HidReports.key(KeyboardUsage.ENTER))

        assertTrue(outcome.down)
        assertFalse(outcome.up)
    }

    @Test
    fun `every mapped default function presses as a clean pair`() {
        RemoteFunction.entries
            .mapNotNull { DefaultKeyMap[it].report }
            .forEach { report ->
                val sent = mutableListOf<HidReport>()
                KeyPressSender { out ->
                    sent += out
                    true
                }.press(report)
                assertEquals(2, sent.size)
                assertEquals(report, sent[0])
                assertTrue(sent[1].isRelease)
                assertEquals(report.id, sent[1].id)
            }
    }
}

package com.narmeshnigam.a1remote.service

import com.narmeshnigam.a1remote.hid.ConsumerUsage
import com.narmeshnigam.a1remote.hid.DefaultKeyMap
import com.narmeshnigam.a1remote.hid.HidReports
import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.KeyStatus
import com.narmeshnigam.a1remote.hid.KeyboardUsage
import com.narmeshnigam.a1remote.hid.MouseButton
import com.narmeshnigam.a1remote.hid.RemoteFunction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HidLinkTest {

    private lateinit var transport: FakeHidTransport

    @Before
    fun install() {
        transport = FakeHidTransport()
        HidLink.transport = transport
    }

    @After
    fun remove() {
        HidLink.transport = null
        HidLink.clearWireLog()
    }

    @Test
    fun `with no service running nothing is transmitted and the caller is told`() {
        HidLink.transport = null

        assertEquals(SendResult.NO_SERVICE, HidLink.sendKey(RemoteFunction.DOWN))
        assertEquals(SendResult.NO_SERVICE, HidLink.sendPress(HidReports.key(0x51), "probe"))
        assertEquals(SendResult.NO_SERVICE, HidLink.sendMotion(HidReports.mouse(dx = 4), "move"))
    }

    @Test
    fun `a mapped function reaches the wire as a press and a release`() {
        assertEquals(SendResult.SENT, HidLink.sendKey(RemoteFunction.DOWN))

        assertEquals(
            listOf(HidReports.key(KeyboardUsage.DOWN_ARROW), HidReports.keyboardRelease()),
            transport.sent,
        )
    }

    @Test
    fun `an unmapped function transmits nothing at all`() {
        // No shipped function is unmapped now that the four the A1 ignored are gone, so the
        // binding is made unmapped here. The guarantee is what matters: a key map that knows no
        // code for a function puts nothing on the wire rather than something nearby.
        val unmapped = FakeHidTransport(
            bindings = DefaultKeyMap.all() + mapOf(
                RemoteFunction.POWER to KeyBinding(null, KeyStatus.UNMAPPED, "unknown"),
            ),
        )
        HidLink.transport = unmapped

        assertEquals(SendResult.UNMAPPED, HidLink.sendKey(RemoteFunction.POWER))
        assertTrue("a function with no mapping must put nothing on the wire", unmapped.sent.isEmpty())
    }

    @Test
    fun `nothing is transmitted while the link is down`() {
        transport.connected = false

        assertEquals(SendResult.NOT_CONNECTED, HidLink.sendKey(RemoteFunction.OK))
        assertEquals(SendResult.NOT_CONNECTED, HidLink.sendMotion(HidReports.mouse(dx = 9), "move"))
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun `a button press gets a release but a motion report does not`() {
        HidLink.sendPress(HidReports.mouse(buttons = MouseButton.LEFT), "left click")
        assertEquals(2, transport.sent.size)
        assertTrue(transport.sent[1].isRelease)

        transport.sent.clear()
        HidLink.sendMotion(HidReports.mouse(dx = 10, dy = -4), "move")
        assertEquals("a delta is over the moment it is delivered", 1, transport.sent.size)
    }

    @Test
    fun `every confirmed keypad function is released after it is pressed`() {
        listOf(
            RemoteFunction.UP,
            RemoteFunction.DOWN,
            RemoteFunction.LEFT,
            RemoteFunction.RIGHT,
            RemoteFunction.OK,
            RemoteFunction.BACK,
            RemoteFunction.HOME,
            RemoteFunction.MENU,
            RemoteFunction.VOLUME_UP,
            RemoteFunction.VOLUME_DOWN,
            RemoteFunction.MUTE,
        ).forEach { HidLink.sendKey(it) }

        assertEquals(22, transport.sent.size)
        assertTrue(transport.everyPressWasReleased())
    }

    @Test
    fun `the wire log keeps only the last fifty entries`() {
        HidLink.clearWireLog()
        repeat(60) { index -> HidLink.record(WireLogEntry(index.toLong(), "key", "entry $index")) }

        val log = HidLink.wireLog.value
        assertEquals(50, log.size)
        assertEquals("entry 10", log.first().detail)
        assertEquals("entry 59", log.last().detail)
    }

    @Test
    fun `consumer functions go out on the consumer collection`() {
        HidLink.sendKey(RemoteFunction.MUTE)
        assertEquals(HidReports.consumer(ConsumerUsage.MUTE), transport.sent.first())
    }
}

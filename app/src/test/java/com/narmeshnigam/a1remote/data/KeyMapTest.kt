package com.narmeshnigam.a1remote.data

import com.narmeshnigam.a1remote.hid.DefaultKeyMap
import com.narmeshnigam.a1remote.hid.HidReports
import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.KeyStatus
import com.narmeshnigam.a1remote.hid.RemoteFunction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KeyMapTest {

    private fun keyMap(store: KeyMapStore = FakeKeyMapStore()): KeyMap =
        KeyMap(store, TestScope(UnconfinedTestDispatcher()))

    @Test
    fun `defaults come from the shipped table`() {
        val map = keyMap()
        assertEquals(DefaultKeyMap[RemoteFunction.DOWN], map[RemoteFunction.DOWN])
        assertEquals(HidReports.key(0x51), map.reportFor(RemoteFunction.DOWN))
    }

    @Test
    fun `an unmapped binding returns null rather than a wrong report`() {
        // Nothing ships unmapped any more, but the state is still reachable — a stored override
        // can carry no report — and null has to stay the answer rather than a nearby usage.
        val store = FakeKeyMapStore(
            mapOf(RemoteFunction.POWER to KeyBinding(null, KeyStatus.UNMAPPED, "unknown")),
        )
        val map = keyMap(store)
        assertNull(map.reportFor(RemoteFunction.POWER))
        assertFalse(map.isVerified(RemoteFunction.POWER))
    }

    @Test
    fun `a stored override replaces the default`() = runTest {
        val store = FakeKeyMapStore(
            mapOf(
                RemoteFunction.POWER to KeyBinding(HidReports.key(0x66), KeyStatus.CONFIRMED, "Power"),
            ),
        )
        val map = keyMap(store)
        assertEquals(HidReports.key(0x66), map.reportFor(RemoteFunction.POWER))
        assertTrue(map.isVerified(RemoteFunction.POWER))
    }

    @Test
    fun `a promotion reloads at runtime with no restart`() = runTest {
        val map = keyMap()
        assertFalse(map.isVerified(RemoteFunction.POWER))
        assertEquals(KeyStatus.CANDIDATE, map[RemoteFunction.POWER].status)

        map.promote(RemoteFunction.POWER, HidReports.key(0x66), "Power")

        assertEquals(HidReports.key(0x66), map.reportFor(RemoteFunction.POWER))
        assertEquals(KeyStatus.CONFIRMED, map[RemoteFunction.POWER].status)
        assertTrue(map.isVerified(RemoteFunction.POWER))
    }

    @Test
    fun `a promotion can map a binding that carried no report`() = runTest {
        val store = FakeKeyMapStore(
            mapOf(RemoteFunction.POWER to KeyBinding(null, KeyStatus.UNMAPPED, "unknown")),
        )
        val map = keyMap(store)
        assertNull(map.reportFor(RemoteFunction.POWER))

        map.promote(RemoteFunction.POWER, HidReports.key(0x66), "Power")

        assertEquals(HidReports.key(0x66), map.reportFor(RemoteFunction.POWER))
        assertTrue(map.isVerified(RemoteFunction.POWER))
    }

    @Test
    fun `reset drops the override and falls back to the shipped default`() = runTest {
        val map = keyMap()
        map.promote(RemoteFunction.POWER, HidReports.key(0x66), "Power")
        assertTrue(map.isVerified(RemoteFunction.POWER))

        map.reset(RemoteFunction.POWER)

        assertEquals(DefaultKeyMap[RemoteFunction.POWER], map[RemoteFunction.POWER])
        assertFalse(map.isVerified(RemoteFunction.POWER))
    }

    @Test
    fun `resetAll drops every override`() = runTest {
        val map = keyMap()
        map.promote(RemoteFunction.POWER, HidReports.key(0x66), "Power")
        map.promote(RemoteFunction.MENU, HidReports.key(0x29), "Escape")

        map.resetAll()

        assertEquals(DefaultKeyMap.all(), map.bindings.value)
    }

    @Test
    fun `every function is present in the live table at all times`() = runTest {
        val map = keyMap()
        assertEquals(RemoteFunction.entries.toSet(), map.bindings.value.keys)
        map.promote(RemoteFunction.POWER, HidReports.key(0x66), "Power")
        assertEquals(RemoteFunction.entries.toSet(), map.bindings.value.keys)
    }

    @Test
    fun `only confirmed bindings count as verified`() {
        val map = keyMap()
        assertTrue(map.isVerified(RemoteFunction.OK))
        assertFalse("power is still a guess", map.isVerified(RemoteFunction.POWER))
    }
}

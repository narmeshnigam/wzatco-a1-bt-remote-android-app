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
    fun `an unmapped function returns null rather than a wrong report`() {
        val map = keyMap()
        assertNull(map.reportFor(RemoteFunction.SCREEN_FLIP))
        assertNull(map.reportFor(RemoteFunction.KEYSTONE))
    }

    @Test
    fun `a stored override replaces the default`() = runTest {
        val store = FakeKeyMapStore(
            mapOf(
                RemoteFunction.FOCUS_UP to KeyBinding(HidReports.key(0x3E), KeyStatus.CONFIRMED, "F5"),
            ),
        )
        val map = keyMap(store)
        assertEquals(HidReports.key(0x3E), map.reportFor(RemoteFunction.FOCUS_UP))
        assertTrue(map.isVerified(RemoteFunction.FOCUS_UP))
    }

    @Test
    fun `a promotion reloads at runtime with no restart`() = runTest {
        val map = keyMap()
        assertFalse(map.isVerified(RemoteFunction.SOURCE))
        assertEquals(KeyStatus.CANDIDATE, map[RemoteFunction.SOURCE].status)

        map.promote(RemoteFunction.SOURCE, HidReports.key(0x3D), "F4")

        assertEquals(HidReports.key(0x3D), map.reportFor(RemoteFunction.SOURCE))
        assertEquals(KeyStatus.CONFIRMED, map[RemoteFunction.SOURCE].status)
        assertTrue(map.isVerified(RemoteFunction.SOURCE))
    }

    @Test
    fun `a promotion can map a function the defaults leave unmapped`() = runTest {
        val map = keyMap()
        assertNull(map.reportFor(RemoteFunction.KEYSTONE))

        map.promote(RemoteFunction.KEYSTONE, HidReports.key(0x3B), "F2")

        assertEquals(HidReports.key(0x3B), map.reportFor(RemoteFunction.KEYSTONE))
        assertTrue(map.isVerified(RemoteFunction.KEYSTONE))
    }

    @Test
    fun `reset drops the override and falls back to the shipped default`() = runTest {
        val map = keyMap()
        map.promote(RemoteFunction.FOCUS_UP, HidReports.key(0x3E), "F5")
        assertTrue(map.isVerified(RemoteFunction.FOCUS_UP))

        map.reset(RemoteFunction.FOCUS_UP)

        assertEquals(DefaultKeyMap[RemoteFunction.FOCUS_UP], map[RemoteFunction.FOCUS_UP])
        assertFalse(map.isVerified(RemoteFunction.FOCUS_UP))
    }

    @Test
    fun `resetAll drops every override`() = runTest {
        val map = keyMap()
        map.promote(RemoteFunction.FOCUS_UP, HidReports.key(0x3E), "F5")
        map.promote(RemoteFunction.KEYSTONE, HidReports.key(0x3B), "F2")

        map.resetAll()

        assertEquals(DefaultKeyMap.all(), map.bindings.value)
    }

    @Test
    fun `every function is present in the live table at all times`() = runTest {
        val map = keyMap()
        assertEquals(RemoteFunction.entries.toSet(), map.bindings.value.keys)
        map.promote(RemoteFunction.KEYSTONE, HidReports.key(0x3B), "F2")
        assertEquals(RemoteFunction.entries.toSet(), map.bindings.value.keys)
    }

    @Test
    fun `only confirmed bindings count as verified`() {
        val map = keyMap()
        assertTrue(map.isVerified(RemoteFunction.OK))
        assertFalse(map.isVerified(RemoteFunction.POWER))
        assertFalse(map.isVerified(RemoteFunction.SCREEN_FLIP))
    }
}

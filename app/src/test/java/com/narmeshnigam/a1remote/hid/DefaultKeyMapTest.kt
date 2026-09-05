package com.narmeshnigam.a1remote.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The key map is a transcription of the table in KEY_LAB.md. These tests are the transcription
 * check: if the table and the code ever disagree, one of them is a lie about the hardware.
 */
class DefaultKeyMapTest {

    @Test
    fun `every function is in the table`() {
        RemoteFunction.entries.forEach { function ->
            assertNotNull("no binding for $function", DefaultKeyMap[function])
        }
    }

    @Test
    fun `confirmed functions map to the usages KEY_LAB records`() {
        val expected = mapOf(
            RemoteFunction.UP to HidReports.key(0x52),
            RemoteFunction.DOWN to HidReports.key(0x51),
            RemoteFunction.LEFT to HidReports.key(0x50),
            RemoteFunction.RIGHT to HidReports.key(0x4F),
            RemoteFunction.OK to HidReports.key(0x28),
            RemoteFunction.BACK to HidReports.consumer(0x0224),
            RemoteFunction.HOME to HidReports.consumer(0x0223),
            RemoteFunction.MENU to HidReports.key(0x65),
            RemoteFunction.VOLUME_UP to HidReports.consumer(0x00E9),
            RemoteFunction.VOLUME_DOWN to HidReports.consumer(0x00EA),
            RemoteFunction.MUTE to HidReports.consumer(0x00E2),
        )
        expected.forEach { (function, report) ->
            val binding = DefaultKeyMap[function]
            assertEquals("$function report", report, binding.report)
            assertEquals("$function status", KeyStatus.CONFIRMED, binding.status)
        }
    }

    @Test
    fun `candidate functions are shipped but marked as guesses`() {
        val expected = mapOf(
            RemoteFunction.POWER to HidReports.consumer(0x0030),
            RemoteFunction.FOCUS_UP to HidReports.consumer(0x022D),
            RemoteFunction.FOCUS_DOWN to HidReports.consumer(0x022E),
            RemoteFunction.SOURCE to HidReports.consumer(0x0089),
        )
        expected.forEach { (function, report) ->
            val binding = DefaultKeyMap[function]
            assertEquals("$function report", report, binding.report)
            assertEquals("$function status", KeyStatus.CANDIDATE, binding.status)
        }
    }

    @Test
    fun `unmapped functions return no report rather than a plausible wrong one`() {
        listOf(RemoteFunction.SCREEN_FLIP, RemoteFunction.KEYSTONE).forEach { function ->
            val binding = DefaultKeyMap[function]
            assertNull("$function must have no report until Key Lab finds one", binding.report)
            assertEquals(KeyStatus.UNMAPPED, binding.status)
        }
    }

    @Test
    fun `exactly the eight confirmed functions of the build spec are confirmed`() {
        // BUILD_SPEC §1: eight of the twelve functions map to standard usages. Counting one key
        // at a time, that is the four arrows plus OK, Back, Home, Menu, the two volume keys
        // and mute.
        val confirmed = RemoteFunction.entries.filter { DefaultKeyMap[it].status == KeyStatus.CONFIRMED }
        assertEquals(11, confirmed.size)
        assertEquals(
            setOf(
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
            ),
            confirmed.toSet(),
        )
    }

    @Test
    fun `the alternate back mapping is escape, as KEY_LAB records`() {
        assertEquals(HidReports.key(0x29), DefaultKeyMap.backAlternate())
    }

    @Test
    fun `findings-file names round-trip through the function enum`() {
        // KEY_LAB.md's findings schema uses these exact strings.
        assertEquals(RemoteFunction.FOCUS_UP, RemoteFunction.valueOf("FOCUS_UP"))
        assertEquals(RemoteFunction.SCREEN_FLIP, RemoteFunction.valueOf("SCREEN_FLIP"))
    }
}

package com.narmeshnigam.a1remote.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `power is shipped but marked as a guess`() {
        val binding = DefaultKeyMap[RemoteFunction.POWER]
        assertEquals(HidReports.consumer(0x0030), binding.report)
        assertEquals(KeyStatus.CANDIDATE, binding.status)
    }

    @Test
    fun `power is the only function the shipped table is unsure of`() {
        val unsure = RemoteFunction.entries.filter { DefaultKeyMap[it].status != KeyStatus.CONFIRMED }
        assertEquals(listOf(RemoteFunction.POWER), unsure)
    }

    @Test
    fun `nothing ships without a report now that the unreachable functions are gone`() {
        // Focus ±, Source, Flip and Keystone were the four rows that shipped mapped to nothing.
        // They are no longer functions at all, so the table has no holes left in it.
        RemoteFunction.entries.forEach { function ->
            assertNotNull("$function ships with no report", DefaultKeyMap[function].report)
        }
    }

    @Test
    fun `exactly the eleven confirmed functions of the build spec are confirmed`() {
        // BUILD_SPEC §1, counting one key at a time: the four arrows plus OK, Back, Home, Menu,
        // the two volume keys and mute. Power is the twelfth and the only unproven one.
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
        assertEquals(RemoteFunction.POWER, RemoteFunction.valueOf("POWER"))
        assertEquals(RemoteFunction.VOLUME_DOWN, RemoteFunction.valueOf("VOLUME_DOWN"))
    }
}

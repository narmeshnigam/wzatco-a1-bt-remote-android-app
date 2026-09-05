package com.narmeshnigam.a1remote.vm

import com.narmeshnigam.a1remote.hid.RemoteFunction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BUILD_SPEC §5: 400 ms delay, then every 90 ms while held. Verified on virtual time — the
 * alternative is holding a finger on a phone and counting.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoRepeatTest {

    @Test
    fun `nothing repeats before the initial delay`() = runTest {
        var ticks = 0
        val job = launch { AutoRepeat.run { ticks++ } }

        advanceTimeBy(AutoRepeat.INITIAL_DELAY_MS - 1)
        assertEquals(0, ticks)

        job.cancel()
    }

    @Test
    fun `the first repeat lands at 400 ms`() = runTest {
        var ticks = 0
        val job = launch { AutoRepeat.run { ticks++ } }

        advanceTimeBy(AutoRepeat.INITIAL_DELAY_MS + 1)
        assertEquals(1, ticks)

        job.cancel()
    }

    @Test
    fun `after the first repeat it ticks every 90 ms`() = runTest {
        var ticks = 0
        val job = launch { AutoRepeat.run { ticks++ } }

        advanceTimeBy(AutoRepeat.INITIAL_DELAY_MS + 1)
        assertEquals(1, ticks)

        advanceTimeBy(AutoRepeat.INTERVAL_MS)
        assertEquals(2, ticks)

        advanceTimeBy(AutoRepeat.INTERVAL_MS * 10)
        assertEquals(12, ticks)

        job.cancel()
    }

    @Test
    fun `releasing stops it`() = runTest {
        var ticks = 0
        val job = launch { AutoRepeat.run { ticks++ } }

        advanceTimeBy(AutoRepeat.INITIAL_DELAY_MS + AutoRepeat.INTERVAL_MS * 3)
        val atRelease = ticks
        job.cancel()

        advanceTimeBy(AutoRepeat.INTERVAL_MS * 20)
        assertEquals("no tick may arrive after the finger lifts", atRelease, ticks)
    }

    @Test
    fun `the timing is the one the spec gives`() {
        assertEquals(400L, AutoRepeat.INITIAL_DELAY_MS)
        assertEquals(90L, AutoRepeat.INTERVAL_MS)
    }

    @Test
    fun `only the d-pad, volume and focus repeat`() {
        val repeating = RemoteFunction.entries
            .filter { it.repeatBehaviour() == RepeatBehaviour.REPEATING }
            .toSet()

        assertEquals(
            setOf(
                RemoteFunction.UP,
                RemoteFunction.DOWN,
                RemoteFunction.LEFT,
                RemoteFunction.RIGHT,
                RemoteFunction.VOLUME_UP,
                RemoteFunction.VOLUME_DOWN,
                RemoteFunction.FOCUS_UP,
                RemoteFunction.FOCUS_DOWN,
            ),
            repeating,
        )
    }

    @Test
    fun `power never repeats`() {
        assertEquals(RepeatBehaviour.SINGLE, RemoteFunction.POWER.repeatBehaviour())
    }
}

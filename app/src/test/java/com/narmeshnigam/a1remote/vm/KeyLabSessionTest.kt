package com.narmeshnigam.a1remote.vm

import com.narmeshnigam.a1remote.data.Verdict
import com.narmeshnigam.a1remote.hid.KeyLabCandidates
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.hid.ReportKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Key Lab protocol of KEY_LAB.md, without a projector. */
class KeyLabSessionTest {

    private fun session() = KeyLabSession()

    @Test
    fun `the pass opens on the first function and its first candidate`() {
        val state = session().state.value
        assertEquals(RemoteFunction.FOCUS_UP, state.function)
        assertEquals(0, state.functionIndex)
        assertEquals(KeyLabCandidates.FUNCTIONS.size, state.functionCount)
        assertEquals(KeyLabMode.CANDIDATES, state.mode)
        assertEquals(KeyLabCandidates.listFor(RemoteFunction.FOCUS_UP).first(), state.candidate)
        assertEquals("Candidate 1 of 4", state.positionLabel)
    }

    @Test
    fun `a miss advances to the next candidate of the same function`() {
        val session = session()
        session.nextCandidate()

        val state = session.state.value
        assertEquals(RemoteFunction.FOCUS_UP, state.function)
        assertEquals(1, state.candidateIndex)
        assertEquals(KeyLabCandidates.listFor(RemoteFunction.FOCUS_UP)[1], state.candidate)
        assertEquals("Candidate 2 of 4", state.positionLabel)
    }

    @Test
    fun `an exhausted list advances to the next function`() {
        val session = session()
        repeat(KeyLabCandidates.listFor(RemoteFunction.FOCUS_UP).size) { session.nextCandidate() }

        val state = session.state.value
        assertEquals(RemoteFunction.FOCUS_DOWN, state.function)
        assertEquals(1, state.functionIndex)
        assertEquals(0, state.candidateIndex)
        assertEquals(KeyLabMode.CANDIDATES, state.mode)
    }

    @Test
    fun `a hit advances to the next function whatever candidate it was on`() {
        val session = session()
        session.nextCandidate()

        val hit = session.advancePast(Verdict.MAPPED)

        assertEquals(KeyLabCandidates.listFor(RemoteFunction.FOCUS_UP)[1], hit)
        assertEquals(RemoteFunction.FOCUS_DOWN, session.state.value.function)
        assertEquals(0, session.state.value.candidateIndex)
    }

    @Test
    fun `a side effect is recorded against the candidate but does not end the function`() {
        val session = session()

        val tried = session.advancePast(Verdict.SIDE_EFFECT)

        assertEquals(KeyLabCandidates.listFor(RemoteFunction.FOCUS_UP).first(), tried)
        assertEquals(RemoteFunction.FOCUS_UP, session.state.value.function)
        assertEquals(1, session.state.value.candidateIndex)
    }

    @Test
    fun `the pass wraps at the last function`() {
        val session = session()
        session.selectFunction(KeyLabCandidates.FUNCTIONS.last())

        session.nextFunction()

        assertEquals(KeyLabCandidates.FUNCTIONS.first(), session.state.value.function)
    }

    @Test
    fun `an exhausted list falls into the function's sweep before moving on`() {
        val session = session()
        session.selectFunction(RemoteFunction.SCREEN_FLIP)
        repeat(KeyLabCandidates.listFor(RemoteFunction.SCREEN_FLIP).size) { session.nextCandidate() }

        val state = session.state.value
        assertEquals(RemoteFunction.SCREEN_FLIP, state.function)
        assertEquals(KeyLabMode.SWEEP, state.mode)
        assertEquals(KeyLabCandidates.FUNCTION_KEY_SWEEP.candidateAt(0), state.candidate)
    }

    @Test
    fun `an exhausted sweep moves to the next function`() {
        val session = session()
        session.selectFunction(RemoteFunction.KEYSTONE)
        session.setMode(KeyLabMode.SWEEP)
        repeat(KeyLabCandidates.CONSUMER_SWEEP.size) { session.nextCandidate() }

        assertEquals(RemoteFunction.POWER, session.state.value.function)
    }

    @Test
    fun `a sweep walks its range by index and stops at the top`() {
        val session = session()
        session.selectFunction(RemoteFunction.KEYSTONE)
        session.setMode(KeyLabMode.SWEEP)
        session.setSweepRunning(true)

        val sweep = KeyLabCandidates.CONSUMER_SWEEP
        (1 until sweep.size).forEach { index ->
            assertTrue("step $index", session.advanceSweep())
            assertEquals(index, session.state.value.sweep?.index)
            assertEquals(sweep.candidateAt(index), session.state.value.candidate)
        }

        assertEquals("Sweep 16 of 16 · 0x0180–0x018F", session.state.value.positionLabel)
        assertFalse(session.advanceSweep())
        assertFalse("the walk stops rather than wrapping", session.state.value.sweep!!.running)
        assertEquals(sweep.size - 1, session.state.value.sweep?.index)
    }

    @Test
    fun `switching mode restarts the sweep and never leaves it running`() {
        val session = session()
        session.selectFunction(RemoteFunction.SCREEN_FLIP)
        session.setMode(KeyLabMode.SWEEP)
        session.setSweepRunning(true)
        session.advanceSweep()

        session.setMode(KeyLabMode.CANDIDATES)

        assertEquals(0, session.state.value.sweep?.index)
        assertFalse(session.state.value.sweep!!.running)
    }

    @Test
    fun `sweep mode is refused for a function KEY_LAB gives no range`() {
        val session = session()
        session.setMode(KeyLabMode.SWEEP)

        assertEquals(KeyLabMode.CANDIDATES, session.state.value.mode)
        assertFalse(session.state.value.hasSweep)
    }

    @Test
    fun `manual entry accepts hex with or without the prefix`() {
        val session = session()
        session.setMode(KeyLabMode.MANUAL)
        session.setManualUsage("0x022d")

        val state = session.state.value
        assertNull(state.manual.error)
        assertEquals(0x022D, state.manual.usage)
        assertEquals(ReportKind.CONSUMER, state.candidate?.kind)
        assertEquals(0x022D, state.candidate?.usage)

        session.setManualUsage("22D")
        assertEquals(0x022D, session.state.value.manual.usage)
    }

    @Test
    fun `manual entry refuses a consumer usage above the range the descriptor declares`() {
        val session = session()
        session.setMode(KeyLabMode.MANUAL)
        session.setManualUsage("0400")

        val state = session.state.value
        assertNull(state.manual.usage)
        assertNull("nothing may be sent", state.candidate)
        assertNotNull(state.manual.error)
        assertTrue(state.manual.error!!, state.manual.error!!.contains("0x03FF"))

        session.setManualUsage("03FF")
        assertEquals(0x03FF, session.state.value.manual.usage)
    }

    @Test
    fun `manual entry refuses a keyboard usage above 0xFF`() {
        val session = session()
        session.setMode(KeyLabMode.MANUAL)
        session.setManualKind(ReportKind.KEYBOARD)
        session.setManualUsage("100")

        assertNull(session.state.value.manual.usage)
        assertEquals("Keyboard usages are 0x00–0xFF.", session.state.value.manual.error)

        session.setManualUsage("FF")
        assertEquals(0xFF, session.state.value.manual.usage)
    }

    @Test
    fun `an empty or unreadable manual entry is not an error the operator has to clear`() {
        val session = session()
        session.setMode(KeyLabMode.MANUAL)

        assertNull(session.state.value.manual.error)
        assertNull(session.state.value.candidate)

        session.setManualUsage("zz")
        assertNotNull(session.state.value.manual.error)
        assertNull(session.state.value.candidate)
    }

    @Test
    fun `a manual miss leaves the entry alone, because the operator chooses what is next`() {
        val session = session()
        session.setMode(KeyLabMode.MANUAL)
        session.setManualUsage("0180")

        session.nextCandidate()

        assertEquals(RemoteFunction.FOCUS_UP, session.state.value.function)
        assertEquals(KeyLabMode.MANUAL, session.state.value.mode)
        assertEquals(0x0180, session.state.value.manual.usage)
    }

    @Test
    fun `selecting a function starts it clean`() {
        val session = session()
        session.nextCandidate()
        session.setMode(KeyLabMode.MANUAL)
        session.setManualUsage("0180")

        session.selectFunction(RemoteFunction.SOURCE)

        val state = session.state.value
        assertEquals(RemoteFunction.SOURCE, state.function)
        assertEquals(0, state.candidateIndex)
        assertEquals(KeyLabMode.CANDIDATES, state.mode)
        assertEquals("", state.manual.usageText)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a function that is not under test cannot be selected`() {
        session().selectFunction(RemoteFunction.OK)
    }
}

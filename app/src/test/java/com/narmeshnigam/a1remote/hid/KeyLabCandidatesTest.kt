package com.narmeshnigam.a1remote.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The candidate lists against KEY_LAB.md, entry by entry.
 *
 * These assertions are transcription checks, not behaviour: if the table in KEY_LAB.md and the
 * table in the app ever disagree, the operator tests one thing and records another.
 */
class KeyLabCandidatesTest {

    @Test
    fun `focus up is the candidates KEY_LAB names, in order`() {
        assertEquals(
            listOf(
                KeyLabCandidate(ReportKind.CONSUMER, 0x022D, "Zoom In"),
                KeyLabCandidate(ReportKind.KEYBOARD, 0x3E, "F5"),
                KeyLabCandidate(ReportKind.CONSUMER, 0x0225, "AC Forward"),
                KeyLabCandidate(ReportKind.KEYBOARD, 0x57, "Keypad +"),
                KeyLabCandidate(ReportKind.KEYBOARD, 0x2E, "Equals"),
                KeyLabCandidate(ReportKind.KEYBOARD, 0x4B, "Page Up"),
                KeyLabCandidate(ReportKind.CONSUMER, 0x022F, "Zoom"),
            ),
            KeyLabCandidates.listFor(RemoteFunction.FOCUS_UP),
        )
    }

    @Test
    fun `every list starts with the guess the shipped key map already carries`() {
        listOf(
            RemoteFunction.FOCUS_UP to ConsumerUsage.ZOOM_IN,
            RemoteFunction.FOCUS_DOWN to ConsumerUsage.ZOOM_OUT,
            RemoteFunction.SOURCE to ConsumerUsage.MEDIA_SELECT_TV,
            RemoteFunction.POWER to ConsumerUsage.POWER,
        ).forEach { (function, usage) ->
            val first = KeyLabCandidates.listFor(function).first()
            assertEquals(function.name, usage, first.usage)
            assertEquals(function.name, DefaultKeyMap[function].report, first.report)
        }
    }

    @Test
    fun `the six unresolved functions are the ones under test`() {
        assertEquals(
            listOf(
                RemoteFunction.FOCUS_UP,
                RemoteFunction.FOCUS_DOWN,
                RemoteFunction.SOURCE,
                RemoteFunction.SCREEN_FLIP,
                RemoteFunction.KEYSTONE,
                RemoteFunction.POWER,
            ),
            KeyLabCandidates.FUNCTIONS,
        )
    }

    @Test
    fun `a confirmed function has no candidates to try`() {
        assertTrue(KeyLabCandidates.listFor(RemoteFunction.OK).isEmpty())
        assertNull(KeyLabCandidates.sweepFor(RemoteFunction.OK))
    }

    @Test
    fun `every unresolved function except power carries a sweep`() {
        assertEquals(KeyLabCandidates.HIGH_FUNCTION_KEY_SWEEP, KeyLabCandidates.sweepFor(RemoteFunction.SCREEN_FLIP))
        assertEquals(KeyLabCandidates.CONSUMER_SWEEP, KeyLabCandidates.sweepFor(RemoteFunction.KEYSTONE))
        listOf(RemoteFunction.FOCUS_UP, RemoteFunction.FOCUS_DOWN, RemoteFunction.SOURCE).forEach { function ->
            assertEquals(function.name, KeyLabCandidates.LOW_FUNCTION_KEY_SWEEP, KeyLabCandidates.sweepFor(function))
        }
        // Power stays list-only: a sweep that walks into a working power-off mid-run would take
        // the projector down before the operator could say which code did it.
        assertNull(KeyLabCandidates.sweepFor(RemoteFunction.POWER))
    }

    @Test
    fun `the focus sweep walks F1 to F12 and touches nothing else`() {
        val sweep = KeyLabCandidates.LOW_FUNCTION_KEY_SWEEP
        assertEquals(12, sweep.size)
        assertEquals(KeyLabCandidate(ReportKind.KEYBOARD, 0x3A, "F1"), sweep.candidateAt(0))
        assertEquals(KeyLabCandidate(ReportKind.KEYBOARD, 0x45, "F12"), sweep.candidateAt(11))
        assertEquals("0x3A–0x45", sweep.describe())
    }

    @Test
    fun `the keyboard sweep walks F13 to F24`() {
        val sweep = KeyLabCandidates.HIGH_FUNCTION_KEY_SWEEP
        assertEquals(12, sweep.size)
        assertEquals(KeyLabCandidate(ReportKind.KEYBOARD, 0x68, "F13"), sweep.candidateAt(0))
        assertEquals(KeyLabCandidate(ReportKind.KEYBOARD, 0x73, "F24"), sweep.candidateAt(11))
        assertEquals("0x68–0x73", sweep.describe())
    }

    @Test
    fun `the consumer sweep walks 0x0180 to 0x018F and names nothing it cannot`() {
        val sweep = KeyLabCandidates.CONSUMER_SWEEP
        assertEquals(16, sweep.size)
        assertEquals(0x0180, sweep.candidateAt(0).usage)
        assertEquals(0x018F, sweep.candidateAt(15).usage)
        assertEquals("Consumer 0x0185", sweep.candidateAt(5).usageName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a sweep index past the top is refused`() {
        KeyLabCandidates.CONSUMER_SWEEP.candidateAt(16)
    }

    @Test
    fun `every candidate and sweep entry fits the declared descriptor ranges`() {
        val entries = KeyLabCandidates.FUNCTIONS.flatMap { function ->
            val sweep = KeyLabCandidates.sweepFor(function)
            KeyLabCandidates.listFor(function) + (0 until (sweep?.size ?: 0)).map { sweep!!.candidateAt(it) }
        }
        assertTrue(entries.isNotEmpty())
        entries.forEach { candidate ->
            assertTrue(candidate.describe(), candidate.usage in 0..candidate.kind.usageMax)
            assertNotNull(candidate.describe(), ReportKind.of(candidate.report))
        }
    }

    @Test
    fun `a candidate describes itself the way the findings file spells it`() {
        val candidate = KeyLabCandidate(ReportKind.CONSUMER, 0x022D, "Zoom In")
        assertEquals("0x022D", candidate.hexUsage())
        assertEquals("Consumer 0x022D Zoom In", candidate.describe())
    }
}

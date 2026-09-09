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
    fun `power off is the candidates KEY_LAB names, in order`() {
        assertEquals(
            listOf(
                KeyLabCandidate(ReportKind.CONSUMER, 0x0030, "Power"),
                KeyLabCandidate(ReportKind.CONSUMER, 0x0032, "Sleep"),
                KeyLabCandidate(ReportKind.KEYBOARD, 0x66, "Power"),
            ),
            KeyLabCandidates.listFor(RemoteFunction.POWER),
        )
    }

    @Test
    fun `the list starts with the guess the shipped key map already carries`() {
        val first = KeyLabCandidates.listFor(RemoteFunction.POWER).first()
        assertEquals(ConsumerUsage.POWER, first.usage)
        assertEquals(DefaultKeyMap[RemoteFunction.POWER].report, first.report)
    }

    @Test
    fun `power off is the one function still under test`() {
        // Focus ±, Source, Screen flip and Keystone were removed from the app once every code
        // in their lists and sweeps had been walked against the A1 with no reaction.
        assertEquals(listOf(RemoteFunction.POWER), KeyLabCandidates.FUNCTIONS)
    }

    @Test
    fun `a confirmed function has no candidates to try`() {
        assertTrue(KeyLabCandidates.listFor(RemoteFunction.OK).isEmpty())
        assertNull(KeyLabCandidates.sweepFor(RemoteFunction.OK))
    }

    @Test
    fun `no function carries a sweep, power least of all`() {
        RemoteFunction.entries.forEach { function ->
            assertNull(function.name, KeyLabCandidates.sweepFor(function))
        }
        // Power is the deliberate one: a sweep that walked into a working power-off mid-run
        // would take the projector down before the operator could say which code did it.
        assertNull(KeyLabCandidates.sweepFor(RemoteFunction.POWER))
    }

    @Test
    fun `the low function-key sweep walks F1 to F12 and touches nothing else`() {
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
        val sweeps = listOf(
            KeyLabCandidates.LOW_FUNCTION_KEY_SWEEP,
            KeyLabCandidates.HIGH_FUNCTION_KEY_SWEEP,
            KeyLabCandidates.CONSUMER_SWEEP,
        )
        val entries = KeyLabCandidates.FUNCTIONS.flatMap(KeyLabCandidates::listFor) +
            sweeps.flatMap { sweep -> (0 until sweep.size).map(sweep::candidateAt) }

        assertTrue(entries.isNotEmpty())
        entries.forEach { candidate ->
            assertTrue(candidate.describe(), candidate.usage in 0..candidate.kind.usageMax)
            assertNotNull(candidate.describe(), ReportKind.of(candidate.report))
        }
    }

    @Test
    fun `a candidate describes itself the way the findings file spells it`() {
        val candidate = KeyLabCandidate(ReportKind.CONSUMER, 0x0030, "Power")
        assertEquals("0x0030", candidate.hexUsage())
        assertEquals("Consumer 0x0030 Power", candidate.describe())
    }
}

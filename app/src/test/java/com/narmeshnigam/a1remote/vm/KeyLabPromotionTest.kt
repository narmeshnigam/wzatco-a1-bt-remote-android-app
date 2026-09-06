package com.narmeshnigam.a1remote.vm

import com.narmeshnigam.a1remote.data.FakeFindingsStore
import com.narmeshnigam.a1remote.data.FakeKeyMapStore
import com.narmeshnigam.a1remote.data.Finding
import com.narmeshnigam.a1remote.data.KeyMap
import com.narmeshnigam.a1remote.data.Verdict
import com.narmeshnigam.a1remote.hid.KeyLabCandidates
import com.narmeshnigam.a1remote.hid.KeyStatus
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.hid.ReportKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What a verdict does to the key map and to the findings — the whole point of Key Lab. */
@OptIn(ExperimentalCoroutinesApi::class)
class KeyLabPromotionTest {

    private val keyMapStore = FakeKeyMapStore()
    private val keyMap = KeyMap(keyMapStore, TestScope(UnconfinedTestDispatcher()))
    private val findings = FakeFindingsStore()

    @Test
    fun `a hit promotes the candidate and flips the key to verified`() = runTest {
        val session = KeyLabSession()
        session.nextCandidate() // the first guess missed; the second is F5
        val candidate = session.advancePast(Verdict.MAPPED)!!

        fileVerdict(RemoteFunction.FOCUS_UP, candidate, Verdict.MAPPED, keyMap, findings)

        assertTrue(keyMap.isVerified(RemoteFunction.FOCUS_UP))
        assertEquals(candidate.report, keyMap.reportFor(RemoteFunction.FOCUS_UP))
        assertEquals("F5", keyMap[RemoteFunction.FOCUS_UP].usageName)
        assertEquals(KeyStatus.CONFIRMED, keyMap[RemoteFunction.FOCUS_UP].status)
    }

    @Test
    fun `a hit is filed as mapped with the usage that was actually sent`() = runTest {
        val candidate = KeyLabCandidates.listFor(RemoteFunction.SOURCE).first()

        fileVerdict(RemoteFunction.SOURCE, candidate, Verdict.MAPPED, keyMap, findings)

        assertEquals(
            listOf(
                Finding(RemoteFunction.SOURCE, ReportKind.CONSUMER, 0x0089, "Media Select TV", Verdict.MAPPED),
            ),
            findings.recorded,
        )
    }

    @Test
    fun `a miss is filed and changes nothing about the key map`() = runTest {
        val candidate = KeyLabCandidates.listFor(RemoteFunction.SCREEN_FLIP).first()

        fileVerdict(RemoteFunction.SCREEN_FLIP, candidate, Verdict.NO_EFFECT, keyMap, findings)

        assertEquals(Verdict.NO_EFFECT, findings.recorded.single().verdict)
        assertFalse(keyMap.isVerified(RemoteFunction.SCREEN_FLIP))
        assertNull("an unproven function stays unmapped", keyMap.reportFor(RemoteFunction.SCREEN_FLIP))
    }

    @Test
    fun `a side effect is kept but promotes nothing`() = runTest {
        val candidate = KeyLabCandidates.CONSUMER_SWEEP.candidateAt(3)

        fileVerdict(RemoteFunction.KEYSTONE, candidate, Verdict.SIDE_EFFECT, keyMap, findings)

        assertEquals(Verdict.SIDE_EFFECT, findings.recorded.single().verdict)
        assertEquals(0x0183, findings.recorded.single().usage)
        assertFalse(keyMap.isVerified(RemoteFunction.KEYSTONE))
        assertNull(keyMap.reportFor(RemoteFunction.KEYSTONE))
    }

    @Test
    fun `every miss is kept, so an unreachable function has its evidence`() = runTest {
        val session = KeyLabSession()
        session.selectFunction(RemoteFunction.FOCUS_DOWN)
        KeyLabCandidates.listFor(RemoteFunction.FOCUS_DOWN).forEach { _ ->
            val candidate = session.advancePast(Verdict.NO_EFFECT)!!
            fileVerdict(RemoteFunction.FOCUS_DOWN, candidate, Verdict.NO_EFFECT, keyMap, findings)
        }

        assertEquals(
            KeyLabCandidates.listFor(RemoteFunction.FOCUS_DOWN).map { it.usage },
            findings.recorded.map { it.usage },
        )
        assertFalse(keyMap.isVerified(RemoteFunction.FOCUS_DOWN))
        // Focus now carries a sweep, so an exhausted list falls into it rather than giving up on
        // the function: the evidence is filed and there is still range left to walk.
        assertEquals(RemoteFunction.FOCUS_DOWN, session.state.value.function)
        assertEquals(KeyLabMode.SWEEP, session.state.value.mode)
    }
}

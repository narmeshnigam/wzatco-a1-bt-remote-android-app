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
import org.junit.Assert.assertTrue
import org.junit.Test

/** What a verdict does to the key map and to the findings — the whole point of Key Lab. */
@OptIn(ExperimentalCoroutinesApi::class)
class KeyLabPromotionTest {

    private val keyMapStore = FakeKeyMapStore()
    private val keyMap = KeyMap(keyMapStore, TestScope(UnconfinedTestDispatcher()))
    private val findings = FakeFindingsStore()

    private val powerCodes = KeyLabCandidates.listFor(RemoteFunction.POWER)

    @Test
    fun `a hit promotes the candidate and flips the key to verified`() = runTest {
        val session = KeyLabSession()
        session.nextCandidate() // the first guess missed; the second is Consumer Sleep
        val candidate = session.advancePast(Verdict.MAPPED)!!

        fileVerdict(RemoteFunction.POWER, candidate, Verdict.MAPPED, keyMap, findings)

        assertTrue(keyMap.isVerified(RemoteFunction.POWER))
        assertEquals(candidate.report, keyMap.reportFor(RemoteFunction.POWER))
        assertEquals("Sleep", keyMap[RemoteFunction.POWER].usageName)
        assertEquals(KeyStatus.CONFIRMED, keyMap[RemoteFunction.POWER].status)
    }

    @Test
    fun `a hit is filed as mapped with the usage that was actually sent`() = runTest {
        val candidate = powerCodes.first()

        fileVerdict(RemoteFunction.POWER, candidate, Verdict.MAPPED, keyMap, findings)

        assertEquals(
            listOf(Finding(RemoteFunction.POWER, ReportKind.CONSUMER, 0x0030, "Power", Verdict.MAPPED)),
            findings.recorded,
        )
    }

    @Test
    fun `a miss is filed and changes nothing about the key map`() = runTest {
        val candidate = powerCodes.first()

        fileVerdict(RemoteFunction.POWER, candidate, Verdict.NO_EFFECT, keyMap, findings)

        assertEquals(Verdict.NO_EFFECT, findings.recorded.single().verdict)
        assertFalse(keyMap.isVerified(RemoteFunction.POWER))
        // The shipped guess is still what the map carries — a miss proves nothing new about it.
        assertEquals(KeyStatus.CANDIDATE, keyMap[RemoteFunction.POWER].status)
    }

    @Test
    fun `a side effect is kept but promotes nothing`() = runTest {
        val candidate = KeyLabCandidates.CONSUMER_SWEEP.candidateAt(3)

        fileVerdict(RemoteFunction.POWER, candidate, Verdict.SIDE_EFFECT, keyMap, findings)

        assertEquals(Verdict.SIDE_EFFECT, findings.recorded.single().verdict)
        assertEquals(0x0183, findings.recorded.single().usage)
        assertFalse(keyMap.isVerified(RemoteFunction.POWER))
        assertEquals(KeyStatus.CANDIDATE, keyMap[RemoteFunction.POWER].status)
    }

    @Test
    fun `every miss is kept, so an unreachable function has its evidence`() = runTest {
        val session = KeyLabSession()
        powerCodes.forEach { _ ->
            val candidate = session.advancePast(Verdict.NO_EFFECT)!!
            fileVerdict(RemoteFunction.POWER, candidate, Verdict.NO_EFFECT, keyMap, findings)
        }

        assertEquals(powerCodes.map { it.usage }, findings.recorded.map { it.usage })
        assertFalse(keyMap.isVerified(RemoteFunction.POWER))
        // Power carries no sweep, and it is the only function under test, so an exhausted list
        // comes back round to the top of the same list rather than ending the pass.
        assertEquals(RemoteFunction.POWER, session.state.value.function)
        assertEquals(KeyLabMode.CANDIDATES, session.state.value.mode)
        assertEquals(0, session.state.value.candidateIndex)
    }
}

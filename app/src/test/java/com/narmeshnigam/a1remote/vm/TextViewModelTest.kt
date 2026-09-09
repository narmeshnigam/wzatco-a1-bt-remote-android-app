package com.narmeshnigam.a1remote.vm

import com.narmeshnigam.a1remote.hid.HidReports
import com.narmeshnigam.a1remote.hid.KeyboardModifier
import com.narmeshnigam.a1remote.hid.TextTyping
import com.narmeshnigam.a1remote.service.FakeHidTransport
import com.narmeshnigam.a1remote.service.HidLink
import com.narmeshnigam.a1remote.service.SendResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Typing a whole string onto the wire, on virtual time.
 *
 * The run is a coroutine that walks a string one report at a time, and the two things that
 * matter about it cannot be seen by looking at the projector: that every keystroke is released
 * before the next is pressed, and that a refusal stops the run instead of carrying on into a
 * field that is already wrong.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TextViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var transport: FakeHidTransport

    @Before
    fun install() {
        Dispatchers.setMain(dispatcher)
        transport = FakeHidTransport()
        HidLink.transport = transport
    }

    @After
    fun remove() {
        HidLink.transport = null
        HidLink.clearWireLog()
        Dispatchers.resetMain()
    }

    private fun viewModel(draft: String) = TextViewModel().apply { setDraft(draft) }

    @Test
    fun `the draft is not transmitted as it is typed`() = runTest(dispatcher) {
        viewModel("secret")
        advanceUntilIdle()

        assertTrue("typing on the phone must put nothing on the wire", transport.sent.isEmpty())
    }

    @Test
    fun `sending walks the string in order, releasing every key`() = runTest(dispatcher) {
        val model = viewModel("Hi")

        model.sendDraft()
        advanceUntilIdle()

        assertEquals(
            listOf(
                HidReports.keyboard(KeyboardModifier.LEFT_SHIFT, 0x0B),
                HidReports.keyboardRelease(),
                HidReports.keyboard(KeyboardModifier.NONE, 0x0C),
                HidReports.keyboardRelease(),
            ),
            transport.sent,
        )
        assertTrue(transport.everyPressWasReleased())
        assertNull("the run is over", model.progress.value)
        assertEquals("Typed 2 characters.", model.note.value)
    }

    @Test
    fun `progress counts the keystrokes that have actually gone out`() = runTest(dispatcher) {
        val model = viewModel("abc")

        model.sendDraft()
        // The loop sends, then waits out the gap. One gap in, exactly one key has been typed.
        advanceTimeBy(1.milliseconds)
        assertEquals(TypingProgress(sent = 1, total = 3), model.progress.value)

        advanceUntilIdle()
        assertNull(model.progress.value)
        assertEquals(6, transport.sent.size)
    }

    @Test
    fun `a refusal part way through stops the run and says how far it got`() = runTest(dispatcher) {
        transport.refusePressFrom = 2
        val model = viewModel("abcd")

        model.sendDraft()
        advanceUntilIdle()

        assertEquals(SendResult.FAILED, model.lastResult.value)
        assertEquals("Stopped after 2 of 4. The field holds only that much.", model.note.value)
        assertNull(model.progress.value)
        assertEquals("only the two accepted presses reached the wire", 4, transport.sent.size)
    }

    @Test
    fun `nothing is sent over a dead link`() = runTest(dispatcher) {
        transport.connected = false
        val model = viewModel("abc")

        model.sendDraft()
        advanceUntilIdle()

        assertEquals(SendResult.NOT_CONNECTED, model.lastResult.value)
        assertEquals("Stopped after 0 of 3. The field holds only that much.", model.note.value)
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun `a draft with an untypable character sends nothing at all`() = runTest(dispatcher) {
        val model = viewModel("café")

        model.sendDraft()
        advanceUntilIdle()

        assertTrue("half a string in a field is worse than none", transport.sent.isEmpty())
        assertEquals(listOf('é'), model.untypable)
    }

    @Test
    fun `an empty draft sends nothing`() = runTest(dispatcher) {
        val model = viewModel("")

        model.sendDraft()
        advanceUntilIdle()

        assertTrue(transport.sent.isEmpty())
        assertNull(model.progress.value)
    }

    @Test
    fun `a second send while one is running is ignored rather than interleaved`() = runTest(dispatcher) {
        val model = viewModel("abc")

        model.sendDraft()
        advanceTimeBy(1.milliseconds)
        model.sendDraft()
        advanceUntilIdle()

        assertEquals("three characters, not six", 6, transport.sent.size)
    }

    @Test
    fun `stopping ends the run part way and does not pretend it can be undone`() = runTest(dispatcher) {
        val model = viewModel("abcdef")

        model.sendDraft()
        advanceTimeBy(1.milliseconds)
        model.stop()
        advanceUntilIdle()

        assertEquals("Stopped. 1 went out; nothing sent can be taken back.", model.note.value)
        assertNull(model.progress.value)
        assertEquals("what had gone stays gone", 2, transport.sent.size)
    }

    @Test
    fun `a named key goes straight out on its own`() = runTest(dispatcher) {
        val model = viewModel("")

        model.sendKeystroke(TextTyping.ENTER)

        assertEquals(listOf(HidReports.key(0x28), HidReports.keyboardRelease()), transport.sent)
        assertEquals(SendResult.SENT, model.lastResult.value)
        assertEquals("Enter", transport.labels.single())
    }

    @Test
    fun `clearing the draft clears what the screen says about it`() = runTest(dispatcher) {
        val model = viewModel("abc")
        model.sendDraft()
        advanceUntilIdle()

        model.clearDraft()

        assertEquals("", model.draft.value)
        assertNull(model.note.value)
        assertNull(model.lastResult.value)
    }
}

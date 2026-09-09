package com.narmeshnigam.a1remote.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.narmeshnigam.a1remote.service.HidLink
import com.narmeshnigam.a1remote.service.LinkStage
import com.narmeshnigam.a1remote.vm.TextViewModel
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The keys of the keyboard screen, by the label the tree finds them under. */
private const val SEND = "Send text"
private const val CLEAR = "Clear"
private const val BACKSPACE = "Backspace"
private const val PLACEHOLDER = "Type what the projector should receive"

@RunWith(AndroidJUnit4::class)
class KeyboardScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @After
    fun disconnect() {
        HidLink.reset()
    }

    private fun setKeyboard(connected: Boolean): TextViewModel {
        // The screen reads the link straight off HidLink, so the link is what the test sets.
        HidLink.update { state ->
            state.copy(stage = if (connected) LinkStage.CONNECTED else LinkStage.STOPPED, hostName = "A1")
        }
        val model = TextViewModel()
        compose.setContent { KeyboardScreen(viewModel = model) }
        compose.awaitKey(SEND)
        return model
    }

    @Test
    fun nothingCanBeSentWithNoHost() {
        setKeyboard(connected = false)
        compose.onNode(hasSetTextAction()).performTextInput("hello")
        compose.waitForIdle()

        compose.onNodeWithContentDescription(SEND).assertIsNotEnabled()
        compose.onNodeWithContentDescription(BACKSPACE).assertIsNotEnabled()
    }

    @Test
    fun aTypableDraftArmsSendOnceAHostIsThere() {
        setKeyboard(connected = true)
        compose.onNodeWithContentDescription(SEND).assertIsNotEnabled()

        compose.onNode(hasSetTextAction()).performTextInput("wifi-pass_1")
        compose.waitForIdle()

        compose.onNodeWithContentDescription(SEND).assertIsEnabled()
        compose.onNodeWithContentDescription(CLEAR).assertIsEnabled()
    }

    @Test
    fun aCharacterWithNoKeyBlocksTheWholeSendAndSaysWhich() {
        setKeyboard(connected = true)

        compose.onNode(hasSetTextAction()).performTextInput("café")
        compose.waitForIdle()

        compose.onNodeWithContentDescription(SEND).assertIsNotEnabled()
        compose.onNodeWithText("Cannot type 'é'", substring = true).assertExists()
    }

    @Test
    fun clearingEmptiesTheDraftAndDisarmsSend() {
        val model = setKeyboard(connected = true)
        compose.onNode(hasSetTextAction()).performTextInput("hello")
        compose.waitForIdle()

        compose.onNodeWithContentDescription(CLEAR).performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription(SEND).assertIsNotEnabled()
        compose.onNodeWithText(PLACEHOLDER).assertExists()
        assertTrue(model.draft.value.isEmpty())
    }
}

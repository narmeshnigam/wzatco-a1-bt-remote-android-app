package com.narmeshnigam.a1remote.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.narmeshnigam.a1remote.hid.DefaultKeyMap
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every key label on the keypad, in layout order. */
private val KEY_LABELS = listOf(
    "Up", "Left", "OK", "Right", "Down",
    "Back", "Home", "Menu",
    "Vol −", "Mute", "Vol +",
    "Focus −", "Focus +", "Source",
    "Flip", "Keystone", "Cursor",
)

@RunWith(AndroidJUnit4::class)
class KeypadScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setKeypad(connected: Boolean, onPress: (RemoteFunction) -> Unit = {}) {
        compose.setContent {
            KeypadScreen(
                bindings = DefaultKeyMap.all(),
                connected = connected,
                onPress = onPress,
                onOpenCursor = {},
            )
        }
    }

    @Test
    fun everyKeyIsPresentAndReachable() {
        setKeypad(connected = true)
        KEY_LABELS.forEach { label ->
            compose.onNodeWithContentDescription(label).assertExists()
        }
    }

    @Test
    fun everyKeyMeetsTheMinimumTouchTarget() {
        setKeypad(connected = true)
        KEY_LABELS.forEach { label ->
            compose.onNodeWithContentDescription(label)
                .assertWidthIsAtLeast(A1Dimens.MinTouch)
                .assertHeightIsAtLeast(A1Dimens.MinTouch)
        }
    }

    @Test
    fun noReportIsSentWhileDisconnected() {
        val pressed = mutableListOf<RemoteFunction>()
        setKeypad(connected = false) { pressed += it }

        KEY_LABELS.filter { it != "Cursor" }.forEach { label ->
            compose.onNodeWithContentDescription(label).performClick()
        }
        compose.waitForIdle()

        assertTrue("nothing may be transmitted over a dead link, got $pressed", pressed.isEmpty())
    }

    @Test
    fun everyFunctionKeyIsDisabledWhileDisconnected() {
        setKeypad(connected = false)
        KEY_LABELS.filter { it != "Cursor" }.forEach { label ->
            compose.onNodeWithContentDescription(label).assertIsNotEnabled()
        }
    }

    @Test
    fun aConnectedKeyDispatchesItsOwnFunction() {
        val pressed = mutableListOf<RemoteFunction>()
        setKeypad(connected = true) { pressed += it }

        compose.onNodeWithContentDescription("Down").performClick()
        compose.waitForIdle()

        assertEquals(listOf(RemoteFunction.DOWN), pressed)
    }

    @Test
    fun theCursorKeyStaysUsableWhileDisconnected() {
        var opened = false
        compose.setContent {
            KeypadScreen(
                bindings = DefaultKeyMap.all(),
                connected = false,
                onPress = {},
                onOpenCursor = { opened = true },
            )
        }

        compose.onNodeWithContentDescription("Cursor").performClick()
        compose.waitForIdle()

        assertTrue("switching screens is not a transmission and must still work", opened)
    }
}

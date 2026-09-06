package com.narmeshnigam.a1remote.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.narmeshnigam.a1remote.hid.DefaultKeyMap
import com.narmeshnigam.a1remote.hid.KeyStatus
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every function key on the keypad, in layout order, keyed by the label it is found by. */
private val FUNCTION_KEYS = linkedMapOf(
    "Up" to RemoteFunction.UP,
    "Left" to RemoteFunction.LEFT,
    "OK" to RemoteFunction.OK,
    "Right" to RemoteFunction.RIGHT,
    "Down" to RemoteFunction.DOWN,
    "Back" to RemoteFunction.BACK,
    "Home" to RemoteFunction.HOME,
    "Menu" to RemoteFunction.MENU,
    "Vol −" to RemoteFunction.VOLUME_DOWN,
    "Mute" to RemoteFunction.MUTE,
    "Vol +" to RemoteFunction.VOLUME_UP,
    "Focus −" to RemoteFunction.FOCUS_DOWN,
    "Focus +" to RemoteFunction.FOCUS_UP,
    "Source" to RemoteFunction.SOURCE,
    "Flip" to RemoteFunction.SCREEN_FLIP,
    "Keystone" to RemoteFunction.KEYSTONE,
)

/** The one key that is not a function: it switches screens. */
private const val TRACKPAD = "Trackpad"

/** The shipped map decides which keys are confirmed; the test follows it rather than restating it. */
private fun isConfirmed(function: RemoteFunction): Boolean = DefaultKeyMap[function].status == KeyStatus.CONFIRMED

private val CONFIRMED_LABELS = FUNCTION_KEYS.filterValues(::isConfirmed).keys
private val UNVERIFIED_LABELS = FUNCTION_KEYS.filterValues { !isConfirmed(it) }.keys

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
        compose.awaitKey("Up")
    }

    @Test
    fun theShippedMapStillSplitsTheKeypadBothWays() {
        // Guards the assertions below against becoming vacuous if the default map ever changes.
        assertTrue("some keys must be confirmed", CONFIRMED_LABELS.isNotEmpty())
        assertTrue("some keys must still be unverified", UNVERIFIED_LABELS.isNotEmpty())
    }

    @Test
    fun everyKeyIsPresentAndReachable() {
        setKeypad(connected = true)
        (FUNCTION_KEYS.keys + TRACKPAD).forEach { label ->
            compose.onNodeWithContentDescription(label).assertExists()
        }
    }

    @Test
    fun everyKeyMeetsTheMinimumTouchTarget() {
        setKeypad(connected = true)
        (FUNCTION_KEYS.keys + TRACKPAD).forEach { label ->
            compose.onNodeWithContentDescription(label)
                .assertWidthIsAtLeast(A1Dimens.MinTouch)
                .assertHeightIsAtLeast(A1Dimens.MinTouch)
        }
    }

    @Test
    fun noConfirmedKeyDispatchesWhileDisconnected() {
        val pressed = mutableListOf<RemoteFunction>()
        setKeypad(connected = false) { pressed += it }

        CONFIRMED_LABELS.forEach { label ->
            compose.onNodeWithContentDescription(label).performClick()
        }
        compose.waitForIdle()

        assertTrue("nothing may be transmitted over a dead link, got $pressed", pressed.isEmpty())
    }

    @Test
    fun everyConfirmedKeyIsDisabledWhileDisconnected() {
        setKeypad(connected = false)
        CONFIRMED_LABELS.forEach { label ->
            compose.onNodeWithContentDescription(label).assertIsNotEnabled()
        }
    }

    @Test
    fun anUnverifiedKeyStaysLiveWhileDisconnectedAndHandsUpItsFunction() {
        // An unverified key does not send; its press is a route to Fix Keys, made by the caller.
        // Routing is not a transmission, so the key must stay usable with no host at all.
        val pressed = mutableListOf<RemoteFunction>()
        setKeypad(connected = false) { pressed += it }

        UNVERIFIED_LABELS.forEach { label ->
            compose.onNodeWithContentDescription(label).assertIsEnabled().performClick()
        }
        compose.waitForIdle()

        assertEquals(UNVERIFIED_LABELS.map { FUNCTION_KEYS.getValue(it) }, pressed)
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
    fun theTrackpadKeyStaysUsableWhileDisconnected() {
        var opened = false
        compose.setContent {
            KeypadScreen(
                bindings = DefaultKeyMap.all(),
                connected = false,
                onPress = {},
                onOpenCursor = { opened = true },
            )
        }
        compose.awaitKey(TRACKPAD)

        compose.onNodeWithContentDescription(TRACKPAD).performClick()
        compose.waitForIdle()

        assertTrue("switching screens is not a transmission and must still work", opened)
    }
}

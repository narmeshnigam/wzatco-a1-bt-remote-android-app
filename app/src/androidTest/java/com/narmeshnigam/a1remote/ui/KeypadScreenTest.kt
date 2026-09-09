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
import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.KeyStatus
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every key on the keypad, in layout order, keyed by the label it is found by. */
private val FUNCTION_KEYS = linkedMapOf(
    "Up" to RemoteFunction.UP,
    "Left" to RemoteFunction.LEFT,
    "OK" to RemoteFunction.OK,
    "Right" to RemoteFunction.RIGHT,
    "Down" to RemoteFunction.DOWN,
    "Vol +" to RemoteFunction.VOLUME_UP,
    "Home" to RemoteFunction.HOME,
    "Back" to RemoteFunction.BACK,
    "Vol −" to RemoteFunction.VOLUME_DOWN,
    "Menu" to RemoteFunction.MENU,
    "Mute" to RemoteFunction.MUTE,
)

/**
 * A key map in which Mute is not yet proven.
 *
 * Every function still on the keypad is confirmed on the A1, so the unverified style has no
 * shipped example left to test against. Rather than delete the assertions that the style works —
 * it is how the app tells the truth about what it knows — one binding is demoted here.
 */
private val WITH_AN_UNVERIFIED_KEY: Map<RemoteFunction, KeyBinding> = DefaultKeyMap.all() +
    mapOf(RemoteFunction.MUTE to DefaultKeyMap[RemoteFunction.MUTE].copy(status = KeyStatus.CANDIDATE))

private fun isConfirmed(function: RemoteFunction): Boolean =
    WITH_AN_UNVERIFIED_KEY.getValue(function).status == KeyStatus.CONFIRMED

private val CONFIRMED_LABELS = FUNCTION_KEYS.filterValues(::isConfirmed).keys
private val UNVERIFIED_LABELS = FUNCTION_KEYS.filterValues { !isConfirmed(it) }.keys

@RunWith(AndroidJUnit4::class)
class KeypadScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setKeypad(
        connected: Boolean,
        bindings: Map<RemoteFunction, KeyBinding> = WITH_AN_UNVERIFIED_KEY,
        onPress: (RemoteFunction) -> Unit = {},
    ) {
        compose.setContent {
            KeypadScreen(bindings = bindings, connected = connected, onPress = onPress)
        }
        compose.awaitKey("Up")
    }

    @Test
    fun theShippedMapLeavesNothingOnTheKeypadUnproven() {
        // Every function that survived the layout change is confirmed on the A1. If that ever
        // stops being true, the keypad is carrying a key it cannot drive again.
        val unproven = FUNCTION_KEYS.values.filter { DefaultKeyMap[it].status != KeyStatus.CONFIRMED }
        assertEquals(emptyList<RemoteFunction>(), unproven)
    }

    @Test
    fun everyKeyIsPresentAndReachable() {
        setKeypad(connected = true)
        FUNCTION_KEYS.keys.forEach { label ->
            compose.onNodeWithContentDescription(label).assertExists()
        }
    }

    @Test
    fun theRemovedKeysAreGoneFromTheKeypad() {
        setKeypad(connected = true)
        listOf("Focus +", "Focus −", "Source", "Flip", "Keystone", "Trackpad").forEach { label ->
            compose.onNodeWithContentDescription(label).assertDoesNotExist()
        }
    }

    @Test
    fun everyKeyMeetsTheMinimumTouchTarget() {
        setKeypad(connected = true)
        FUNCTION_KEYS.keys.forEach { label ->
            compose.onNodeWithContentDescription(label)
                .assertWidthIsAtLeast(A1Dimens.MinTouch)
                .assertHeightIsAtLeast(A1Dimens.MinTouch)
        }
    }

    @Test
    fun noConfirmedKeyDispatchesWhileDisconnected() {
        val pressed = mutableListOf<RemoteFunction>()
        setKeypad(connected = false, onPress = { pressed += it })

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
        setKeypad(connected = false, onPress = { pressed += it })

        UNVERIFIED_LABELS.forEach { label ->
            compose.onNodeWithContentDescription(label).assertIsEnabled().performClick()
        }
        compose.waitForIdle()

        assertEquals(UNVERIFIED_LABELS.map { FUNCTION_KEYS.getValue(it) }, pressed)
    }

    @Test
    fun aConnectedKeyDispatchesItsOwnFunction() {
        val pressed = mutableListOf<RemoteFunction>()
        setKeypad(connected = true, onPress = { pressed += it })

        compose.onNodeWithContentDescription("Menu").performClick()
        compose.waitForIdle()

        assertEquals(listOf(RemoteFunction.MENU), pressed)
    }

    @Test
    fun eachArrowOfTheDialDispatchesItsOwnDirection() {
        // The dial resolves a touch by angle rather than by which button it landed on, so every
        // arrow is pressed in turn: a sign error in the geometry would send the opposite one.
        val dial = linkedMapOf(
            "Up" to RemoteFunction.UP,
            "Right" to RemoteFunction.RIGHT,
            "Down" to RemoteFunction.DOWN,
            "Left" to RemoteFunction.LEFT,
            "OK" to RemoteFunction.OK,
        )
        val pressed = mutableListOf<RemoteFunction>()
        setKeypad(connected = true, onPress = { pressed += it })

        dial.keys.forEach { label -> compose.onNodeWithContentDescription(label).performClick() }
        compose.waitForIdle()

        assertEquals(dial.values.toList(), pressed)
    }
}

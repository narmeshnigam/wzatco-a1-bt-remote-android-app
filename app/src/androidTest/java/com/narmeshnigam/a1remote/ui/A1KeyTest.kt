package com.narmeshnigam.a1remote.ui

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.narmeshnigam.a1remote.vm.AutoRepeat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The auto-repeat timing is unit-tested in `AutoRepeatTest`; this checks the key is actually
 * wired to it, and that lifting the finger stops it. A key that keeps firing after release
 * would walk the projector somewhere the user cannot see.
 */
@RunWith(AndroidJUnit4::class)
class A1KeyTest {

    @get:Rule
    val compose = createComposeRule()

    private var presses = 0

    private fun setKey(repeating: Boolean) {
        presses = 0
        compose.setContent {
            A1Key(
                label = "Down",
                onPress = { presses++ },
                repeating = repeating,
                modifier = Modifier.size(80.dp),
            )
        }
        // The key must be in the tree before the clock is paused (see awaitKey).
        compose.awaitKey("Down")
        compose.mainClock.autoAdvance = false
    }

    private fun setTapKey() {
        presses = 0
        compose.setContent {
            A1Key(
                label = "Host",
                onPress = { presses++ },
                tapToClick = true,
                modifier = Modifier.size(80.dp),
            )
        }
        compose.awaitKey("Host")
    }

    @Test
    fun aTapToClickKeyDoesNotFireOnTouchDownAlone() {
        setTapKey()

        compose.onNodeWithContentDescription("Host").performTouchInput { down(center) }
        compose.waitForIdle()
        assertEquals("a touch-down that may become a scroll must not connect", 0, presses)

        compose.onNodeWithContentDescription("Host").performTouchInput { up() }
        compose.waitForIdle()
        assertEquals("the click lands only when the finger lifts on the row", 1, presses)
    }

    @Test
    fun aPressFiresOnKeyDownRatherThanOnRelease() {
        setKey(repeating = false)

        compose.onNodeWithContentDescription("Down").performTouchInput { down(center) }
        compose.mainClock.advanceTimeByFrame()

        assertEquals("the report goes out under the finger, not after it lifts", 1, presses)

        compose.onNodeWithContentDescription("Down").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(AutoRepeat.INITIAL_DELAY_MS * 4)
        assertEquals(1, presses)
    }

    @Test
    fun aNonRepeatingKeyNeverRepeatsHoweverLongItIsHeld() {
        setKey(repeating = false)

        compose.onNodeWithContentDescription("Down").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(AutoRepeat.INITIAL_DELAY_MS + AutoRepeat.INTERVAL_MS * 20)

        assertEquals(1, presses)
    }

    @Test
    fun aRepeatingKeyWaitsTheInitialDelayThenRepeats() {
        setKey(repeating = true)

        compose.onNodeWithContentDescription("Down").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(AutoRepeat.INITIAL_DELAY_MS - 50)
        assertEquals("nothing may repeat before 400 ms", 1, presses)

        compose.mainClock.advanceTimeBy(100)
        assertEquals("the first repeat lands at 400 ms", 2, presses)

        compose.mainClock.advanceTimeBy(AutoRepeat.INTERVAL_MS * 5)
        assertEquals(7, presses)
    }

    @Test
    fun liftingTheFingerStopsTheRepeatImmediately() {
        setKey(repeating = true)

        compose.onNodeWithContentDescription("Down").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(AutoRepeat.INITIAL_DELAY_MS + AutoRepeat.INTERVAL_MS * 3)
        val atRelease = presses

        compose.onNodeWithContentDescription("Down").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(AutoRepeat.INTERVAL_MS * 30)

        assertEquals("no key may keep firing after the finger lifts", atRelease, presses)
    }
}

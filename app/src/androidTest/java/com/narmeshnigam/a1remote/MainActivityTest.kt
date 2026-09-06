package com.narmeshnigam.a1remote

import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.narmeshnigam.a1remote.service.HidLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val permissions = BluetoothPermissionsRule()

    @Test
    fun theScreenStaysAwakeWhileTheRemoteIsInFrontOfTheUser() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val flags = activity.window.attributes.flags
                assertTrue(
                    "FLAG_KEEP_SCREEN_ON must be set while resumed",
                    flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0,
                )
            }

            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { activity ->
                val flags = activity.window.attributes.flags
                assertEquals(
                    "and released the moment the remote is backgrounded",
                    0,
                    flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                )
            }
        }
    }

    @Test
    fun theLinkSurvivesRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val before = HidLink.state.value
            scenario.recreate()
            val after = HidLink.state.value

            // The link belongs to the service, not to any screen. A rotation must not disturb a
            // registration; re-registering per rotation would drop the projector's connection.
            assertEquals(before.stage, after.stage)
            assertEquals(before.registerAppReturned, after.registerAppReturned)
        }
    }

    @Test
    fun theTransportIsHeldWhileTheServiceRuns() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { }
            // The shell starts the service on entry, which installs the transport.
            waitFor { HidLink.transport != null }
            assertNotNull("the service must install its transport", HidLink.transport)
        }
    }

    private fun waitFor(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline && !condition()) {
            Thread.sleep(50)
        }
    }
}

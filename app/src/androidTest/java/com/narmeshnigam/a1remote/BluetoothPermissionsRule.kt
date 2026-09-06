package com.narmeshnigam.a1remote

import androidx.test.platform.app.InstrumentationRegistry
import com.narmeshnigam.a1remote.ui.Permissions
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Holds the app's required Bluetooth permissions for a test that launches the real activity.
 *
 * `GrantPermissionRule` alone is not enough on this phone: OxygenOS/ColorOS refuses shell grants
 * outright — "Neither user 2000 nor current process has GRANT_RUNTIME_PERMISSIONS" — unless the
 * developer switch *Disable permission monitoring* is on, and that switch resets itself. So this
 * rule grants only what the app does not already hold, and when the ROM refuses it says which
 * switch to flip instead of failing every activity test with a bare SecurityException.
 */
class BluetoothPermissionsRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            grantMissing()
            base.evaluate()
        }
    }

    private fun grantMissing() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        Permissions.required()
            .filterNot { Permissions.isGranted(target, it) }
            .forEach { permission ->
                try {
                    instrumentation.uiAutomation.grantRuntimePermission(target.packageName, permission)
                } catch (e: SecurityException) {
                    throw AssertionError(
                        "Could not grant $permission: this ROM blocks shell grants. Either allow " +
                            "Nearby devices for WZATCO A1 Remote by hand, or switch on Developer " +
                            "options → Disable permission monitoring (OxygenOS/ColorOS) and re-run.",
                        e,
                    )
                }
            }
    }
}

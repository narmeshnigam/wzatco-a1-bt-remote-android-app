package com.narmeshnigam.a1remote.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * The runtime permissions this app asks for, split by whether the app can work without them.
 *
 * BUILD_SPEC §7: ask from a rationale screen, never on cold launch, and a denial must leave the
 * app usable and honest about what is blocked.
 */
object Permissions {
    /**
     * Without these there is no HID link at all.
     *
     * On API 28..30 `BLUETOOTH` and `BLUETOOTH_ADMIN` are install-time permissions, so there is
     * nothing to ask for and this list is empty.
     */
    fun required(): List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        emptyList()
    }

    /**
     * Without this the foreground service still runs, but its persistent notification is not
     * shown. That notification is what keeps an aggressive ROM from treating the service as
     * idle background work, so the app asks for it and says why — but never blocks on it.
     */
    fun optional(): List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyList()
    }

    fun all(): List<String> = required() + optional()

    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasRequired(context: Context): Boolean = required().all { isGranted(context, it) }

    fun hasOptional(context: Context): Boolean = optional().all { isGranted(context, it) }
}

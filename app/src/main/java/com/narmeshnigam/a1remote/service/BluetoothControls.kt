package com.narmeshnigam.a1remote.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/**
 * Best-effort control over the phone's own Bluetooth radio, for the Setup screen.
 *
 * Android 13 deprecated `BluetoothAdapter.enable()` / `disable()`; on a stock ROM they return
 * `false` and do nothing, because the platform reserves the toggle for the user. This device's
 * ROM may still honour them — every call reports whether the stack accepted it, and when it does
 * not the caller opens the system Bluetooth UI instead. Nothing here claims to have toggled the
 * radio when it has not (see `docs/OPEN_QUESTIONS.md`, "phone Bluetooth toggle").
 */
object BluetoothControls {

    private const val TAG = "A1BtControls"

    fun adapter(context: Context): BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter

    fun isOn(context: Context): Boolean = adapter(context)?.isEnabled == true

    /** The system consent dialog to switch Bluetooth on — the sanctioned path on API 33+. */
    fun enableDialogIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    /**
     * The system Bluetooth settings screen — the fallback for turning off or restarting the radio
     * when the direct call is refused. (There is no `Settings.Panel` action for Bluetooth, unlike
     * Wi-Fi, so this opens the full settings page.)
     */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Tries to switch Bluetooth off directly. False means the OS refused — open [settingsIntent]. */
    @SuppressLint("MissingPermission") // BLUETOOTH_CONNECT is held; the call is deprecated, not unsafe
    @Suppress("DEPRECATION")
    fun disableDirect(context: Context): Boolean {
        val adapter = adapter(context) ?: return false
        if (!adapter.isEnabled) return true
        val accepted = runCatching { adapter.disable() }.getOrDefault(false)
        Log.i(TAG, "BluetoothAdapter.disable() returned $accepted")
        return accepted
    }

    /** Tries to switch Bluetooth on directly. False means the OS refused — show [enableDialogIntent]. */
    @SuppressLint("MissingPermission") // BLUETOOTH_CONNECT is held; the call is deprecated, not unsafe
    @Suppress("DEPRECATION")
    fun enableDirect(context: Context): Boolean {
        val adapter = adapter(context) ?: return false
        if (adapter.isEnabled) return true
        val accepted = runCatching { adapter.enable() }.getOrDefault(false)
        Log.i(TAG, "BluetoothAdapter.enable() returned $accepted")
        return accepted
    }
}

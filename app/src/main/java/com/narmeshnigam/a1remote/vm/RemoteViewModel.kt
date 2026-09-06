package com.narmeshnigam.a1remote.vm

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.narmeshnigam.a1remote.data.KeyMaps
import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.KeyStatus
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.service.BluetoothControls
import com.narmeshnigam.a1remote.service.BondedHost
import com.narmeshnigam.a1remote.service.HidLink
import com.narmeshnigam.a1remote.service.HidService
import com.narmeshnigam.a1remote.service.LinkState
import com.narmeshnigam.a1remote.service.SendResult
import com.narmeshnigam.a1remote.service.WireLogEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Connection state, press handling and the wire log for the remote screens (BUILD_SPEC §3).
 *
 * The link and the key map are process-wide because the service outlives every screen; this
 * view model is the screen-facing view of them, and the place auto-repeat and haptics attach.
 */
class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val keyMap = KeyMaps.get(application)

    val link: StateFlow<LinkState> = HidLink.state
    val wireLog: StateFlow<List<WireLogEntry>> = HidLink.wireLog
    val bindings: StateFlow<Map<RemoteFunction, KeyBinding>> = keyMap.bindings

    private val _lastResult = MutableStateFlow<SendResult?>(null)
    val lastResult: StateFlow<SendResult?> = _lastResult.asStateFlow()

    private val _bondedHosts = MutableStateFlow<List<BondedHost>>(emptyList())
    val bondedHosts: StateFlow<List<BondedHost>> = _bondedHosts.asStateFlow()

    private val app: Context get() = getApplication()

    private val _bluetoothOn = MutableStateFlow(BluetoothControls.isOn(getApplication()))

    /** Whether the phone's Bluetooth radio is on, kept live for the Setup controls. */
    val bluetoothOn: StateFlow<Boolean> = _bluetoothOn.asStateFlow()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                _bluetoothOn.value = BluetoothControls.isOn(app)
            }
        }
    }

    init {
        app.registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onCleared() {
        runCatching { app.unregisterReceiver(bluetoothReceiver) }
        super.onCleared()
    }

    /** Re-reads the bond list. Cheap, and pairing can happen behind the app's back. */
    fun refreshBondedHosts() {
        _bondedHosts.value = HidLink.bondedHosts()
    }

    /** Attempts to switch Bluetooth off. False means the OS refused — the caller opens the panel. */
    fun turnOffBluetooth(): Boolean = BluetoothControls.disableDirect(app)

    /**
     * Attempts to cycle Bluetooth off and back on — the surest way to clear a stuck HID
     * registration. False means the OS refused the off, and the caller opens the system panel.
     */
    fun restartBluetooth(): Boolean {
        if (!BluetoothControls.disableDirect(app)) return false
        viewModelScope.launch {
            val adapter = BluetoothControls.adapter(app) ?: return@launch
            var waited = 0L
            while (adapter.state != BluetoothAdapter.STATE_OFF && waited < BT_RESTART_TIMEOUT_MS) {
                delay(BT_POLL_MS)
                waited += BT_POLL_MS
            }
            BluetoothControls.enableDirect(app)
        }
        return true
    }

    /** Opens the HID connection to a host the phone is already bonded with. */
    fun connectHost(address: String): Boolean = HidLink.connectHost(address)

    /** Transmit one function. Does nothing at all when the link is down (BUILD_SPEC §5). */
    fun press(function: RemoteFunction) {
        _lastResult.value = HidLink.sendKey(function)
    }

    /** True when [function] has been proved on the A1 and may be drawn verified. */
    fun isVerified(function: RemoteFunction): Boolean = bindings.value[function]?.status == KeyStatus.CONFIRMED

    fun connect() = HidService.start(getApplication())

    fun disconnect() = HidService.stop(getApplication())

    fun clearWireLog() = HidLink.clearWireLog()

    private companion object {
        /** How long to wait for the radio to reach OFF before re-enabling it on a restart. */
        const val BT_RESTART_TIMEOUT_MS = 6_000L
        const val BT_POLL_MS = 200L
    }
}

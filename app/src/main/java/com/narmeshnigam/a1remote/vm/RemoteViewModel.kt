package com.narmeshnigam.a1remote.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.narmeshnigam.a1remote.data.KeyMaps
import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.KeyStatus
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.service.BondedHost
import com.narmeshnigam.a1remote.service.HidLink
import com.narmeshnigam.a1remote.service.HidService
import com.narmeshnigam.a1remote.service.LinkState
import com.narmeshnigam.a1remote.service.SendResult
import com.narmeshnigam.a1remote.service.WireLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    /** Re-reads the bond list. Cheap, and pairing can happen behind the app's back. */
    fun refreshBondedHosts() {
        _bondedHosts.value = HidLink.bondedHosts()
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
}

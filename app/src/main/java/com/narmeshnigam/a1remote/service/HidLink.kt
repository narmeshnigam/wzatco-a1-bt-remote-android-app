package com.narmeshnigam.a1remote.service

import com.narmeshnigam.a1remote.hid.HidReport
import com.narmeshnigam.a1remote.hid.RemoteFunction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What [HidLink] needs from whatever is actually holding the Bluetooth proxy. */
interface HidTransport {
    /** Sends [function]'s current mapping as a key-down/key-up pair. */
    fun sendKey(function: RemoteFunction): SendResult

    /**
     * Sends one arbitrary report as a key-down/key-up pair, bypassing the key map.
     *
     * Key Lab needs this to try a candidate that is not mapped to anything yet.
     */
    fun sendPress(report: HidReport, label: String): SendResult

    /**
     * Sends a single report with no matching release.
     *
     * Only relative mouse motion may use this: a move report describes a delta that is over
     * the moment it is delivered, so there is nothing to release. Anything that can be *held*
     * goes through [sendPress] and gets its guaranteed key-up.
     */
    fun sendMotion(report: HidReport, label: String): SendResult
}

/**
 * Process-wide view of the link.
 *
 * The service owns the Bluetooth proxy and can be killed and restarted independently of any
 * screen, so link state lives here rather than in a view model: a rotation or a back-and-forward
 * must not disturb a registration. State dies with the process, which is what we want — a
 * registration must never be assumed to have survived one.
 */
object HidLink {
    private const val WIRE_LOG_LIMIT = 50

    private val _state = MutableStateFlow(LinkState())
    val state: StateFlow<LinkState> = _state.asStateFlow()

    private val _wireLog = MutableStateFlow<List<WireLogEntry>>(emptyList())
    val wireLog: StateFlow<List<WireLogEntry>> = _wireLog.asStateFlow()

    @Volatile
    internal var transport: HidTransport? = null

    /** Transmit one function as a key-down/key-up pair. */
    fun sendKey(function: RemoteFunction): SendResult = transport?.sendKey(function) ?: SendResult.NO_SERVICE

    internal fun update(block: (LinkState) -> LinkState) = _state.update(block)

    internal fun reset() {
        _state.value = LinkState()
    }

    internal fun record(entry: WireLogEntry) {
        _wireLog.update { log -> (log + entry).takeLast(WIRE_LOG_LIMIT) }
    }

    fun clearWireLog() {
        _wireLog.value = emptyList()
    }
}

package com.narmeshnigam.a1remote.service

import com.narmeshnigam.a1remote.hid.DefaultKeyMap
import com.narmeshnigam.a1remote.hid.HidReport
import com.narmeshnigam.a1remote.hid.HidReports
import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.KeyPressSender
import com.narmeshnigam.a1remote.hid.RemoteFunction

/**
 * The fake host of TEST_PLAN.md: a [HidTransport] that captures reports instead of transmitting
 * them, so everything above the Bluetooth stack is testable with no projector in the room.
 *
 * @param connected when false the fake refuses exactly as the real service does on a dead link
 * @param bindings the key map the fake resolves against — the shipped table unless a test needs
 *   a binding the defaults no longer contain, such as one carrying no report at all
 */
class FakeHidTransport(
    var connected: Boolean = true,
    private val bindings: Map<RemoteFunction, KeyBinding> = DefaultKeyMap.all(),
) : HidTransport {

    /** Every report the app tried to put on the wire, in order. */
    val sent = mutableListOf<HidReport>()

    /** The wire-log labels the app attached to them. */
    val labels = mutableListOf<String>()

    /**
     * Refuse every press from this one onward, counting from zero.
     *
     * A run of typing has to stop at the first report the stack refuses rather than carrying on
     * into a field that is already wrong, and that is only testable if a fake can refuse part
     * of the way through.
     */
    var refusePressFrom: Int? = null

    private var presses = 0

    override fun sendKey(function: RemoteFunction): SendResult {
        val binding = bindings.getValue(function)
        val report = binding.report ?: return SendResult.UNMAPPED
        return sendPress(report, "${function.name} ${binding.usageName}")
    }

    override fun sendPress(report: HidReport, label: String): SendResult {
        if (!connected) return SendResult.NOT_CONNECTED
        val refused = refusePressFrom?.let { presses >= it } == true
        presses++
        if (refused) return SendResult.FAILED
        labels += label
        KeyPressSender { out ->
            sent += out
            true
        }.press(report)
        return SendResult.SENT
    }

    override fun sendMotion(report: HidReport, label: String): SendResult {
        if (!connected) return SendResult.NOT_CONNECTED
        labels += label
        sent += report
        return SendResult.SENT
    }

    var bonded: List<BondedHost> = emptyList()
    val connectRequests = mutableListOf<String>()

    override fun bondedHosts(): List<BondedHost> = bonded

    override fun connectHost(address: String): Boolean {
        connectRequests += address
        return connected
    }

    /** True when every report sent so far is followed by its matching release. */
    fun everyPressWasReleased(): Boolean = sent.filter { it.id != HidReports.mouseRelease().id || !it.isRelease }
        .chunked(2)
        .all { pair -> pair.size == 2 && pair[1].isRelease && pair[0].id == pair[1].id }
}

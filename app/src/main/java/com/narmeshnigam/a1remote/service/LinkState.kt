package com.narmeshnigam.a1remote.service

/**
 * Where the HID link has got to. The stages before [REGISTERED] exist so a refusal by the ROM
 * is visible as itself rather than as a generic failure.
 */
enum class LinkStage {
    /** The service is not running. */
    STOPPED,

    /** A required runtime permission is missing. */
    PERMISSION_DENIED,

    /** No `BluetoothManager` or no adapter — this phone cannot do Bluetooth at all. */
    NO_BLUETOOTH,

    /** Adapter present but switched off. */
    BLUETOOTH_OFF,

    /** `getProfileProxy(HID_DEVICE)` accepted, waiting for the proxy callback. */
    ACQUIRING_PROXY,

    /** `getProfileProxy(HID_DEVICE)` returned false, or the callback never arrived. */
    PROXY_REFUSED,

    /** Proxy in hand, `registerApp()` called, waiting for `onAppStatusChanged`. */
    REGISTERING,

    /** `registerApp()` returned false, or reported `registered = false`. This is the Gate 1 failure. */
    REGISTRATION_REFUSED,

    /** Registered as a HID device. No host has connected yet. */
    REGISTERED,

    /** A host is connected and reports will reach it. */
    CONNECTED,
}

/**
 * The full picture of the link, including each individual platform call's result.
 *
 * Every nullable flag is "not attempted yet" when null. They are surfaced verbatim in the
 * debug screen so a ROM-level refusal cannot be mistaken for an application bug.
 */
data class LinkState(
    val stage: LinkStage = LinkStage.STOPPED,
    val bluetoothManagerAcquired: Boolean? = null,
    val adapterAcquired: Boolean? = null,
    val adapterEnabled: Boolean? = null,
    val proxyRequestAccepted: Boolean? = null,
    val proxyConnected: Boolean? = null,
    val registerAppReturned: Boolean? = null,
    val appStatusRegistered: Boolean? = null,
    val hostName: String? = null,
    val hostAddress: String? = null,
    val message: String? = null,
) {
    val isConnected: Boolean get() = stage == LinkStage.CONNECTED

    val isRegistered: Boolean get() = stage == LinkStage.REGISTERED || stage == LinkStage.CONNECTED
}

/** The outcome of one attempt to transmit a function. */
enum class SendResult {
    /** Key-down and key-up both accepted by the stack. */
    SENT,

    /** The service is not running, so there is nothing to send through. */
    NO_SERVICE,

    /** Registered but no host is connected. Reports are never queued for a dead link. */
    NOT_CONNECTED,

    /** The function has no mapping. Nothing was transmitted, deliberately. */
    UNMAPPED,

    /** A Bluetooth runtime permission is missing. */
    PERMISSION_DENIED,

    /** The stack accepted the call but refused the report. */
    FAILED,
}

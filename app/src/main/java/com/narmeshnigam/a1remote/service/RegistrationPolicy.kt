package com.narmeshnigam.a1remote.service

/**
 * Pure decisions for `BluetoothHidDevice` registration, kept out of [HidService] so they can be
 * unit-tested.
 *
 * Observed on OxygenOS `DN2101_11_F.59` (2026-09-06):
 * - Calling `registerApp()` while this app is already registered returns `false`
 *   (`HidDeviceService: registerApp(): failed because another app is registered`) and no
 *   `onAppStatusChanged` follows.
 * - Deregistration is asynchronous in the native stack. A process that restarts within a second of
 *   its predecessor gets `register_app: application already registered` and
 *   `unregister_app: BT-HD deregistering in progress` — both return `false` — until the stack
 *   finishes. A `false` return therefore does not by itself mean the ROM refused the profile.
 * - A registration owned by a *dead* process is not always released. After an uninstall and
 *   reinstall (new uid) the stack logged `unregisterAppUid(): caller UID doesn't match user UID`
 *   and refused every `registerApp()` from the new uid; our own `unregisterApp()` cannot clear a
 *   registration held for another uid. Only cycling Bluetooth cleared it — hence [onAdapterState]:
 *   the radio coming back is the cue to register again without a tap.
 */
object RegistrationPolicy {
    /** Attempts made before a `false` return is treated as a refusal. */
    const val MAX_ATTEMPTS = 10

    /** Delay before each retry; long enough for the native deregistration to finish. */
    const val RETRY_DELAY_MS = 1_000L

    enum class Next { ACQUIRE_PROXY, REGISTER, ALREADY_REGISTERED }

    enum class AfterReturn { WAIT_FOR_CALLBACK, KEEP_REGISTERED, RETRY_LATER, REFUSED }

    enum class OnAdapter { TEAR_DOWN, REGISTER, NONE }

    fun next(proxyHeld: Boolean, appRegistered: Boolean): Next = when {
        !proxyHeld -> Next.ACQUIRE_PROXY
        appRegistered -> Next.ALREADY_REGISTERED
        else -> Next.REGISTER
    }

    /** [attempt] is 1-based: the call that just returned [returned]. */
    fun afterReturn(returned: Boolean, appRegistered: Boolean, attempt: Int): AfterReturn = when {
        returned -> AfterReturn.WAIT_FOR_CALLBACK
        appRegistered -> AfterReturn.KEEP_REGISTERED
        attempt < MAX_ATTEMPTS -> AfterReturn.RETRY_LATER
        else -> AfterReturn.REFUSED
    }

    /**
     * What the phone's adapter switching [on] or off means for the link. Off takes the proxy and
     * the registration with it, so the service tears down honestly; on is the cue to register
     * again — unless the app is somehow still registered, in which case nothing is touched.
     */
    fun onAdapterState(on: Boolean, appRegistered: Boolean): OnAdapter = when {
        !on -> OnAdapter.TEAR_DOWN
        appRegistered -> OnAdapter.NONE
        else -> OnAdapter.REGISTER
    }
}

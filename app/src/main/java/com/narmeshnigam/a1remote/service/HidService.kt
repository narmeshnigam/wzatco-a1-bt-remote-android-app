package com.narmeshnigam.a1remote.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.narmeshnigam.a1remote.data.KeyMaps
import com.narmeshnigam.a1remote.data.LastHostStore
import com.narmeshnigam.a1remote.hid.HidDescriptor
import com.narmeshnigam.a1remote.hid.HidReport
import com.narmeshnigam.a1remote.hid.HidReports
import com.narmeshnigam.a1remote.hid.KeyPressSender
import com.narmeshnigam.a1remote.hid.RemoteFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Foreground service that owns the `BluetoothHidDevice` proxy, the SDP registration, the single
 * host connection and report transmission (BUILD_SPEC §3).
 *
 * Every platform call that a ROM can refuse — acquiring the `BluetoothManager`, taking the
 * HID_DEVICE proxy, `registerApp()` and the `onAppStatusChanged` callback — is logged with its
 * exact result under the tag [TAG] and mirrored into [LinkState]. On a build that blocks the HID
 * Device profile the refusal is then unmistakable in logcat and on screen, rather than looking
 * like an application bug.
 */
// A HID device service legitimately carries many small methods — the profile lifecycle, the
// registration state machine and the transmission API each need their own.
@Suppress("TooManyFunctions")
class HidService :
    Service(),
    HidTransport {

    private val reportExecutor = Executors.newSingleThreadExecutor()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val notification by lazy { LinkNotification(this) }
    private val lastHostStore by lazy { LastHostStore(this) }

    private var adapter: BluetoothAdapter? = null
    private var proxy: BluetoothHidDevice? = null
    private var appRegistered = false
    private var registerRetryJob: Job? = null
    private var reconnectJob: Job? = null
    private var connectTimeoutJob: Job? = null

    /** The address of the last host that actually connected, read back from [LastHostStore]. */
    @Volatile
    private var savedHostAddress: String? = null

    /** The one-shot on-start reach for the saved host — only ever tried once per service life. */
    private var autoConnectAttempted = false

    @Volatile
    private var connecting = false

    @Volatile
    private var host: BluetoothDevice? = null

    /** The host to retry after an unexpected drop. Cleared on a deliberate unplug. */
    @Volatile
    private var lastHost: BluetoothDevice? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        notification.createChannel()
        HidLink.transport = this
        // Read the remembered host before registration lands, so the on-start auto-connect has
        // something to reach for the moment the app is registered.
        serviceScope.launch {
            savedHostAddress = runCatching { lastHostStore.get() }.getOrNull()
            maybeAutoConnect()
        }
    }

    // FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE is an API 29 constant inlined at compile time, and
    // ServiceCompat.startForeground ignores the type argument below API 29. Safe on this minSdk 28.
    @SuppressLint("InlinedApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand action=${intent?.action} flags=$flags startId=$startId")
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this,
            LinkNotification.NOTIFICATION_ID,
            notification.build(HidLink.state.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )
        register()
        // OxygenOS stops background work eagerly; ask the platform to bring the service back.
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        HidLink.transport = null
        reconnectJob?.cancel()
        serviceScope.cancel()
        unregister()
        reportExecutor.shutdown()
        HidLink.reset()
        super.onDestroy()
    }

    // region registration

    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    private fun register() {
        if (!hasBluetoothPermission()) {
            fail(LinkStage.PERMISSION_DENIED, "BLUETOOTH_CONNECT not granted; not attempting registration")
            return
        }

        val manager = getSystemService(BluetoothManager::class.java)
        Log.i(TAG, "getSystemService(BluetoothManager) returned $manager")
        note("getSystemService", "BluetoothManager = ${manager ?: "null"}")
        HidLink.update { it.copy(bluetoothManagerAcquired = manager != null) }
        if (manager == null) {
            fail(LinkStage.NO_BLUETOOTH, "getSystemService(BluetoothManager) returned null")
            return
        }

        val bluetoothAdapter = manager.adapter
        Log.i(TAG, "BluetoothManager.getAdapter() returned $bluetoothAdapter")
        note("getAdapter", bluetoothAdapter?.toString() ?: "null")
        HidLink.update { it.copy(adapterAcquired = bluetoothAdapter != null) }
        if (bluetoothAdapter == null) {
            fail(LinkStage.NO_BLUETOOTH, "BluetoothManager.getAdapter() returned null")
            return
        }
        adapter = bluetoothAdapter

        val enabled = bluetoothAdapter.isEnabled
        Log.i(TAG, "BluetoothAdapter.isEnabled = $enabled")
        HidLink.update { it.copy(adapterEnabled = enabled) }
        if (!enabled) {
            fail(LinkStage.BLUETOOTH_OFF, "Bluetooth is switched off")
            return
        }

        val held = proxy
        when (RegistrationPolicy.next(proxyHeld = held != null, appRegistered = appRegistered)) {
            RegistrationPolicy.Next.ACQUIRE_PROXY -> acquireProxy(bluetoothAdapter)
            RegistrationPolicy.Next.REGISTER -> registerApp(checkNotNull(held))
            RegistrationPolicy.Next.ALREADY_REGISTERED -> {
                Log.i(TAG, "HID_DEVICE proxy held and app already registered; not calling registerApp() again")
                note("register", "already registered")
                HidLink.update {
                    it.copy(stage = if (host != null) LinkStage.CONNECTED else LinkStage.REGISTERED, message = null)
                }
                updateNotification()
                maybeAutoConnect()
            }
        }
    }

    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    private fun acquireProxy(bluetoothAdapter: BluetoothAdapter) {
        val accepted = bluetoothAdapter.getProfileProxy(this, profileListener, BluetoothProfile.HID_DEVICE)
        Log.i(TAG, "BluetoothAdapter.getProfileProxy(HID_DEVICE) returned $accepted")
        note("getProfileProxy", "HID_DEVICE returned $accepted")
        HidLink.update {
            it.copy(
                stage = if (accepted) LinkStage.ACQUIRING_PROXY else LinkStage.PROXY_REFUSED,
                proxyRequestAccepted = accepted,
                message = if (accepted) {
                    null
                } else {
                    "getProfileProxy(HID_DEVICE) returned false — this ROM refused the HID Device profile"
                },
            )
        }
        updateNotification()
    }

    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    private fun registerApp(hid: BluetoothHidDevice, attempt: Int = 1) {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            SDP_NAME,
            SDP_DESCRIPTION,
            SDP_PROVIDER,
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidDescriptor.bytes(),
        )
        HidLink.update { it.copy(stage = LinkStage.REGISTERING) }
        updateNotification()

        val returned = try {
            hid.registerApp(sdp, null, null, reportExecutor, hidCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "BluetoothHidDevice.registerApp() threw SecurityException", e)
            note("registerApp", "SecurityException: ${e.message}")
            HidLink.update {
                it.copy(
                    stage = LinkStage.REGISTRATION_REFUSED,
                    registerAppReturned = false,
                    message = "registerApp() threw SecurityException: ${e.message}",
                )
            }
            updateNotification()
            return
        }

        Log.i(TAG, "BluetoothHidDevice.registerApp() returned $returned")
        note("registerApp", "returned $returned")
        HidLink.update { it.copy(registerAppReturned = returned) }
        handleRegisterResult(hid, returned, attempt)
    }

    private fun handleRegisterResult(hid: BluetoothHidDevice, returned: Boolean, attempt: Int) {
        when (RegistrationPolicy.afterReturn(returned, appRegistered, attempt)) {
            RegistrationPolicy.AfterReturn.WAIT_FOR_CALLBACK -> HidLink.update { it.copy(message = null) }
            RegistrationPolicy.AfterReturn.KEEP_REGISTERED -> {
                Log.i(TAG, "registerApp() returned false but the app is registered; keeping the registration")
                HidLink.update {
                    it.copy(stage = if (host != null) LinkStage.CONNECTED else LinkStage.REGISTERED, message = null)
                }
            }
            RegistrationPolicy.AfterReturn.RETRY_LATER -> {
                // A prior instance of this app that was killed can leave its registration held in
                // HidDeviceService (mUserUid still set), so registerApp() returns false with
                // "application already registered". Clearing it needs an explicit unregisterApp()
                // for our own uid, not just a wait — so deregister, then re-register.
                Log.w(
                    TAG,
                    "registerApp() false on attempt $attempt; waiting for callback, else clearing in " +
                        "${RegistrationPolicy.RETRY_DELAY_MS} ms",
                )
                HidLink.update { it.copy(message = "Waiting for the registration callback (attempt $attempt)") }
                registerRetryJob?.cancel()
                registerRetryJob = serviceScope.launch {
                    // This ROM returns false from registerApp() even when the registration then
                    // succeeds via onAppStatusChanged (open question 14). The callback is the
                    // authoritative signal, so wait for it before assuming a stale registration is
                    // blocking us — tearing down here would kill a registration that just landed.
                    delay(RegistrationPolicy.RETRY_DELAY_MS)
                    if (proxy !== hid || appRegistered) return@launch
                    val cleared = runCatching { hid.unregisterApp() }.getOrDefault(false)
                    Log.i(TAG, "BluetoothHidDevice.unregisterApp() returned $cleared while clearing stale state")
                    delay(RegistrationPolicy.RETRY_DELAY_MS)
                    if (proxy === hid && !appRegistered) registerApp(hid, attempt + 1)
                }
            }
            RegistrationPolicy.AfterReturn.REFUSED -> HidLink.update {
                it.copy(
                    stage = LinkStage.REGISTRATION_REFUSED,
                    message = "registerApp() returned false ${RegistrationPolicy.MAX_ATTEMPTS} times — " +
                        "the stack refused the HID Device profile",
                )
            }
        }
        updateNotification()
    }

    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    private fun unregister() {
        val hid = proxy ?: return
        proxy = null
        host = null
        appRegistered = false
        registerRetryJob?.cancel()
        runCatching { hid.unregisterApp() }
            .onSuccess { Log.i(TAG, "BluetoothHidDevice.unregisterApp() returned $it") }
            .onFailure { Log.e(TAG, "unregisterApp() failed", it) }
        runCatching { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid) }
            .onFailure { Log.e(TAG, "closeProfileProxy() failed", it) }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, service: BluetoothProfile) {
            Log.i(TAG, "ServiceListener.onServiceConnected(profile=$profile, service=$service)")
            if (profile != BluetoothProfile.HID_DEVICE) return
            val hid = service as? BluetoothHidDevice
            if (hid == null) {
                fail(LinkStage.PROXY_REFUSED, "HID_DEVICE proxy was not a BluetoothHidDevice")
                return
            }
            proxy = hid
            note("proxy", "HID_DEVICE proxy connected")
            HidLink.update { it.copy(proxyConnected = true) }
            registerApp(hid)
        }

        override fun onServiceDisconnected(profile: Int) {
            Log.w(TAG, "ServiceListener.onServiceDisconnected(profile=$profile)")
            if (profile != BluetoothProfile.HID_DEVICE) return
            proxy = null
            host = null
            note("proxy", "HID_DEVICE proxy disconnected")
            HidLink.update {
                it.copy(
                    stage = LinkStage.PROXY_REFUSED,
                    proxyConnected = false,
                    message = "HID_DEVICE proxy disconnected",
                )
            }
            updateNotification()
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.i(TAG, "Callback.onAppStatusChanged(pluggedDevice=$pluggedDevice, registered=$registered)")
            note("onAppStatusChanged", "registered=$registered")
            appRegistered = registered
            if (registered) registerRetryJob?.cancel()
            HidLink.update {
                it.copy(
                    stage = when {
                        !registered -> LinkStage.REGISTRATION_REFUSED
                        it.stage == LinkStage.CONNECTED -> LinkStage.CONNECTED
                        else -> LinkStage.REGISTERED
                    },
                    appStatusRegistered = registered,
                    message = if (registered) null else "onAppStatusChanged reported registered=false",
                )
            }
            updateNotification()
            if (registered) maybeAutoConnect()
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.i(TAG, "Callback.onConnectionStateChanged(device=$device, state=${stateName(state)})")
            note("connection", "${stateName(state)} ${device.address}")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    host = device
                    lastHost = device
                    connecting = false
                    connectTimeoutJob?.cancel()
                    reconnectJob?.cancel()
                    // Remember it for the next app start.
                    savedHostAddress = device.address
                    serviceScope.launch { runCatching { lastHostStore.set(device.address) } }
                    HidLink.update {
                        it.copy(
                            stage = LinkStage.CONNECTED,
                            hostName = safeName(device),
                            hostAddress = device.address,
                        )
                    }
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    connecting = true
                    HidLink.update {
                        it.copy(
                            stage = LinkStage.CONNECTING,
                            hostName = safeName(device),
                            hostAddress = device.address,
                        )
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasConnected = host != null
                    host = null
                    connecting = false
                    connectTimeoutJob?.cancel()
                    HidLink.update {
                        it.copy(
                            stage = if (it.appStatusRegistered == true) LinkStage.REGISTERED else it.stage,
                            hostName = null,
                            hostAddress = null,
                        )
                    }
                    // Only an established link that dropped is worth chasing. A connect attempt
                    // that never got through (page timeout) will not succeed by being repeated
                    // a second later on top of itself — the stack reports that as
                    // HID_ERR_CONN_IN_PROCESS and nothing moves.
                    if (wasConnected) scheduleReconnect()
                }

                else -> Unit
            }
            updateNotification()
        }

        @SuppressLint("MissingPermission") // only reachable once a registration has succeeded
        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            Log.i(TAG, "Callback.onGetReport(type=$type, id=$id, bufferSize=$bufferSize)")
            val payload = releasePayloadFor(id.toInt())
            if (payload == null) {
                proxy?.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
            } else {
                proxy?.replyReport(device, type, id, payload)
            }
        }

        @SuppressLint("MissingPermission") // only reachable once a registration has succeeded
        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            Log.i(TAG, "Callback.onSetReport(type=$type, id=$id, ${data.size} bytes)")
            proxy?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            Log.i(TAG, "Callback.onSetProtocol(protocol=$protocol)")
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            Log.i(TAG, "Callback.onInterruptData(reportId=$reportId, ${data.size} bytes)")
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            Log.w(TAG, "Callback.onVirtualCableUnplug(device=$device)")
            host = null
            connecting = false
            connectTimeoutJob?.cancel()
            // An unplug is the host saying it is done with us. Retrying would be rude and futile.
            lastHost = null
            reconnectJob?.cancel()
            note("connection", "virtual cable unplugged")
            HidLink.update { it.copy(stage = LinkStage.REGISTERED, hostName = null, hostAddress = null) }
            updateNotification()
        }
    }

    /**
     * BUILD_SPEC §7: retry the last known host three times with backoff, then stop and wait for
     * a manual connect.
     *
     * It stops after three because a projector that has been switched off will not answer, and a
     * phone that retries for ever is a phone with a flat battery.
     */
    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    private fun scheduleReconnect() {
        val target = lastHost ?: return
        if (reconnectJob?.isActive == true) return
        reconnectJob = serviceScope.launch {
            repeat(RECONNECT_ATTEMPTS) { attempt ->
                delay(RECONNECT_BACKOFF_MS shl attempt)
                if (host != null) return@launch
                if (connecting) return@repeat
                val requested = requestConnect(target)
                Log.i(TAG, "reconnect attempt ${attempt + 1}/$RECONNECT_ATTEMPTS -> $requested")
                note("reconnect", "attempt ${attempt + 1}/$RECONNECT_ATTEMPTS -> $requested")
            }
            if (host == null) {
                note("reconnect", "gave up after $RECONNECT_ATTEMPTS attempts; connect manually")
            }
        }
    }

    /**
     * Reaches for the remembered host once, the moment the app is registered on a fresh start.
     *
     * It runs at most once per service life ([autoConnectAttempted]) and only for a device the
     * phone is still bonded with, so it never fights a deliberate disconnect and never pages a
     * device the user has since unpaired.
     */
    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    private fun maybeAutoConnect() {
        if (autoConnectAttempted || host != null) return
        if (connecting || !appRegistered) return
        if (!hasBluetoothPermission()) return
        val address = savedHostAddress ?: return
        val bonded = adapter?.bondedDevices?.any { it.address == address } == true
        if (!bonded) return
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return
        autoConnectAttempted = true
        lastHost = device
        Log.i(TAG, "auto-connecting to remembered host $address")
        note("connect", "auto-connect to remembered host $address")
        requestConnect(device)
    }

    // endregion

    // region transmission

    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    override fun sendKey(function: RemoteFunction): SendResult {
        val binding = KeyMaps.get(this)[function]
        val down = binding.report
        if (down == null) {
            note(function.name, "no mapping — nothing sent")
            return SendResult.UNMAPPED
        }
        return sendPress(down, "${function.name} ${binding.usageName}")
    }

    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    override fun sendPress(report: HidReport, label: String): SendResult {
        if (!hasBluetoothPermission()) return SendResult.PERMISSION_DENIED
        val hid = proxy ?: return SendResult.NO_SERVICE
        // Never queue a report for a dead link (BUILD_SPEC §5).
        val device = host ?: return SendResult.NOT_CONNECTED

        // The key-up guarantee of BUILD_SPEC §4 lives in KeyPressSender, where it is unit-tested.
        val outcome = KeyPressSender { out -> hid.sendReport(device, out.id, out.data) }.press(report)
        note(label, "down id=${report.id} [${report.hex()}] -> ${outcome.down}")
        note(label, "up   [${HidReports.releaseFor(report).hex()}] -> ${outcome.up}")
        return if (outcome.down) SendResult.SENT else SendResult.FAILED
    }

    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    override fun sendMotion(report: HidReport, label: String): SendResult {
        if (!hasBluetoothPermission()) return SendResult.PERMISSION_DENIED
        val hid = proxy ?: return SendResult.NO_SERVICE
        val device = host ?: return SendResult.NOT_CONNECTED
        val accepted = runCatching { hid.sendReport(device, report.id, report.data) }.getOrDefault(false)
        if (!accepted) note(label, "motion id=${report.id} [${report.hex()}] -> false")
        return if (accepted) SendResult.SENT else SendResult.FAILED
    }

    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    override fun bondedHosts(): List<BondedHost> {
        if (!hasBluetoothPermission()) return emptyList()
        val bonded = adapter?.bondedDevices ?: return emptyList()
        return bonded.map { device -> BondedHost(safeName(device) ?: device.address, device.address) }
            .sortedBy { it.name }
    }

    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    override fun connectHost(address: String): Boolean {
        if (!hasBluetoothPermission()) return false
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return false
        if (host?.address == address) {
            Log.i(TAG, "connectHost($address) ignored; already connected")
            return true
        }
        if (connecting) {
            // Firing connect() again while one is in flight makes the stack return
            // HID_ERR_CONN_IN_PROCESS and wedges the L2CAP config handshake. One at a time.
            Log.i(TAG, "connectHost($address) ignored; a connection is already in progress")
            note("connect", "$address ignored; already connecting")
            return false
        }
        lastHost = device
        reconnectJob?.cancel()
        return requestConnect(device)
    }

    /** The single guarded entry point for [BluetoothHidDevice.connect]. */
    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission() at every caller
    private fun requestConnect(device: BluetoothDevice): Boolean {
        val hid = proxy ?: return false
        if (connecting || host?.address == device.address) return host?.address == device.address
        connecting = true
        HidLink.update {
            it.copy(stage = LinkStage.CONNECTING, hostName = safeName(device), hostAddress = device.address)
        }
        updateNotification()
        val requested = runCatching { hid.connect(device) }.getOrDefault(false)
        Log.i(TAG, "BluetoothHidDevice.connect(${device.address}) returned $requested")
        note("connect", "${device.address} -> $requested")
        if (requested) {
            startConnectTimeout(device)
        } else {
            connecting = false
            HidLink.update {
                it.copy(
                    stage = if (appRegistered) LinkStage.REGISTERED else it.stage,
                    hostName = null,
                    hostAddress = null,
                )
            }
            updateNotification()
        }
        return requested
    }

    private fun startConnectTimeout(device: BluetoothDevice) {
        connectTimeoutJob?.cancel()
        connectTimeoutJob = serviceScope.launch {
            delay(CONNECT_TIMEOUT_MS)
            if (!connecting || host != null) return@launch
            Log.w(TAG, "connect to ${device.address} timed out after $CONNECT_TIMEOUT_MS ms; giving up")
            note("connect", "${device.address} timed out")
            connecting = false
            HidLink.update {
                it.copy(
                    stage = if (appRegistered) LinkStage.REGISTERED else it.stage,
                    hostName = null,
                    hostAddress = null,
                )
            }
            updateNotification()
        }
    }

    // endregion

    // region plumbing

    private fun fail(stage: LinkStage, message: String) {
        Log.e(TAG, message)
        note("link", message)
        HidLink.update { it.copy(stage = stage, message = message) }
        updateNotification()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            ?.notify(LinkNotification.NOTIFICATION_ID, notification.build(HidLink.state.value))
    }

    // endregion

    companion object {
        const val TAG = "A1HidService"

        private const val ACTION_STOP = "com.narmeshnigam.a1remote.action.STOP"

        /** BUILD_SPEC §7: three retries, then wait for the user. */
        private const val RECONNECT_ATTEMPTS = 3
        private const val RECONNECT_BACKOFF_MS = 1_000L
        private const val CONNECT_TIMEOUT_MS = 12_000L

        // BUILD_SPEC §4.
        private const val SDP_NAME = "WZATCO A1 Remote"
        private const val SDP_DESCRIPTION = "Projector remote"
        private const val SDP_PROVIDER = "narmeshnigam"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, HidService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, HidService::class.java).setAction(ACTION_STOP))
        }
    }
}

private fun releasePayloadFor(reportId: Int): ByteArray? = when (reportId) {
    HidDescriptor.REPORT_ID_KEYBOARD -> HidReports.keyboardRelease().data
    HidDescriptor.REPORT_ID_CONSUMER -> HidReports.consumerRelease().data
    HidDescriptor.REPORT_ID_MOUSE -> HidReports.mouseRelease().data
    else -> null
}

private fun stateName(state: Int): String = when (state) {
    BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
    BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
    BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
    BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
    else -> "UNKNOWN($state)"
}

private fun note(label: String, detail: String) {
    HidLink.record(WireLogEntry(System.currentTimeMillis(), label, detail))
}

@SuppressLint("MissingPermission") // reading a bonded device's name needs nothing beyond what we hold
private fun safeName(device: BluetoothDevice): String? =
    runCatching { device.name }.getOrNull() ?: device.address

private fun Context.hasBluetoothPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED
} else {
    // BLUETOOTH and BLUETOOTH_ADMIN are install-time permissions on API 28..30.
    true
}

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
import com.narmeshnigam.a1remote.hid.HidDescriptor
import com.narmeshnigam.a1remote.hid.HidReports
import com.narmeshnigam.a1remote.hid.RemoteFunction
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
class HidService :
    Service(),
    HidTransport {

    private val reportExecutor = Executors.newSingleThreadExecutor()
    private val notification by lazy { LinkNotification(this) }

    private var adapter: BluetoothAdapter? = null
    private var proxy: BluetoothHidDevice? = null

    @Volatile
    private var host: BluetoothDevice? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        notification.createChannel()
        HidLink.transport = this
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
        if (held != null) {
            Log.i(TAG, "HID_DEVICE proxy already held; re-registering the app")
            registerApp(held)
            return
        }

        acquireProxy(bluetoothAdapter)
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
    private fun registerApp(hid: BluetoothHidDevice) {
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
        HidLink.update {
            it.copy(
                stage = if (returned) LinkStage.REGISTERING else LinkStage.REGISTRATION_REFUSED,
                registerAppReturned = returned,
                message = if (returned) {
                    null
                } else {
                    "registerApp() returned false — this ROM will not host the HID Device profile"
                },
            )
        }
        updateNotification()
    }

    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission()
    private fun unregister() {
        val hid = proxy ?: return
        proxy = null
        host = null
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
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.i(TAG, "Callback.onConnectionStateChanged(device=$device, state=${stateName(state)})")
            note("connection", "${stateName(state)} ${device.address}")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    host = device
                    HidLink.update {
                        it.copy(
                            stage = LinkStage.CONNECTED,
                            hostName = safeName(device),
                            hostAddress = device.address,
                        )
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    host = null
                    HidLink.update {
                        it.copy(
                            stage = if (it.appStatusRegistered == true) LinkStage.REGISTERED else it.stage,
                            hostName = null,
                            hostAddress = null,
                        )
                    }
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
            note("connection", "virtual cable unplugged")
            HidLink.update { it.copy(stage = LinkStage.REGISTERED, hostName = null, hostAddress = null) }
            updateNotification()
        }
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
        if (!hasBluetoothPermission()) return SendResult.PERMISSION_DENIED
        val hid = proxy ?: return SendResult.NO_SERVICE
        // Never queue a report for a dead link (BUILD_SPEC §5).
        val device = host ?: return SendResult.NOT_CONNECTED

        val up = HidReports.releaseFor(down)
        return try {
            val accepted = hid.sendReport(device, down.id, down.data)
            note(function.name, "${binding.usageName} id=${down.id} [${down.hex()}] -> $accepted")
            if (accepted) SendResult.SENT else SendResult.FAILED
        } finally {
            // The key-up is guaranteed (BUILD_SPEC §4). A key stuck down on the projector cannot
            // be recovered from the phone, so it goes out even if the key-down threw.
            val accepted = runCatching { hid.sendReport(device, up.id, up.data) }.getOrDefault(false)
            note(function.name, "up   id=${up.id} [${up.hex()}] -> $accepted")
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

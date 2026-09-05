package com.narmeshnigam.a1remote.ui

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.ui.theme.A1Colors
import com.narmeshnigam.a1remote.ui.theme.A1Dimens
import com.narmeshnigam.a1remote.vm.RemoteViewModel

/** How long the phone stays discoverable when Setup asks for it. */
private const val DISCOVERABLE_SECONDS = 300

/**
 * The app shell: the permission rationale until the required permissions are held, then the
 * four screens of BUILD_SPEC §6 behind the tab bar of DESIGN_SPEC.
 */
@Composable
fun A1App(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    var hasRequired by remember { mutableStateOf(Permissions.hasRequired(context)) }
    var hasOptional by remember { mutableStateOf(Permissions.hasOptional(context)) }
    var asked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        asked = true
        hasRequired = Permissions.hasRequired(context)
        hasOptional = Permissions.hasOptional(context)
    }

    // Coming back from app settings can have changed a grant behind our back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasRequired = Permissions.hasRequired(context)
        hasOptional = Permissions.hasOptional(context)
    }

    Box(modifier = modifier.fillMaxSize().background(A1Colors.Field, RectangleShape)) {
        if (hasRequired) {
            RemoteShell(notificationsDenied = !hasOptional)
        } else {
            val permanentlyDenied = asked &&
                Permissions.required().none { permission ->
                    activity?.shouldShowRequestPermissionRationale(permission) == true
                }
            RationaleScreen(
                permanentlyDenied = permanentlyDenied,
                notificationsDenied = asked && !hasOptional,
                onGrant = { launcher.launch(Permissions.all().toTypedArray()) },
                onOpenSettings = { context.openAppSettings() },
            )
        }
    }
}

@Composable
private fun RemoteShell(notificationsDenied: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: RemoteViewModel = viewModel()

    val state by viewModel.link.collectAsStateWithLifecycle()
    val wireLog by viewModel.wireLog.collectAsStateWithLifecycle()
    val bindings by viewModel.bindings.collectAsStateWithLifecycle()

    var screen by rememberSaveable { mutableStateOf(A1Screen.KEYPAD) }
    var diagnosticsOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.connect() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatusRow(
                state = state,
                powerVerified = viewModel.isVerified(RemoteFunction.POWER),
                onPower = { viewModel.press(RemoteFunction.POWER) },
                onOpenDiagnostics = { diagnosticsOpen = true },
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (screen) {
                    A1Screen.KEYPAD -> KeypadScreen(
                        bindings = bindings,
                        connected = state.isConnected,
                        onPress = viewModel::press,
                        onOpenCursor = { screen = A1Screen.CURSOR },
                    )

                    A1Screen.CURSOR -> CursorScreen(
                        onReturnToKeypad = { screen = A1Screen.KEYPAD },
                    )

                    A1Screen.KEY_LAB -> PlaceholderScreen(
                        title = "Key Lab",
                        body = "Candidate discovery arrives at Gate 4. Until then Focus, Source, " +
                            "Flip and Keystone stay unverified.",
                    )

                    A1Screen.SETUP -> SetupScreen(
                        state = state,
                        onConnect = viewModel::connect,
                        onDisconnect = viewModel::disconnect,
                        onMakeDiscoverable = { context.requestDiscoverable() },
                    )
                }
            }

            if (notificationsDenied) {
                NotificationWarning(modifier = Modifier.fillMaxWidth())
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(A1Dimens.Hairline)
                    .background(A1Colors.KeyBorder, RectangleShape),
            )
            A1TabBar(current = screen, onSelect = { screen = it })
        }

        if (diagnosticsOpen) {
            DiagnosticsSheet(
                state = state,
                wireLog = wireLog,
                onRegister = viewModel::connect,
                onUnregister = viewModel::disconnect,
                onClearLog = viewModel::clearWireLog,
                onClose = { diagnosticsOpen = false },
            )
        }
    }
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/**
 * Registering as a HID device does not make the phone discoverable, and the projector has to be
 * the side that initiates the pairing. This is the system's own consent dialog.
 */
private fun Context.requestDiscoverable() {
    startActivity(
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_SECONDS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

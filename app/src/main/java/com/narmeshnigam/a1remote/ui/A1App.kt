package com.narmeshnigam.a1remote.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.service.HidLink
import com.narmeshnigam.a1remote.service.HidService
import com.narmeshnigam.a1remote.service.SendResult
import com.narmeshnigam.a1remote.ui.theme.A1Colors

/**
 * The Gate 1 shell: the permission rationale until the required permissions are held, then the
 * debug screen. Nothing is asked for on cold launch (BUILD_SPEC §7).
 */
@Composable
fun A1App(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    var hasRequired by remember { mutableStateOf(Permissions.hasRequired(context)) }
    var hasOptional by remember { mutableStateOf(Permissions.hasOptional(context)) }
    var asked by remember { mutableStateOf(false) }
    var lastSendResult by remember { mutableStateOf<SendResult?>(null) }

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

    LaunchedEffect(hasRequired) {
        if (hasRequired) HidService.start(context)
    }

    val state by HidLink.state.collectAsStateWithLifecycle()
    val wireLog by HidLink.wireLog.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().background(A1Colors.Field)) {
        if (hasRequired) {
            DebugScreen(
                state = state,
                wireLog = wireLog,
                lastSendResult = lastSendResult,
                notificationsDenied = !hasOptional,
                onRegister = { HidService.start(context) },
                onUnregister = { HidService.stop(context) },
                onSendArrowDown = { lastSendResult = HidLink.sendKey(RemoteFunction.DOWN) },
                onClearLog = { HidLink.clearWireLog() },
            )
        } else {
            val permanentlyDenied = asked &&
                Permissions.required().none { permission ->
                    activity?.shouldShowRequestPermissionRationale(permission) == true
                }
            RationaleScreen(
                permanentlyDenied = permanentlyDenied,
                notificationsDenied = asked && !hasOptional,
                onGrant = { launcher.launch(Permissions.all().toTypedArray()) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                },
            )
        }
    }
}

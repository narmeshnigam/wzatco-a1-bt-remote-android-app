package com.narmeshnigam.a1remote.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.narmeshnigam.a1remote.MainActivity
import com.narmeshnigam.a1remote.R

/**
 * The permanent notification of the foreground service.
 *
 * It is not decoration: on a ROM that stops background work eagerly, this notification is what
 * marks the HID link as something the user is actively using.
 */
internal class LinkNotification(private val context: Context) {

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.hid_service_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.hid_service_channel_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun build(state: LinkState): Notification {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_remote)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(state.hostName?.let { "Connected to $it" } ?: stageText(state.stage))
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open)
            .build()
    }

    private fun stageText(stage: LinkStage): String = when (stage) {
        LinkStage.STOPPED -> "Stopped"
        LinkStage.PERMISSION_DENIED -> "Bluetooth permission not granted"
        LinkStage.NO_BLUETOOTH -> "No Bluetooth adapter"
        LinkStage.BLUETOOTH_OFF -> "Bluetooth is off"
        LinkStage.ACQUIRING_PROXY -> "Acquiring the HID profile"
        LinkStage.PROXY_REFUSED -> "HID profile refused by the system"
        LinkStage.REGISTERING -> "Registering"
        LinkStage.REGISTRATION_REFUSED -> "Registration refused by the system"
        LinkStage.REGISTERED -> "Registered — waiting for the projector"
        LinkStage.CONNECTED -> "Connected"
    }

    companion object {
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "hid_link"
    }
}

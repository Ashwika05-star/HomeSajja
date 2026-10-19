package com.homesajja.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.homesajja.app.MainActivity
import com.homesajja.app.R
import com.homesajja.app.data.model.EntityType

private const val CHANNEL_ACTIVITY = "activity"
private const val CHANNEL_CHAT = "chat_messages"

/** Shows system notifications. Tapping one opens the app on the screen the notification is about. */
object NotificationAlerts {

    /** True while any screen of the app is visible; then the in-app badge is enough and no alert is shown. */
    fun isAppInForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    fun show(context: Context, notificationId: String, title: String, body: String, relatedType: EntityType, relatedId: String) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return

        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chatChannel = NotificationChannel(
                CHANNEL_CHAT,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Direct messages and customer inquiries"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val activityChannel = NotificationChannel(
                CHANNEL_ACTIVITY,
                "Activity & Updates",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Offers, exchanges, repair updates and quotes"
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(chatChannel)
            manager.createNotificationChannel(activityChannel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationRouting.EXTRA_RELATED_TYPE, relatedType.name)
            putExtra(NotificationRouting.EXTRA_RELATED_ID, relatedId)
        }
        val tapAction = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = if (relatedType == EntityType.CHAT) CHANNEL_CHAT else CHANNEL_ACTIVITY
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(tapAction)

        if (relatedType == EntityType.CHAT) {
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        manager.notify(notificationId.hashCode(), builder.build())
    }
}

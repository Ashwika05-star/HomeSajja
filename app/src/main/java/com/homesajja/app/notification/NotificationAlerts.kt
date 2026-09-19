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

private const val CHANNEL_ID = "activity"

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
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Activity", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Messages, requests and updates"
            },
        )

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
        // Same id for the same notification, so a push and a local alert for it replace each other.
        manager.notify(
            notificationId.hashCode(),
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(tapAction)
                .build(),
        )
    }
}

package com.watchdoc.prototype.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.watchdoc.prototype.model.PostStatus

data class NotificationDiagnostics(
    val runtimeRequired: Boolean,
    val permissionGranted: Boolean,
    val applicationEnabled: Boolean,
    val channelEnabled: Boolean,
    val channelImportance: Int
) {
    val blockingStatus: PostStatus? get() = when {
        !permissionGranted -> PostStatus.PERMISSION_BLOCKED
        !applicationEnabled -> PostStatus.APP_DISABLED
        !channelEnabled -> PostStatus.CHANNEL_DISABLED
        else -> null
    }
    val guidance: String get() = when (blockingStatus) {
        PostStatus.PERMISSION_BLOCKED -> "Allow notifications using Request permission. If Android no longer shows a prompt, open app notification settings."
        PostStatus.APP_DISABLED -> "Open app notification settings and enable WatchDoc Prototype notifications."
        PostStatus.CHANNEL_DISABLED -> "Open reading channel settings and enable WatchDoc Reading Tests."
        else -> "Phone posting is available. Check the watch connection in Realme Link."
    }
}

class NotificationStatusChecker(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "watchdoc_reading_tests"
        const val CHANNEL_NAME = "WatchDoc Reading Tests"
    }
    private val manager = checkNotNull(context.getSystemService(NotificationManager::class.java))
    fun ensureChannel() {
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Manually sent document sections and notification payload experiments"
        })
    }
    fun check(): NotificationDiagnostics {
        val required = Build.VERSION.SDK_INT >= 33
        val granted = !required || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val channel = manager.getNotificationChannel(CHANNEL_ID)
        return NotificationDiagnostics(required, granted, NotificationManagerCompat.from(context).areNotificationsEnabled(),
            channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE,
            channel?.importance ?: NotificationManager.IMPORTANCE_NONE)
    }
}

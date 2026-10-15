package com.watchdoc.prototype.notification

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.watchdoc.prototype.MainActivity
import com.watchdoc.prototype.R
import com.watchdoc.prototype.model.BodyFormat
import com.watchdoc.prototype.model.PostStatus
import com.watchdoc.prototype.model.TestRecord

data class PostResult(val status: PostStatus, val detail: String)

class WatchNotificationPublisher(private val context: Context, private val checker: NotificationStatusChecker) {
    companion object {
        const val FIXED_READER_ID = 4242
        const val TAG = "watchdoc_prototype"
    }
    fun phoneLocked(): Boolean = checkNotNull(context.getSystemService(KeyguardManager::class.java)).isKeyguardLocked

    fun post(record: TestRecord): PostResult {
        return try {
            checker.ensureChannel()
            val diagnostics = checker.check()
            diagnostics.blockingStatus?.let { return PostResult(it, diagnostics.guidance) }
            // Explicit guard also allows lint to verify runtime permission at notify().
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return PostResult(PostStatus.PERMISSION_BLOCKED, "Open Setup to allow notifications.")
            }
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = "com.watchdoc.prototype.OPEN.${record.notificationId}"
                putExtra("screen", when (record.source) { "Reader" -> "Reader"; "Setup" -> "Setup"; else -> "Lab" })
            }
            val pending = PendingIntent.getActivity(context, record.notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val builder = NotificationCompat.Builder(context, NotificationStatusChecker.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_watchdoc)
                .setContentTitle(record.title)
                .setContentText(record.body)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setOngoing(false)
                .setOnlyAlertOnce(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            if (record.bodyFormat == BodyFormat.BIG_TEXT) builder.setStyle(NotificationCompat.BigTextStyle().bigText(record.body))
            NotificationManagerCompat.from(context).notify(TAG, record.notificationId, builder.build())
            PostResult(PostStatus.POSTED, "Android notify() returned. Repeated alerting requested; Android/OEM rate limits, channel settings and Realme Link can affect alerts and forwarding.")
        } catch (e: SecurityException) {
            val status = checker.check().blockingStatus ?: PostStatus.ERROR
            PostResult(status, e.message ?: "Android denied notification posting.")
        } catch (e: Exception) {
            PostResult(PostStatus.ERROR, e.message ?: e.javaClass.simpleName)
        }
    }

    fun clear() {
        val manager = checkNotNull(context.getSystemService(NotificationManager::class.java))
        manager.activeNotifications.filter { it.tag == TAG }.forEach { manager.cancel(TAG, it.id) }
    }
}

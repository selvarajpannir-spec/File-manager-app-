package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "keyword_search_channel"
    const val FOREGROUND_CHANNEL_ID = "keyword_search_foreground_channel"
    const val NOTIFICATION_ID = 1001
    const val FOREGROUND_NOTIFICATION_ID = 2001
    const val EXTRA_TARGET_TAB = "extra_target_tab"
    const val TAB_KEYWORD_SEARCH = "KEYWORD_SEARCH"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Completion notifications channel
            val completionChannel = NotificationChannel(
                CHANNEL_ID,
                "Search Completed Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when deep multi-format storage scans finish"
            }
            notificationManager.createNotificationChannel(completionChannel)

            // 2. Foreground scanning progress channel (Ongoing progress)
            val progressChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Deep Search Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress bar and scanning percentage widget in notification bar"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(progressChannel)
        }
    }

    fun buildProgressNotification(
        context: Context,
        keywords: List<String>,
        scannedCount: Int,
        foundCount: Int,
        percent: Int,
        cancelIntent: PendingIntent? = null
    ): Notification {
        createNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_TAB, TAB_KEYWORD_SEARCH)
        }

        val pendingTapIntent = PendingIntent.getActivity(
            context,
            FOREGROUND_NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val keywordsText = keywords.joinToString(", ")
        val title = "Deep Scanning: $percent% complete"
        val content = "Scanned $scannedCount files • Found $foundCount match(es)\nKeywords: \"$keywordsText\""

        val builder = NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle(title)
            .setContentText("Scanned $scannedCount files • $foundCount found ($percent%)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setProgress(100, percent.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(pendingTapIntent)

        if (cancelIntent != null) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                cancelIntent
            )
        }

        return builder.build()
    }

    fun showSearchCompletedNotification(
        context: Context,
        keywords: List<String>,
        foundCount: Int,
        scannedCount: Int
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_TAB, TAB_KEYWORD_SEARCH)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val keywordsText = keywords.joinToString(", ")
        val title = "Files+ Search Completed"
        val content = if (foundCount > 0) {
            "Found $foundCount file(s) matching \"$keywordsText\" ($scannedCount scanned). Tap to open results."
        } else {
            "Scanned $scannedCount files across storage. No matches for \"$keywordsText\"."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}

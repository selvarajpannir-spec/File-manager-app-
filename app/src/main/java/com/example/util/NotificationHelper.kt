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
import com.example.service.NotificationActionReceiver

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

            // 1. Completion notifications channel (swipeable)
            val completionChannel = NotificationChannel(
                CHANNEL_ID,
                "Search Completed Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when deep multi-format storage scans finish or results are found"
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

    /**
     * Builds the active foreground scanning notification widget with live progress and a Cancel/Stop action.
     */
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

        val resolvedCancelIntent = cancelIntent ?: createCancelPendingIntent(context)

        val keywordsText = keywords.joinToString(", ")
        val title = "Scanning Storage: $percent%"
        val summaryLine = "$scannedCount files checked • $foundCount match(es) found"
        val bigText = "$summaryLine\nKeywords: \"$keywordsText\"\nTap to view live results or ❌ to stop."

        return NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setProgress(100, percent.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(pendingTapIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "❌ Cancel Scan",
                resolvedCancelIntent
            )
            .build()
    }

    /**
     * Shows a completed notification that is SWIPEABLE (not ongoing) and has an explicit ❌ Dismiss action.
     */
    fun showSearchCompletedNotification(
        context: Context,
        keywords: List<String>,
        foundCount: Int,
        scannedCount: Int
    ) {
        createNotificationChannel(context)

        // Cancel the ongoing progress notification immediately so it doesn't hang
        cancelForegroundNotification(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_TAB, TAB_KEYWORD_SEARCH)
        }

        val pendingTapIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPendingIntent = createDismissPendingIntent(context, NOTIFICATION_ID)

        val keywordsText = keywords.joinToString(", ")
        val title = if (foundCount > 0) {
            "✅ Found $foundCount Match(es)"
        } else {
            "🔍 Scan Finished (0 Matches)"
        }

        val content = if (foundCount > 0) {
            "Found $foundCount file(s) for \"$keywordsText\" across $scannedCount scanned items.\nTap to open results."
        } else {
            "Scanned $scannedCount files across storage. No files matched \"$keywordsText\"."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingTapIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setOngoing(false) // Allows swipe to close
            .setAutoCancel(true) // Dismiss on tap
            .addAction(
                android.R.drawable.ic_menu_view,
                "📂 Open",
                pendingTapIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "❌ Dismiss",
                dismissPendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    /**
     * Shows a swipeable "Search Cancelled" notification.
     */
    fun showSearchCancelledNotification(
        context: Context,
        keywords: List<String>,
        scannedCount: Int
    ) {
        createNotificationChannel(context)
        cancelForegroundNotification(context)

        val dismissPendingIntent = createDismissPendingIntent(context, NOTIFICATION_ID)

        val keywordsText = keywords.joinToString(", ")
        val title = "🚫 Scan Cancelled"
        val content = "Storage scan stopped after checking $scannedCount files (\"$keywordsText\")."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setDeleteIntent(dismissPendingIntent)
            .setOngoing(false) // Allows swipe to close
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "❌ Dismiss",
                dismissPendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun cancelForegroundNotification(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(FOREGROUND_NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    fun cancelAllSearchNotifications(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(FOREGROUND_NOTIFICATION_ID)
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    fun createCancelPendingIntent(context: Context): PendingIntent {
        val cancelIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CANCEL_SCAN
        }
        return PendingIntent.getBroadcast(
            context,
            3001,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createDismissPendingIntent(context: Context, notificationId: Int): PendingIntent {
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS_NOTIFICATION
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            3002 + notificationId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

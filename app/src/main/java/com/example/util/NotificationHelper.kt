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
    const val SEARCH_NOTIFICATION_ID = 2001
    // Backward compatibility aliases
    const val FOREGROUND_NOTIFICATION_ID = SEARCH_NOTIFICATION_ID
    const val NOTIFICATION_ID = SEARCH_NOTIFICATION_ID

    const val EXTRA_TARGET_TAB = "extra_target_tab"
    const val TAB_KEYWORD_SEARCH = "KEYWORD_SEARCH"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Deep Search Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live search progress and completion results"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds the active scanning progress notification.
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
            SEARCH_NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resolvedCancelIntent = cancelIntent ?: createCancelPendingIntent(context)

        val keywordsText = keywords.joinToString(", ")
        val title = "Scanning Storage: $percent%"
        val summaryLine = "$scannedCount files checked • $foundCount match(es) found"
        val bigText = "$summaryLine\nKeywords: \"$keywordsText\"\nTap to view live results or Cancel to stop."

        return NotificationCompat.Builder(context, CHANNEL_ID)
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
     * Updates the existing notification in-place to the completed state.
     * Marks it as non-ongoing (swipeable) and dismissable.
     */
    fun showSearchCompletedNotification(
        context: Context,
        keywords: List<String>,
        foundCount: Int,
        scannedCount: Int
    ) {
        createNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_TAB, TAB_KEYWORD_SEARCH)
        }

        val pendingTapIntent = PendingIntent.getActivity(
            context,
            SEARCH_NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPendingIntent = createDismissPendingIntent(context, SEARCH_NOTIFICATION_ID)

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
            .setOngoing(false) // Non-ongoing: allows swipe to close
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
            notificationManager.notify(SEARCH_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    /**
     * Updates the notification in-place to the cancelled state.
     */
    fun showSearchCancelledNotification(
        context: Context,
        keywords: List<String>,
        scannedCount: Int
    ) {
        createNotificationChannel(context)

        val dismissPendingIntent = createDismissPendingIntent(context, SEARCH_NOTIFICATION_ID)

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
            .setOngoing(false) // Non-ongoing: allows swipe to close
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "❌ Dismiss",
                dismissPendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(SEARCH_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun cancelForegroundNotification(context: Context) {
        cancelAllSearchNotifications(context)
    }

    fun cancelAllSearchNotifications(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(SEARCH_NOTIFICATION_ID)
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

    fun createDismissPendingIntent(context: Context, notificationId: Int = SEARCH_NOTIFICATION_ID): PendingIntent {
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

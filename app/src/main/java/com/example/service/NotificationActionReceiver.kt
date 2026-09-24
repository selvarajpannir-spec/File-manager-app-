package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.util.NotificationHelper

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CANCEL_SCAN = "com.example.service.ACTION_CANCEL_SCAN"
        const val ACTION_DISMISS_NOTIFICATION = "com.example.service.ACTION_DISMISS_NOTIFICATION"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            ACTION_CANCEL_SCAN -> {
                // Cancel ongoing deep search service and clear ongoing notification
                DeepSearchForegroundService.cancelSearch(context)
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(NotificationHelper.FOREGROUND_NOTIFICATION_ID)
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)
            }
            ACTION_DISMISS_NOTIFICATION -> {
                val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NotificationHelper.NOTIFICATION_ID)
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(notifId)
                notificationManager.cancel(NotificationHelper.FOREGROUND_NOTIFICATION_ID)
            }
        }
    }
}

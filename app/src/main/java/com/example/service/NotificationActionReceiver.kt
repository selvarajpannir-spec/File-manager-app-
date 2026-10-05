package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                DeepSearchForegroundService.cancelSearch(context)
                NotificationHelper.cancelAllSearchNotifications(context)
            }
            ACTION_DISMISS_NOTIFICATION -> {
                NotificationHelper.cancelAllSearchNotifications(context)
            }
        }
    }
}

package com.example.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.data.model.FileSystemItem
import com.example.util.NotificationHelper
import com.example.util.SearchMatchResult
import com.example.util.StorageSearchScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeepSearchServiceState(
    val isRunning: Boolean = false,
    val keywords: List<String> = emptyList(),
    val scannedCount: Int = 0,
    val foundCount: Int = 0,
    val progressPercent: Int = 0,
    val results: List<SearchMatchResult> = emptyList(),
    val isFinished: Boolean = false,
    val isCancelled: Boolean = false,
    val statusMessage: String = ""
)

class DeepSearchForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var scanJob: Job? = null

    companion object {
        private const val TAG = "DeepSearchService"
        const val ACTION_START_SEARCH = "com.example.service.ACTION_START_SEARCH"
        const val ACTION_CANCEL_SEARCH = "com.example.service.ACTION_CANCEL_SEARCH"
        const val EXTRA_KEYWORDS = "extra_keywords"
        const val EXTRA_ROOT_PATH = "extra_root_path"
        const val EXTRA_MATCH_ALL = "extra_match_all"

        private val _searchState = MutableStateFlow(DeepSearchServiceState())
        val searchState: StateFlow<DeepSearchServiceState> = _searchState.asStateFlow()

        fun startSearch(
            context: Context,
            keywords: List<String>,
            rootPath: String? = null,
            matchAll: Boolean = false
        ) {
            val intent = Intent(context, DeepSearchForegroundService::class.java).apply {
                action = ACTION_START_SEARCH
                putStringArrayListExtra(EXTRA_KEYWORDS, ArrayList(keywords))
                putExtra(EXTRA_ROOT_PATH, rootPath)
                putExtra(EXTRA_MATCH_ALL, matchAll)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
            }
        }

        fun cancelSearch(context: Context) {
            val intent = Intent(context, DeepSearchForegroundService::class.java).apply {
                action = ACTION_CANCEL_SEARCH
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send cancel intent: ${e.message}", e)
            }
        }

        fun clearState() {
            _searchState.value = DeepSearchServiceState()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_SEARCH -> {
                val keywords = intent.getStringArrayListExtra(EXTRA_KEYWORDS) ?: arrayListOf()
                val rootPath = intent.getStringExtra(EXTRA_ROOT_PATH)
                val matchAll = intent.getBooleanExtra(EXTRA_MATCH_ALL, false)
                startDeepScanning(keywords, rootPath, matchAll)
            }
            ACTION_CANCEL_SEARCH -> {
                cancelScanning()
            }
        }

        return START_NOT_STICKY
    }

    private fun startDeepScanning(keywords: List<String>, rootPath: String?, matchAll: Boolean) {
        if (keywords.isEmpty()) {
            stopSelf()
            return
        }

        val cancelPendingIntent = createCancelPendingIntent()

        // 1. Enter Foreground Mode immediately to prevent OS process killing
        val initialNotification = NotificationHelper.buildProgressNotification(
            context = this,
            keywords = keywords,
            scannedCount = 0,
            foundCount = 0,
            percent = 0,
            cancelIntent = cancelPendingIntent
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(
                        NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                }
            } else {
                startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}", e)
        }

        _searchState.value = DeepSearchServiceState(
            isRunning = true,
            keywords = keywords,
            scannedCount = 0,
            foundCount = 0,
            progressPercent = 0,
            results = emptyList(),
            isFinished = false,
            isCancelled = false,
            statusMessage = "Deep scanning storage in background..."
        )

        scanJob?.cancel()
        scanJob = serviceScope.launch {
            var lastNotificationUpdateTime = 0L
            val estimatedMaxFiles = 1200 // dynamic scaling base

            try {
                val results = StorageSearchScanner.searchStorage(
                    context = applicationContext,
                    keywords = keywords,
                    rootPath = rootPath,
                    matchAllKeywords = matchAll,
                    onProgress = { scanned, found ->
                        // Calculate percentage with smooth curve
                        val percent = if (scanned >= estimatedMaxFiles) {
                            (90 + ((scanned - estimatedMaxFiles) / 500)).coerceAtMost(98)
                        } else {
                            ((scanned.toFloat() / estimatedMaxFiles) * 90).toInt().coerceIn(1, 90)
                        }

                        _searchState.value = _searchState.value.copy(
                            scannedCount = scanned,
                            foundCount = found,
                            progressPercent = percent
                        )

                        // Throttle notification updates to at most once per 600ms
                        val now = System.currentTimeMillis()
                        if (now - lastNotificationUpdateTime > 600) {
                            lastNotificationUpdateTime = now
                            updateProgressNotification(keywords, scanned, found, percent, cancelPendingIntent)
                        }
                    }
                )

                _searchState.value = DeepSearchServiceState(
                    isRunning = false,
                    keywords = keywords,
                    scannedCount = _searchState.value.scannedCount,
                    foundCount = results.size,
                    progressPercent = 100,
                    results = results,
                    isFinished = true,
                    isCancelled = false,
                    statusMessage = "Found ${results.size} match(es) across ${_searchState.value.scannedCount} files."
                )

                // Show completed alert notification
                NotificationHelper.showSearchCompletedNotification(
                    context = applicationContext,
                    keywords = keywords,
                    foundCount = results.size,
                    scannedCount = _searchState.value.scannedCount
                )
            } catch (e: Exception) {
                Log.e(TAG, "Search error: ${e.message}", e)
                _searchState.value = _searchState.value.copy(
                    isRunning = false,
                    isFinished = true,
                    statusMessage = "Search ended: ${e.message}"
                )
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun updateProgressNotification(
        keywords: List<String>,
        scanned: Int,
        found: Int,
        percent: Int,
        cancelIntent: PendingIntent
    ) {
        val notification = NotificationHelper.buildProgressNotification(
            context = this,
            keywords = keywords,
            scannedCount = scanned,
            foundCount = found,
            percent = percent,
            cancelIntent = cancelIntent
        )
        try {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing: ${e.message}")
        }
    }

    private fun cancelScanning() {
        scanJob?.cancel()
        _searchState.value = _searchState.value.copy(
            isRunning = false,
            isCancelled = true,
            statusMessage = "Search cancelled by user."
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createCancelPendingIntent(): PendingIntent {
        val cancelIntent = Intent(this, DeepSearchForegroundService::class.java).apply {
            action = ACTION_CANCEL_SEARCH
        }
        return PendingIntent.getService(
            this,
            2002,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
        serviceScope.cancel()
    }
}

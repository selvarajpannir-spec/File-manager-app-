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
    private var currentKeywords: List<String> = emptyList()

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

        currentKeywords = keywords
        val cancelPendingIntent = NotificationHelper.createCancelPendingIntent(this)

        // 1. Enter Foreground Mode with unified notification ID
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
                startForeground(
                    NotificationHelper.SEARCH_NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NotificationHelper.SEARCH_NOTIFICATION_ID, initialNotification)
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
            val estimatedMaxFiles = 800

            try {
                val results = StorageSearchScanner.searchStorage(
                    context = applicationContext,
                    keywords = keywords,
                    rootPath = rootPath,
                    matchAllKeywords = matchAll,
                    onProgress = { scanned, found ->
                        val percent = if (scanned >= estimatedMaxFiles) {
                            (85 + ((scanned - estimatedMaxFiles) / 300)).coerceAtMost(98)
                        } else {
                            ((scanned.toFloat() / estimatedMaxFiles) * 85).toInt().coerceIn(1, 85)
                        }

                        _searchState.value = _searchState.value.copy(
                            scannedCount = scanned,
                            foundCount = found,
                            progressPercent = percent
                        )

                        // Throttle notification updates (~350ms)
                        val now = System.currentTimeMillis()
                        if (now - lastNotificationUpdateTime > 350) {
                            lastNotificationUpdateTime = now
                            updateProgressNotification(keywords, scanned, found, percent, cancelPendingIntent)
                        }
                    },
                    onMatchFound = { newMatch ->
                        val currentList = _searchState.value.results
                        val updated = if (currentList.none { it.item.path == newMatch.item.path }) {
                            currentList + newMatch
                        } else {
                            currentList
                        }
                        val currentScanned = _searchState.value.scannedCount
                        _searchState.value = _searchState.value.copy(
                            results = updated,
                            foundCount = updated.size
                        )

                        // Immediately update notification when new match is found
                        updateProgressNotification(
                            keywords,
                            currentScanned,
                            updated.size,
                            _searchState.value.progressPercent,
                            cancelPendingIntent
                        )
                    }
                )

                val finalScanned = _searchState.value.scannedCount
                _searchState.value = DeepSearchServiceState(
                    isRunning = false,
                    keywords = keywords,
                    scannedCount = finalScanned,
                    foundCount = results.size,
                    progressPercent = 100,
                    results = results,
                    isFinished = true,
                    isCancelled = false,
                    statusMessage = "Found ${results.size} match(es) across $finalScanned files."
                )

                // 1. Detach notification from Foreground Service so it becomes swipeable/dismissable
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(false)
                }

                // 2. Update the EXACT SAME notification ID in-place to the completed swipeable state
                NotificationHelper.showSearchCompletedNotification(
                    context = applicationContext,
                    keywords = keywords,
                    foundCount = results.size,
                    scannedCount = finalScanned
                )
            } catch (e: Exception) {
                Log.e(TAG, "Search error: ${e.message}", e)
                _searchState.value = _searchState.value.copy(
                    isRunning = false,
                    isFinished = true,
                    statusMessage = "Search ended: ${e.message}"
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                NotificationHelper.cancelAllSearchNotifications(applicationContext)
            } finally {
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
            notificationManager.notify(NotificationHelper.SEARCH_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing: ${e.message}")
        }
    }

    private fun cancelScanning() {
        scanJob?.cancel()
        val scanned = _searchState.value.scannedCount
        val keywords = currentKeywords.ifEmpty { _searchState.value.keywords }

        _searchState.value = _searchState.value.copy(
            isRunning = false,
            isCancelled = true,
            statusMessage = "Search cancelled by user."
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        NotificationHelper.cancelAllSearchNotifications(this)

        if (keywords.isNotEmpty()) {
            NotificationHelper.showSearchCancelledNotification(
                context = applicationContext,
                keywords = keywords,
                scannedCount = scanned
            )
        }

        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            scanJob?.cancel()
            _searchState.value = _searchState.value.copy(
                isRunning = false,
                isCancelled = true
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            NotificationHelper.cancelAllSearchNotifications(this)
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up onTaskRemoved: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
        serviceScope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        NotificationHelper.cancelAllSearchNotifications(this)
    }
}

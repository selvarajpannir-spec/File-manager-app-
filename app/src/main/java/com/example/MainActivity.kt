package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FileExplorerTab
import com.example.ui.FileManagerScreen
import com.example.ui.FileManagerViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppSettings
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private lateinit var appSettings: AppSettings
    private var pendingTargetTab: String? = null
    private var activeViewModel: FileManagerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appSettings = AppSettings.getInstance(applicationContext)

        NotificationHelper.createNotificationChannel(this)
        com.example.util.StorageSearchScanner.initPdfBoxIfNeeded(this)
        handleNotificationIntent(intent)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 102)
            }
        }

        setContent {
            val themeMode by appSettings.themeMode.collectAsStateWithLifecycle()
            val fontScale by appSettings.fontScale.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themeMode = themeMode,
                fontScaleMultiplier = fontScale.scale
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: FileManagerViewModel = viewModel()
                    activeViewModel = viewModel

                    // If app was opened via notification, switch directly to KEYWORD_SEARCH tab
                    LaunchedEffect(pendingTargetTab) {
                        if (pendingTargetTab == NotificationHelper.TAB_KEYWORD_SEARCH) {
                            viewModel.setActiveTab(FileExplorerTab.KEYWORD_SEARCH)
                            pendingTargetTab = null
                        }
                    }

                    BackHandler(enabled = true) {
                        val handled = viewModel.navigateBack()
                        if (!handled) {
                            finish()
                        }
                    }

                    FileManagerScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val targetTab = intent?.getStringExtra(NotificationHelper.EXTRA_TARGET_TAB)
        if (targetTab == NotificationHelper.TAB_KEYWORD_SEARCH) {
            pendingTargetTab = targetTab
            activeViewModel?.setActiveTab(FileExplorerTab.KEYWORD_SEARCH)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndPromptAllFilesPermission()
    }

    private fun checkAndPromptAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Verified via in-app banner & dialog
            }
        }
    }
}

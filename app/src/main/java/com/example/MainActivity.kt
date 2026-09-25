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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FileExplorerTab
import com.example.ui.FileManagerScreen
import com.example.ui.FileManagerViewModel
import com.example.ui.components.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppSettings
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private lateinit var appSettings: AppSettings
    private var pendingTargetTab: String? = null
    private var activeViewModel: FileManagerViewModel? = null
    private var hasPromptedStorageOnOpen = false

    private val manageAllFilesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        activeViewModel?.refreshStoragePermission(this)
    }

    private val legacyStorageLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        activeViewModel?.refreshStoragePermission(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appSettings = AppSettings.getInstance(applicationContext)

        NotificationHelper.createNotificationChannel(this)
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
            var showSplash by rememberSaveable { mutableStateOf(pendingTargetTab == null) }

            MyApplicationTheme(
                themeMode = themeMode,
                fontScaleMultiplier = fontScale.scale
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(400),
                        label = "splash_transition"
                    ) { isSplashVisible ->
                        if (isSplashVisible) {
                            SplashScreen(
                                onSplashComplete = {
                                    showSplash = false
                                    checkAndLeadToStoragePermission()
                                }
                            )
                        } else {
                            val viewModel: FileManagerViewModel = viewModel()
                            activeViewModel = viewModel

                            LaunchedEffect(Unit) {
                                checkAndLeadToStoragePermission()
                            }

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
        activeViewModel?.refreshStoragePermission(this)
    }

    /**
     * Checks if storage permission is granted when opening the app.
     * If not granted, leads directly to permission access.
     */
    private fun checkAndLeadToStoragePermission() {
        if (hasPromptedStorageOnOpen) return
        hasPromptedStorageOnOpen = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageAllFilesLauncher.launch(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        manageAllFilesLauncher.launch(intent)
                    } catch (_: Exception) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        manageAllFilesLauncher.launch(intent)
                    }
                }
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                legacyStorageLauncher.launch(needed.toTypedArray())
            }
        }
    }
}

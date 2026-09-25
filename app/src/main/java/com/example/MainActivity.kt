package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            val context = LocalContext.current
            val themeMode by appSettings.themeMode.collectAsStateWithLifecycle()
            val fontScale by appSettings.fontScale.collectAsStateWithLifecycle()
            var showSplash by rememberSaveable { mutableStateOf(pendingTargetTab == null) }
            var showExitConfirmDialog by remember { mutableStateOf(false) }
            var lastBackPressTime by remember { mutableLongStateOf(0L) }

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
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastBackPressTime < 2000L) {
                                    // Double tap on back button anywhere in app -> open exit confirmation window directly
                                    showExitConfirmDialog = true
                                } else {
                                    lastBackPressTime = currentTime
                                    val handled = viewModel.navigateBack()
                                    if (!handled) {
                                        Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            FileManagerScreen(viewModel = viewModel)

                            // Exit Confirmation Window / Dialog
                            if (showExitConfirmDialog) {
                                AlertDialog(
                                    onDismissRequest = { showExitConfirmDialog = false },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    },
                                    title = {
                                        Text(
                                            text = "Exit Application",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = "Are you sure you want to close and exit the app?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showExitConfirmDialog = false
                                                finishAffinity()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.testTag("confirm_exit_btn")
                                        ) {
                                            Text("Exit")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = { showExitConfirmDialog = false },
                                            modifier = Modifier.testTag("cancel_exit_btn")
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
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

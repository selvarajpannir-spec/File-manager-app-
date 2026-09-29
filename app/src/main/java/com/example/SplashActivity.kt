package com.example

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppSettings
import com.example.util.NotificationHelper
import com.example.util.StorageSearchScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private lateinit var appSettings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appSettings = AppSettings.getInstance(applicationContext)

        // Run background preparations concurrently during the 3-second intro scene
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.createNotificationChannel(applicationContext)
                StorageSearchScanner.initPdfBoxIfNeeded(applicationContext)
            } catch (e: Exception) {
                // Ignore background preparation errors
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
                    SplashScreen(
                        onSplashComplete = {
                            navigateToMainActivity()
                        }
                    )
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            // Forward any notification or launcher extras to MainActivity
            intent.extras?.let { putExtras(it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(mainIntent)
        finish()
    }
}

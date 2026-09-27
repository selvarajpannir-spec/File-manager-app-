package com.example.ui.components

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private const val BANNER_REMOTE_URL = "https://selvarajpannir-spec.github.io/onsite/banner.html"
private const val BANNER_ASSET_URL = "file:///android_asset/banner.html"

/**
 * JavaScript Bridge interface exposed to banner.html
 */
class PromoBridge(private val context: Context) {
    @JavascriptInterface
    fun openUrl(url: String?) {
        if (url.isNullOrBlank()) return
        openPlayStoreOrBrowser(context, url)
    }

    @JavascriptInterface
    fun openPlayStore(packageNameOrUrl: String?) {
        if (packageNameOrUrl.isNullOrBlank()) return
        openPlayStoreOrBrowser(context, packageNameOrUrl)
    }
}

/**
 * Launches the Play Store natively using market:// with com.android.vending,
 * with standard browser fallback if Google Play Store app is unavailable.
 */
fun openPlayStoreOrBrowser(context: Context, rawUrl: String) {
    val url = rawUrl.trim()
    if (url.isEmpty()) return

    val packageName = when {
        url.startsWith("market://details?id=") -> {
            url.removePrefix("market://details?id=").substringBefore("&")
        }
        url.contains("play.google.com/store/apps/details") -> {
            try {
                Uri.parse(url).getQueryParameter("id")
            } catch (e: Exception) {
                null
            }
        }
        !url.contains("/") && !url.contains(":") -> {
            // Raw package name passed directly
            url
        }
        else -> null
    }

    if (!packageName.isNullOrBlank()) {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                setPackage("com.android.vending")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(marketIntent)
            return
        } catch (e: ActivityNotFoundException) {
            // Google Play Store app not installed, open via web browser
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                return
            } catch (e2: Exception) {
                Toast.makeText(context, "Cannot open Play Store: ${e2.message}", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            // Other error
        }
    }

    // Generic URL opening
    try {
        val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(genericIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open link: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * AdMob / Promo Ad Banner displaying the user's apps from GitHub page
 * with full JavaScript bridge to native Google Play Store.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PromoAdBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("admob_promo_banner_surface"),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
        ) {
            if (!hasError) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                allowContentAccess = true
                                allowFileAccess = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }

                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = false
                            overScrollMode = WebView.OVER_SCROLL_NEVER
                            setBackgroundColor(0) // transparent background

                            addJavascriptInterface(PromoBridge(ctx), "Android")

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    openPlayStoreOrBrowser(ctx, url)
                                    return true
                                }

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    url: String?
                                ): Boolean {
                                    if (url.isNullOrBlank()) return false
                                    openPlayStoreOrBrowser(ctx, url)
                                    return true
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    // Fallback to local embedded asset if remote network fails
                                    if (request?.isForMainFrame == true) {
                                        view?.loadUrl(BANNER_ASSET_URL)
                                    }
                                }
                            }

                            // Initial load from GitHub Pages
                            loadUrl(BANNER_REMOTE_URL)
                        }
                    },
                    update = { webView ->
                        // No-op for update; webView runs internal JS rotation
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Compose native fallback
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            openPlayStoreOrBrowser(
                                context,
                                "https://play.google.com/store/apps/developer?id=Selvaraj+Pannir"
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1A73E8)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = "Our Apps",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Explore More Apps by Developer",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Rider+, Inventory Manager+, Lifting+, Salary+, Docs+",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Get",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

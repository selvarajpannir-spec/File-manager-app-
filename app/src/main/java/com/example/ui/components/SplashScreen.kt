package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Intro Splash Scene for Files+
 * App Icon starts at 1/10th (0.10) screen display width and smoothly grows to 1/5th (0.20) screen display width
 * over the 3-second (3000ms) intro duration before launching the main file manager.
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    // Animation progress from 0f to 1f over exactly 3000ms
    val iconScaleFraction = remember { Animatable(0.10f) } // Starts at 1/10 screen width (0.10)
    val textAlpha = remember { Animatable(0f) }
    val loadProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Icon growth animation from 1/10 (0.10) to 1/5 (0.20) of screen width over 3000ms
        launch {
            iconScaleFraction.animateTo(
                targetValue = 0.20f,
                animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing)
            )
        }

        // 2. Text fade-in after 300ms
        launch {
            delay(300)
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = LinearEasing)
            )
        }

        // 3. Loading bar progress from 0% to 100% over 3000ms
        launch {
            loadProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2950, easing = LinearEasing)
            )
        }

        // Total running time: exactly 3 seconds
        delay(3000)
        onSplashComplete()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF131722),
                        Color(0xFF0F172A),
                        Color(0xFF1E1B18),
                        Color(0xFF0B0E14)
                    )
                )
            )
            .testTag("intro_splash_scene"),
        contentAlignment = Alignment.Center
    ) {
        val screenWidth = maxWidth
        val currentIconSize = screenWidth * iconScaleFraction.value

        // Ambient background glow
        Box(
            modifier = Modifier
                .size(currentIconSize * 2.2f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD54F).copy(alpha = 0.22f),
                            Color(0xFFFFB300).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // App Icon: Yellow folder with zooming lens (starts at 1/10 screen -> ends at 1/5 screen)
            Surface(
                modifier = Modifier
                    .size(currentIconSize)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp))
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFFFD54F).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clip(RoundedCornerShape(22.dp)),
                color = Color(0xFFFBC02D)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_yellow_folder_lens_1790147092379),
                    contentDescription = "Files+ App Icon",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title & Plus Badge with smooth fade-in
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Files",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = textAlpha.value)
                    )
                    Text(
                        text = "+",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFCA28).copy(alpha = textAlpha.value)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Fast Storage & Deep Multi-Format Search",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color = Color(0xFF94A3B8).copy(alpha = textAlpha.value)
            )
        }

        // Bottom progress bar and version
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 42.dp, start = 48.dp, end = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { loadProgress.value },
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFFFCA28),
                trackColor = Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Files+ v2.0 • Ready in ${(3 - (loadProgress.value * 3)).toInt().coerceAtLeast(1)}s",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B)
            )
        }
    }
}

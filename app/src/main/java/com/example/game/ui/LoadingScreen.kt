package com.example.game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngineViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(
    viewModel: GameEngineViewModel,
    onLoadingComplete: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var tipIndex by remember { mutableStateOf(0) }
    
    val tips = listOf(
        "Land fast. Loot smart.",
        "Stay inside the safe zone.",
        "Your squad is your strength.",
        "Keep moving. Still targets are easy prey.",
        "Listen closely: footsteps betray your enemies."
    )

    // Progress animation
    LaunchedEffect(Unit) {
        val totalDuration = 3000L // 3 seconds load time
        val steps = 100
        val delayPerStep = totalDuration / steps
        for (i in 0..steps) {
            progress = i / 100f
            delay(delayPerStep)
        }
        onLoadingComplete()
    }

    // Tip rotation
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    // Metallic dark theme background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D11),
                        Color(0xFF050507)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background red/orange glow
        Box(
            modifier = Modifier
                .size(450.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            FireOrange.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Centered USK FIRE MAX logo with subtle glow animation
            UskBrandingLogo(
                modifier = Modifier
                    .size(180.dp)
                    .padding(8.dp),
                glow = true
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Loading label
            Text(
                text = "LOADING... ${(progress * 100).toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = FireOrange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Animated progress bar
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(BloodRed, FireOrange, NeonOrange)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Tips rotating
            Box(
                modifier = Modifier
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tips[tipIndex],
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

package com.example.game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngineViewModel
import com.example.ui.theme.*

@Composable
fun MatchmakingScreen(
    viewModel: GameEngineViewModel,
    playersCount: Int,
    statusText: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    
    val planeTranslateX by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "plane_slide"
    )

    val parachuteAngle by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "parachute_sway"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkGrey, Color(0xFF130906), DarkGrey)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative floating pre-match clouds
        Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = "Clouds",
            tint = Color.White.copy(alpha = 0.05f),
            modifier = Modifier
                .size(120.dp)
                .offset(x = (-80).dp, y = (-200).dp)
        )
        Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = "Clouds",
            tint = Color.White.copy(alpha = 0.05f),
            modifier = Modifier
                .size(180.dp)
                .offset(x = 100.dp, y = 150.dp)
        )

        // Flying aircraft vector animation
        Icon(
            imageVector = Icons.Default.AirplanemodeActive,
            contentDescription = "Aircraft drop plane",
            tint = Color.White.copy(alpha = 0.25f),
            modifier = Modifier
                .size(44.dp)
                .offset(x = planeTranslateX.dp, y = (-120).dp)
                .rotate(90f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            // Centered USK FIRE MAX logo with subtle glow animation
            UskBrandingLogo(
                modifier = Modifier
                    .size(130.dp)
                    .padding(4.dp),
                glow = true
            )

            // Status message
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "PREPARING USK ISLAND...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FireOrange,
                    letterSpacing = 2.sp
                )
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100,
                    textAlign = TextAlign.Center
                )
            }

            // Circular progress counters
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                CircularProgressIndicator(
                    progress = { playersCount / 50f },
                    color = FireOrange,
                    strokeWidth = 6.dp,
                    modifier = Modifier.size(90.dp),
                    trackColor = Color.Black
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$playersCount/50",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate100
                    )
                    Text(
                        text = "SOLDIERS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                }
            }

            // Tips box (Sleek card alternative)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Black40)
                    .border(1.dp, BorderWhite10, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "TACTICAL TIPS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldYellow,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Stand inside green bushes to gain full stealth cover. Opponents and bots will lose sight of you unless you open fire!",
                        fontSize = 10.sp,
                        color = Slate400,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}


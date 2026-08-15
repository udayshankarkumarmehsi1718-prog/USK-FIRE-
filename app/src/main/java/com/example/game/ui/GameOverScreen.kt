package com.example.game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngineViewModel
import com.example.ui.theme.*

@Composable
fun GameOverScreen(viewModel: GameEngineViewModel) {
    val isWin = viewModel.isVictory
    val kills = viewModel.currentMatchKills
    val rank = viewModel.currentMatchRank

    val rewardCoins = (kills * 25) + (if (isWin) 350 else 50)
    val rewardXp = (kills * 50) + (if (isWin) 200 else 50)

    val infiniteTransition = rememberInfiniteTransition(label = "game_over_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
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
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(CarbonGrey)
                .border(2.dp, if (isWin) GoldYellow else FireOrange, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Centered USK FIRE MAX logo with subtle glow animation
            UskBrandingLogo(
                modifier = Modifier
                    .size(110.dp)
                    .padding(4.dp),
                glow = true
            )

            // Big Bold Branded Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isWin) "USK WIN" else "ELIMINATED",
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = if (isWin) GoldYellow else BloodRed,
                    letterSpacing = 1.sp,
                    modifier = Modifier.rotate(-0.5f)
                )
                Text(
                    text = if (isWin) "SURVIVED THE FIERCE BATTLE" else "BETTER LUCK NEXT CONTRACT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Slate400,
                    letterSpacing = 1.sp
                )
            }

            // Statistics Summary Box (Sleek card alternative)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, BorderWhite10, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatRow(label = "Final Placement", value = "#$rank / 50", icon = Icons.Default.Leaderboard, tint = GoldYellow)
                    StatRow(label = "Warriors Knocked", value = "$kills Kills", icon = Icons.Default.GpsFixed, tint = BloodRed)
                    StatRow(label = "Fighter XP Gained", value = "+$rewardXp XP", icon = Icons.Default.Star, tint = FireOrange)
                    StatRow(label = "Loot Coins Gained", value = "+$rewardCoins Coins", icon = Icons.Default.MonetizationOn, tint = GoldYellow)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action triggers (Play Again or Lobby return)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.startMatchmaking() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("play_again_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(BloodRed, FireOrange, BloodRed)
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PlayArrow, "Deploy", tint = Color.White)
                            Text("RE-DEPLOY WARRIOR", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.returnToLobby() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("return_lobby_button"),
                    border = BorderStroke(1.dp, BorderWhite10),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Black40),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Home, "Home", tint = Slate400)
                        Text("RETURN TO LOBBY", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate400)
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
            Text(text = label, fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Bold)
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Slate100
        )
    }
}


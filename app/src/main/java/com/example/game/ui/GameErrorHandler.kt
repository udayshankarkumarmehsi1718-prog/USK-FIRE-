package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GameErrorHandler(
    modifier: Modifier = Modifier,
    systemError: String?,
    onReset: () -> Unit,
    content: @Composable () -> Unit
) {
    var caughtExceptionMessage by remember { mutableStateOf<String?>(null) }
    val activeErrorMessage = systemError ?: caughtExceptionMessage

    if (activeErrorMessage != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF2C0B0A), Color(0xFF130906), Color(0xFF1A0505))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CarbonGrey)
                    .border(2.dp, BloodRed, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(BloodRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "System Error",
                        tint = BloodRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "USK FIRE",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "SYSTEM ERROR DETECTED",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BloodRed,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = activeErrorMessage,
                        fontSize = 12.sp,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }

                Button(
                    onClick = {
                        caughtExceptionMessage = null
                        onReset()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BloodRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("system_error_restart_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "RESTART GAME",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    } else {
        content()
    }
}

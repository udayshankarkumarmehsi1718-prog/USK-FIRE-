package com.example.game.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.data.PlayerProfile
import com.example.game.engine.GameEngineViewModel
import com.example.game.model.CharacterType
import com.example.game.model.WeaponType
import com.example.ui.theme.*

@Composable
fun LobbyScreen(
    viewModel: GameEngineViewModel,
    profile: PlayerProfile
) {
    var activeSubTab by remember { mutableStateOf("PLAY") } // PLAY, HEROES, SKINS, SETTINGS, BATTLEPASS
    var activeEmote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeEmote) {
        if (activeEmote != null) {
            kotlinx.coroutines.delay(4000)
            activeEmote = null
        }
    }

    // Dialog/Overlay trigger states
    var showStatsDialog by remember { mutableStateOf(false) }
    var showContractsDialog by remember { mutableStateOf(false) }
    var showEventsDialog by remember { mutableStateOf(false) }
    var showFriendsDialog by remember { mutableStateOf(false) }
    var showMailDialog by remember { mutableStateOf(false) }

    var selectedMode by remember { mutableStateOf("BATTLE ROYALE") }

    val infiniteTransition = rememberInfiniteTransition(label = "lobby_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val characterIdleOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "char_idle"
    )

    val danceRotation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dance_rot"
    )

    val laughVibration by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laugh_vib"
    )

    val waveSway by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_sway"
    )

    val activeChar = when (profile.selectedCharacterId) {
        "krono" -> CharacterType.KRONO
        "kira" -> CharacterType.KIRA
        "nexus" -> CharacterType.NEXUS
        "vixen" -> CharacterType.VIXEN
        else -> CharacterType.KRONO
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGrey)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Absolute Radial Glow background centered
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(FireOrange.copy(alpha = 0.15f), Color.Transparent),
                        radius = 1200f
                    )
                )
        )

        // Subtle bottom light glow
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BloodRed.copy(alpha = 0.12f), Color.Transparent),
                        radius = 400f
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Sleek Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile level and name summary (Left)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Slate800)
                            .border(2.dp, NeonOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LV.${profile.level}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonOrange
                        )
                    }

                    Column {
                        Text(
                            text = profile.playerName.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Slate100,
                            modifier = Modifier.rotate(-0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Experience XP bar
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Slate800)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((profile.xp.toFloat() / (profile.level * 500f)).coerceIn(0f, 1f))
                                    .background(Brush.horizontalGradient(listOf(FireOrange, BloodRed)))
                            )
                        }
                    }
                }

                // Centered USK FIRE MAX logo in lobby top bar
                UskBrandingLogo(
                    modifier = Modifier
                        .size(52.dp)
                        .padding(horizontal = 4.dp),
                    glow = true
                )

                // Balance Coins, Diamonds & Settings Shortcuts (Right)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gold Coins Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Black40)
                            .border(1.dp, BorderWhite10, RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = GoldYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${profile.coins}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }

                    // Diamonds Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Black40)
                            .border(1.dp, BorderWhite10, RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "Diamonds",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${profile.diamonds}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }

                    // Friends Quick Button
                    IconButton(
                        onClick = { showFriendsDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Black40)
                            .border(1.dp, BorderWhite10, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Friends List",
                            tint = Slate100,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Mail Inbox Quick Button
                    IconButton(
                        onClick = { showMailDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Black40)
                            .border(1.dp, BorderWhite10, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Mail Inbox",
                            tint = Slate100,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Music Control Quick Toggle
                    IconButton(
                        onClick = { viewModel.toggleMusic() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (profile.musicEnabled) FireOrange.copy(alpha = 0.2f) else Black40)
                            .border(1.dp, if (profile.musicEnabled) FireOrange else BorderWhite10, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (profile.musicEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Music",
                            tint = if (profile.musicEnabled) FireOrange else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 2. Main Middle Area (Left sidebar + Center Panel + Right sidebar)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                // LEFT SIDEBAR (👕, 🔫, 🎒, 🚗)
                Column(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SidebarButton(emoji = "👕", selected = activeSubTab == "HEROES", onClick = { activeSubTab = "HEROES" })
                    SidebarButton(emoji = "🔫", selected = activeSubTab == "SKINS", onClick = { activeSubTab = "SKINS" })
                    SidebarButton(emoji = "🎒", selected = activeSubTab == "BATTLEPASS", onClick = { activeSubTab = "BATTLEPASS" })
                    SidebarButton(emoji = "🚗", selected = activeSubTab == "SETT", onClick = { activeSubTab = "SETT" })
                }

                // CENTER CONTENT CONTAINER (translucent panels switcher)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp)
                ) {
                    when (activeSubTab) {
                        "PLAY" -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Vertical line glowing guide behind
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .fillMaxHeight(0.85f)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, FireOrange.copy(alpha = 0.3f), Color.Transparent)
                                                )
                                            )
                                    )

                                    // Centered character cards showcase & Emote Wheel side-by-side
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. Animated Character Card
                                        val iconRotation = when (activeEmote) {
                                            "DANCE" -> danceRotation
                                            "WAVE" -> waveSway
                                            else -> 0f
                                        }
                                        val iconOffsetY = when (activeEmote) {
                                            "LAUGH" -> laughVibration
                                            else -> characterIdleOffset
                                        }
                                        val iconScale = when (activeEmote) {
                                            "VICTORY" -> 1.25f * pulseScale
                                            "DANCE" -> 1.12f
                                            else -> 1.0f
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(180.dp)
                                                .height(340.dp)
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(Color(0x0FFFFFFF))
                                                .border(BorderStroke(1.dp, BorderWhite10), RoundedCornerShape(24.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(90.dp)
                                                        .scale(iconScale)
                                                        .rotate(iconRotation)
                                                        .offset(y = iconOffsetY.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.radialGradient(
                                                                colors = listOf(activeChar.color.copy(alpha = 0.3f), Color.Transparent)
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = when(profile.selectedCharacterId) {
                                                            "krono" -> Icons.Default.Shield
                                                            "kira" -> Icons.Default.Healing
                                                            "nexus" -> Icons.Default.FlashOn
                                                            else -> Icons.Default.DirectionsRun
                                                        },
                                                        contentDescription = activeChar.charName,
                                                        tint = activeChar.color,
                                                        modifier = Modifier.size(50.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(14.dp))

                                                if (activeEmote != null) {
                                                    val emoteLabel = when (activeEmote) {
                                                        "DANCE" -> "🕺 DANCING!"
                                                        "VICTORY" -> "🏆 CHAMPION!"
                                                        "LAUGH" -> "😂 HAHAHA!"
                                                        "WAVE" -> "👋 GREETINGS!"
                                                        else -> ""
                                                    }
                                                    Text(
                                                        text = emoteLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = GoldYellow,
                                                        modifier = Modifier
                                                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                }

                                                Text(
                                                    text = activeChar.charName.uppercase(),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Slate100,
                                                    letterSpacing = 1.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "TACTICAL RECON",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = FireOrange,
                                                    letterSpacing = 2.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = activeChar.desc,
                                                    fontSize = 10.sp,
                                                    color = Slate400,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 13.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        // 2. Functional Emotes Wheel Selector panel
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Black60),
                                            border = BorderStroke(1.dp, BorderWhite10),
                                            modifier = Modifier
                                                .width(135.dp)
                                                .height(340.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "EMOTES SYSTEM",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = FireOrange,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    Text(
                                                        text = "Interactive moves",
                                                        fontSize = 8.sp,
                                                        color = Slate400
                                                    )
                                                }

                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    val emoteList = listOf(
                                                        Triple("🕺 DANCE", "DANCE", Color(0xFFE040FB)),
                                                        Triple("🏆 VICTORY", "VICTORY", Color(0xFFFFD700)),
                                                        Triple("😂 LAUGH", "LAUGH", Color(0xFF00E5FF)),
                                                        Triple("👋 WAVE", "WAVE", Color(0xFF00FF87))
                                                    )

                                                    emoteList.forEach { (label, code, color) ->
                                                        val active = activeEmote == code
                                                        Button(
                                                            onClick = {
                                                                activeEmote = code
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (active) color.copy(alpha = 0.25f) else Black40
                                                            ),
                                                            border = BorderStroke(1.dp, if (active) color else BorderWhite10),
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (active) color else Color.White
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = "Tap button to play",
                                                    fontSize = 7.sp,
                                                    color = Slate500
                                                )
                                            }
                                        }
                                    }
                                }

                                // Interactive footer match triggers inside Home lobby
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp, start = 4.dp, end = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Mode select card (Left)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Black60)
                                            .border(1.dp, BorderWhite10, RoundedCornerShape(16.dp))
                                            .clickable {
                                                val modesList = listOf("BATTLE ROYALE", "TDM ARENA", "SOLO TRAINING", "CUSTOM ROOM")
                                                val currentIdx = modesList.indexOf(selectedMode)
                                                selectedMode = modesList[(currentIdx + 1) % modesList.size]
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(GrassGreen)
                                                )
                                                Text(
                                                    text = "REGION: NA",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate400,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "CURRENT MODE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = FireOrange,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = selectedMode,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Slate100,
                                                modifier = Modifier.rotate(-0.5f)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x10FFFFFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (selectedMode) {
                                                    "BATTLE ROYALE" -> "🗺️"
                                                    "TDM ARENA" -> "⚔️"
                                                    "SOLO TRAINING" -> "🏋️"
                                                    else -> "🔑"
                                                },
                                                fontSize = 18.sp
                                            )
                                        }
                                    }

                                    // Massive Launch Match Button (Right)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.scale(pulseScale)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(152.dp)
                                                .height(64.dp)
                                                .background(BloodRed.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                        )
                                        Button(
                                            onClick = { viewModel.startMatchmaking() },
                                            modifier = Modifier
                                                .width(140.dp)
                                                .height(54.dp)
                                                .testTag("play_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                            contentPadding = PaddingValues(0.dp),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.linearGradient(
                                                            colors = listOf(BloodRed, FireOrange, BloodRed)
                                                        ),
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(14.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "INITIALIZE",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        letterSpacing = 2.sp
                                                    )
                                                    Text(
                                                        text = "USK START",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White,
                                                        modifier = Modifier.rotate(-0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "HEROES" -> {
                            CharacterTabContent(viewModel, profile)
                        }
                        "SKINS" -> {
                            WeaponsTabContent(viewModel, profile)
                        }
                        "BATTLEPASS" -> {
                            BattlePassTabContent(viewModel, profile)
                        }
                        "SETT" -> {
                            SettingsTabContent(viewModel, profile)
                        }
                    }
                }

                // RIGHT SIDEBAR (Events box, 🏆 Stats, 💬 Contracts)
                Column(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Events Mini Widget
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Black60)
                            .border(1.dp, BorderWhite10, RoundedCornerShape(12.dp))
                            .clickable { showEventsDialog = true }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "EVENTS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireOrange
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(BloodRed, FireOrange)))
                            )
                        }
                    }

                    SidebarButton(emoji = "🏆", selected = showStatsDialog, onClick = { showStatsDialog = true })
                    SidebarButton(emoji = "💬", selected = showContractsDialog, onClick = { showContractsDialog = true })
                }
            }

            // 3. Bottom persistent sub-navigation menu (exactly matches existing active tabs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LobbyBottomNavItem(
                    label = "LOBBY",
                    icon = Icons.Default.Home,
                    selected = activeSubTab == "PLAY",
                    tag = "nav_play_tab",
                    onClick = { activeSubTab = "PLAY" }
                )
                LobbyBottomNavItem(
                    label = "SHOP",
                    icon = Icons.Default.ShoppingCart,
                    selected = activeSubTab == "SKINS",
                    tag = "nav_skins_tab",
                    onClick = { activeSubTab = "SKINS" }
                )
                LobbyBottomNavItem(
                    label = "FIGHTERS",
                    icon = Icons.Default.People,
                    selected = activeSubTab == "HEROES",
                    tag = "nav_heroes_tab",
                    onClick = { activeSubTab = "HEROES" }
                )
                LobbyBottomNavItem(
                    label = "MISSIONS",
                    icon = Icons.Default.Layers,
                    selected = activeSubTab == "BATTLEPASS",
                    tag = "nav_pass_tab",
                    onClick = { activeSubTab = "BATTLEPASS" }
                )
                LobbyBottomNavItem(
                    label = "SETTINGS",
                    icon = Icons.Default.Settings,
                    selected = activeSubTab == "SETT",
                    tag = "nav_sett_tab",
                    onClick = { activeSubTab = "SETT" }
                )
            }
        }

        // --- POPUP OVERLAYS ---
        if (showStatsDialog) {
            StatsOverlay(profile) { showStatsDialog = false }
        }
        if (showContractsDialog) {
            ContractsOverlay { showContractsDialog = false }
        }
        if (showEventsDialog) {
            EventsOverlay { showEventsDialog = false }
        }
        if (showFriendsDialog) {
            FriendsOverlay { showFriendsDialog = false }
        }
        if (showMailDialog) {
            MailOverlay(viewModel) { showMailDialog = false }
        }
    }
}

@Composable
fun SidebarButton(emoji: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) FireOrange else Black60
    val borderCol = if (selected) GoldYellow else BorderWhite10

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 18.sp)
    }
}

@Composable
fun LobbyBottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val tint = if (selected) FireOrange else Slate500
    val indicatorBg = if (selected) FireOrange.copy(alpha = 0.12f) else Color.Transparent

    Column(
        modifier = Modifier
            .testTag(tag)
            .clip(RoundedCornerShape(12.dp))
            .background(indicatorBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            color = tint,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MissionItem(desc: String, rewardedCoins: Int, completed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = desc,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (completed) Slate500 else Slate100
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Coins",
                    tint = GoldYellow,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "+$rewardedCoins Coins",
                    fontSize = 10.sp,
                    color = GoldYellow,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Icon(
            imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = "Completed",
            tint = if (completed) GrassGreen else Slate500,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun BoxScope.StatsOverlay(profile: PlayerProfile, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CarbonGrey)
                .border(2.dp, FireOrange, RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WARRIOR LOGS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = FireOrange,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Slate400)
                }
            }

            // Centered USK FIRE MAX logo in profile/login screen
            UskBrandingLogo(
                modifier = Modifier
                    .size(90.dp)
                    .padding(vertical = 4.dp),
                glow = false
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatRowItem("Matches Played", "${profile.matchesPlayed}", Icons.Default.SportsEsports)
                StatRowItem("USK Wins", "${profile.uskWins}", Icons.Default.EmojiEvents)
                StatRowItem("Total Kills", "${profile.totalKills}", Icons.Default.GpsFixed)
            }
        }
    }
}

@Composable
fun StatRowItem(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, label, tint = FireOrange, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Bold)
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Slate100)
    }
}

@Composable
fun BoxScope.ContractsOverlay(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .height(380.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CarbonGrey)
                .border(2.dp, FireOrange, RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAILY CONTRACTS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = FireOrange,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Slate400)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    MissionItem(desc = "Eliminate 3 opponents in a match", rewardedCoins = 150, completed = false)
                }
                item {
                    MissionItem(desc = "Drive Razor-GT sports car 500m", rewardedCoins = 100, completed = true)
                }
                item {
                    MissionItem(desc = "Secure a 'USK WIN' (Victory)", rewardedCoins = 300, completed = false)
                }
            }
        }
    }
}

@Composable
fun BoxScope.EventsOverlay(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CarbonGrey)
                .border(2.dp, FireOrange, RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EVENTS & REWARDS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = FireOrange,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Slate400)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(BloodRed, FireOrange))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SUMMER PROTOCOL II",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "Secure double match XP and complete special contracts before the countdown expires to unlock the limited edition Legendary Katana skin!",
                fontSize = 11.sp,
                color = Slate400,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

// Sub Tab contents re-styled with Sleek Interface translucents
@Composable
fun CharacterTabContent(
    viewModel: GameEngineViewModel,
    profile: PlayerProfile
) {
    val characters = CharacterType.values()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "USK FIGHTERS SELECTOR",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = FireOrange,
            letterSpacing = 1.sp
        )
        Text(
            text = "Each fighter contains tactical nanotech passive traits which adapt survival rates.",
            fontSize = 10.sp,
            color = Slate400,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(characters) { char ->
                val isSelected = profile.selectedCharacterId == char.id()
                val borderCol = if (isSelected) char.color else BorderWhite10
                val bg = if (isSelected) char.color.copy(alpha = 0.12f) else Black40

                Card(
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectCharacter(char.id()) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(char.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when(char) {
                                    CharacterType.KRONO -> Icons.Default.Shield
                                    CharacterType.KIRA -> Icons.Default.Healing
                                    CharacterType.NEXUS -> Icons.Default.FlashOn
                                    CharacterType.VIXEN -> Icons.Default.DirectionsRun
                                },
                                contentDescription = char.charName,
                                tint = char.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = char.charName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Slate100
                                )
                                if (isSelected) {
                                    Text(
                                        text = "EQUIPPED",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = char.color,
                                        modifier = Modifier
                                            .border(1.dp, char.color, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = char.desc,
                                fontSize = 10.sp,
                                color = Slate400,
                                lineHeight = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "skill",
                                    tint = GoldYellow,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${char.skillName}: ${char.skillDesc}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldYellow
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeaponsTabContent(
    viewModel: GameEngineViewModel,
    profile: PlayerProfile
) {
    val weapons = WeaponType.values()
    val unlockedSkins = remember(profile.unlockedSkins) { profile.unlockedSkins.split(",") }

    val skinShop = listOf(
        SkinNode("classic", "Classic Matte", "Standard industrial black polymer casing.", 0, true),
        SkinNode("vortex", "Vortex Aurora", "Cybernetic shimmering purple neon paint job.", 400, false),
        SkinNode("cyber", "Cyber Dragon", "Highly animated neon red/orange carbon plating.", 800, false)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "USK WEAPONRY LOADOUTS",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = FireOrange,
            letterSpacing = 1.sp
        )
        Text(
            text = "Verify baseline fictional tactical gear statistics and customize active aesthetics.",
            fontSize = 10.sp,
            color = Slate400,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(modifier = Modifier.weight(1f)) {
            // Weapon specs column
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weapons) { weapon ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Black40),
                        border = BorderStroke(1.dp, BorderWhite10),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(weapon.weaponName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = weapon.color)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Damage: ${weapon.damage}", fontSize = 9.sp, color = Slate400)
                                Text("Cap: ${weapon.ammoCapacity}", fontSize = 9.sp, color = Slate400)
                                Text("Range: ${weapon.range.toInt()}m", fontSize = 9.sp, color = Slate400)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Skin Customizer column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("APPLY WEAPON SKIN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Slate100)

                skinShop.forEach { skin ->
                    val isUnlocked = unlockedSkins.contains(skin.id)
                    val isSelected = profile.selectedWeaponSkinId == skin.id
                    val canAfford = profile.coins >= skin.cost

                    val bg = if (isSelected) FireOrange.copy(alpha = 0.15f) else Black40
                    val borderCol = if (isSelected) FireOrange else BorderWhite10

                    Card(
                        colors = CardDefaults.cardColors(containerColor = bg),
                        border = BorderStroke(1.dp, borderCol),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isUnlocked) {
                                    viewModel.selectWeaponSkin(skin.id)
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(skin.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate100)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = FireOrange, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(skin.desc, fontSize = 8.sp, color = Slate400, lineHeight = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))

                            if (!isUnlocked) {
                                Button(
                                    onClick = { viewModel.purchaseSkin(skin.id, skin.cost) },
                                    enabled = canAfford,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldYellow),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.Black, modifier = Modifier.size(11.dp))
                                        Text("Unlock: ${skin.cost}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                    }
                                }
                            } else if (!isSelected) {
                                Text("Equip Skin", fontSize = 9.sp, color = FireOrange, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BattlePassTabContent(
    viewModel: GameEngineViewModel,
    profile: PlayerProfile
) {
    val tierPoints = profile.level * 80 + profile.xp / 10
    val bpTiers = listOf(
        BpTier(1, "Vortex Emblem", 100, true),
        BpTier(3, "500 Gold Coins", 300, false),
        BpTier(5, "Aero-Katana Neon Skin", 600, false),
        BpTier(8, "1000 Gold Coins", 1000, false),
        BpTier(10, "Phoenix Assault Fire Skin", 1500, false)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "USK ELITE SURVIVAL PASS",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = FireOrange,
            letterSpacing = 1.sp
        )
        Text(
            text = "Progress matches to earn XP, level up, and unlock seasonal cosmetics & currencies.",
            fontSize = 10.sp,
            color = Slate400,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Black40),
            border = BorderStroke(1.dp, BorderWhite10),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("SEASON 1: SURVIVAL SPARK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldYellow)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (profile.xp.toFloat() / (profile.level * 500f)).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = FireOrange,
                    trackColor = Color.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Lvl ${profile.level} Pass Progress • Current Points: $tierPoints",
                    fontSize = 10.sp,
                    color = Slate400
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(bpTiers) { tier ->
                val reached = tierPoints >= tier.pointsRequired
                val claimed = profile.unlockedSkins.contains("bp_tier_${tier.id}") || profile.coins > 1200 && tier.id == 3

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (reached) FireOrange.copy(alpha = 0.12f) else Black40)
                        .border(1.dp, if (reached) FireOrange.copy(alpha = 0.4f) else BorderWhite10, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("BP TIER ${tier.id}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (reached) GoldYellow else Slate100)
                        Text(tier.rewardName, fontSize = 10.sp, color = Slate400)
                        Text("Required Points: ${tier.pointsRequired}", fontSize = 8.sp, color = Slate500)
                    }

                    if (claimed) {
                        Text("CLAIMED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                    } else if (reached) {
                        Button(
                            onClick = { viewModel.claimBattlePassReward(if (tier.id == 3) 500 else 1000) },
                            colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("CLAIM", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Slate500, modifier = Modifier.size(14.dp))
                            Text("LOCKED", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    viewModel: GameEngineViewModel,
    profile: PlayerProfile
) {
    val presets = listOf("Low", "Medium", "High", "Ultra")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "GRAPHICS & CALIBRATION",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = FireOrange,
            letterSpacing = 1.sp
        )

        // Graphics Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Black40),
            border = BorderStroke(1.dp, BorderWhite10)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GRAPHICS QUALITY PRESET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FireOrange)
                Text(
                    "High presets automatically enable anti-aliasing and particle emissions. Budget devices should select Medium or Low.",
                    fontSize = 9.sp,
                    color = Slate400,
                    lineHeight = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        val active = profile.graphicsPreset == preset
                        Button(
                            onClick = { viewModel.setGraphicsPreset(preset) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) FireOrange else Color.Transparent
                            ),
                            border = if (!active) BorderStroke(1.dp, BorderWhite10) else null,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                preset,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else Slate400
                            )
                        }
                    }
                }
            }
        }

        // Sensitivity Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Black40),
            border = BorderStroke(1.dp, BorderWhite10)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("LOOK SENSITIVITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FireOrange)
                Text("Calibrates speed multiplier of the virtual thumb stick.", fontSize = 9.sp, color = Slate400)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Slider(
                        value = profile.sensitivity,
                        onValueChange = { viewModel.updateSensitivity(it) },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = FireOrange,
                            activeTrackColor = FireOrange,
                            inactiveTrackColor = Slate800
                        )
                    )
                    Text(
                        text = String.format("%.1fx", profile.sensitivity),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate100
                    )
                }
            }
        }

        // About & Branding Card with USK FIRE MAX logo
        Card(
            colors = CardDefaults.cardColors(containerColor = Black40),
            border = BorderStroke(1.dp, BorderWhite10),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UskBrandingLogo(
                    modifier = Modifier.size(64.dp),
                    glow = false
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "USK FIRE MAX",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Version 2.4.0 (Build 9852)\nEngine Protocol: FireStorm V2\nAuthorized Safe Play Connection Active",
                        fontSize = 9.sp,
                        color = Slate400,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}

fun CharacterType.id(): String = when(this) {
    CharacterType.KRONO -> "krono"
    CharacterType.KIRA -> "kira"
    CharacterType.NEXUS -> "nexus"
    CharacterType.VIXEN -> "vixen"
}

data class SkinNode(
    val id: String,
    val name: String,
    val desc: String,
    val cost: Int,
    val default: Boolean
)

data class BpTier(
    val id: Int,
    val rewardName: String,
    val pointsRequired: Int,
    val claimed: Boolean
)

@Composable
fun BoxScope.FriendsOverlay(onClose: () -> Unit) {
    var friend1Status by remember { mutableStateOf("INVITE") }
    var friend2Status by remember { mutableStateOf("INVITE") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CarbonGrey)
                .border(2.dp, FireOrange, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.People, "Friends", tint = FireOrange, modifier = Modifier.size(18.dp))
                    Text(
                        text = "SQUAD COMRADES",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = FireOrange,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Slate400)
                }
            }

            Text(
                text = "Assemble your squad before dropping into USK Island. Click Invite to signal players.",
                fontSize = 10.sp,
                color = Slate400,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Friend 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("USK_NIGHTMARE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate100)
                        Text("Level 14 • Combat Medic", fontSize = 10.sp, color = Slate400)
                    }
                    Button(
                        onClick = {
                            friend1Status = if (friend1Status == "INVITE") "JOINING..." else "IN SQUAD"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when(friend1Status) {
                                "INVITE" -> FireOrange
                                "JOINING..." -> GoldYellow
                                else -> GrassGreen
                            }
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = friend1Status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (friend1Status == "INVITE") Color.White else Color.Black
                        )
                    }
                }

                // Friend 2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("NOVA_BLAZE_99", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate100)
                        Text("Level 27 • Vanguard Raider", fontSize = 10.sp, color = Slate400)
                    }
                    Button(
                        onClick = {
                            friend2Status = if (friend2Status == "INVITE") "JOINING..." else "IN SQUAD"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when(friend2Status) {
                                "INVITE" -> FireOrange
                                "JOINING..." -> GoldYellow
                                else -> GrassGreen
                            }
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = friend2Status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (friend2Status == "INVITE") Color.White else Color.Black
                        )
                    }
                }

                // Friend 3 (Offline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.15f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SHADOW_WALKER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate500)
                        Text("Level 42 • Sniper • Offline", fontSize = 10.sp, color = Slate500)
                    }
                    Text(
                        text = "OFFLINE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate500,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.MailOverlay(viewModel: GameEngineViewModel, onClose: () -> Unit) {
    var claimedBounty by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CarbonGrey)
                .border(2.dp, FireOrange, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Email, "Mail", tint = FireOrange, modifier = Modifier.size(18.dp))
                    Text(
                        text = "SECURE MAILBOX",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = FireOrange,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Slate400)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mail Item 1 (Claimable reward)
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (claimedBounty) Black40 else FireOrange.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, if (claimedBounty) BorderWhite10 else FireOrange.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "SERVER DEPLOY BOUNTY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (claimedBounty) Slate100 else GoldYellow
                            )
                            Text("1d ago", fontSize = 8.sp, color = Slate500)
                        }
                        Text(
                            "Thank you for deploying USK FIRE! Here is your exclusive launch incentive pack.",
                            fontSize = 9.sp,
                            color = Slate400,
                            lineHeight = 12.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.MonetizationOn, "Coins", tint = GoldYellow, modifier = Modifier.size(12.dp))
                                Text("+250 Gold Coins", fontSize = 10.sp, color = GoldYellow, fontWeight = FontWeight.Bold)
                            }

                            if (!claimedBounty) {
                                Button(
                                    onClick = {
                                        viewModel.claimBattlePassReward(250)
                                        claimedBounty = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("CLAIM", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            } else {
                                Text("CLAIMED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                            }
                        }
                    }
                }

                // Mail Item 2 (Information only)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Black40),
                    border = BorderStroke(1.dp, BorderWhite10),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ANTI-CHEAT V2 ONLINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate100)
                            Text("2d ago", fontSize = 8.sp, color = Slate500)
                        }
                        Text(
                            "The USK Fire Anti-Cheat Protocol V2 is fully engaged. Safe matchmaking is guaranteed across all public lobbies.",
                            fontSize = 9.sp,
                            color = Slate400,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

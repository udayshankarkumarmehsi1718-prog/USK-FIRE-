package com.example.game.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameAction
import com.example.game.engine.GameEngineViewModel
import com.example.game.engine.GameState
import com.example.game.model.Combatant
import com.example.game.model.CombatantState
import com.example.game.model.LootType
import com.example.game.model.WeaponType
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

// Reusable graphics path objects pre-configured to avoid any dynamic memory resets inside the render loop or recompositions
private val STATIC_RIVER_PATH = Path().apply {
    moveTo(0f, 800f)
    quadraticTo(1200f, 1000f, 1500f, 1500f)
    quadraticTo(1800f, 2000f, 3000f, 2200f)
}
private val STATIC_ARMOR_PATH = Path().apply {
    moveTo(-8f, -10f)
    lineTo(8f, -10f)
    lineTo(10f, -2f)
    lineTo(6f, 10f)
    lineTo(-6f, 10f)
    lineTo(-10f, -2f)
    close()
}

@Composable
fun GamePlayScreen(viewModel: GameEngineViewModel) {
    val coroutineScope = rememberCoroutineScope()
    
    // Joystick state variables
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    val maxJoystickRadius = 70f
    
    // Screen dimensions holder
    var screenWidth by remember { mutableStateOf(800f) }
    var screenHeight by remember { mutableStateOf(1600f) }

    // Game stats collected reactively
    val localPlayer = remember(viewModel.combatants) {
        derivedStateOf { viewModel.combatants.find { it.id == viewModel.localPlayerId } }
    }
    
    val safeZoneState by viewModel.safeZone.collectAsState()
    val isDrivingId by viewModel.drivingVehicleId.collectAsState()
    val profileState by viewModel.playerProfile.collectAsState()
    val isLowGraphics = profileState.graphicsPreset == "Low"

    // Scope/Aim State for long-range Sniper Zoom
    var isAimingScope by remember { mutableStateOf(false) }
    val zoomScale by animateFloatAsState(
        targetValue = if (isAimingScope) 0.55f else 1.0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "scope_zoom"
    )

    // Interactive safe-area and height adjustments
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val density = LocalDensity.current
        LaunchedEffect(constraints) {
            screenWidth = constraints.maxWidth.toFloat()
            screenHeight = constraints.maxHeight.toFloat()
        }

        val player = localPlayer.value
        if (player != null) {
            // Apply continuously repeating shoot triggers while holding fire button if needed
            var fireButtonHeld by remember { mutableStateOf(false) }
            LaunchedEffect(fireButtonHeld) {
                while (fireButtonHeld && player.state == CombatantState.ALIVE) {
                    viewModel.handlePlayerAction(GameAction.FIRE)
                    delay(player.activeWeapon.fireRateMs)
                }
            }

            // 1. GAME ARENA CANVAS (THE MATCH MAP)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                // Optional manual weapon fire on map click
                                if (isDrivingId == null) {
                                    val dx = tapOffset.x - (screenWidth / 2)
                                    val dy = tapOffset.y - (screenHeight / 2)
                                    val angle = atan2(dy, dx)
                                    viewModel.handlePlayerRotation(angle)
                                    viewModel.handlePlayerAction(GameAction.FIRE)
                                }
                            }
                        )
                    }
            ) {
                // Center viewport around player
                val cameraX = player.x - (screenWidth / 2) / zoomScale
                val cameraY = player.y - (screenHeight / 2) / zoomScale

                // Draw map layers using zoom scales
                scale(zoomScale, pivot = Offset(screenWidth / 2, screenHeight / 2)) {
                    // Draw grass island grid
                    drawMapGrid(cameraX, cameraY, viewModel.mapWidth, viewModel.mapHeight, isLowGraphics)

                    // Draw procedural winding water river
                    drawMapRiver(STATIC_RIVER_PATH, cameraX, cameraY)

                    // Draw Loot Items on ground
                    viewModel.lootItems.filter { !it.pickedUp }.forEach { loot ->
                        val screenX = loot.x - cameraX
                        val screenY = loot.y - cameraY
                        if (screenX in -100f..(screenWidth / zoomScale + 100f) && screenY in -100f..(screenHeight / zoomScale + 100f)) {
                            drawLootItem(STATIC_ARMOR_PATH, loot.type, screenX, screenY)
                        }
                    }

                    // Draw Drivable Vehicles
                    viewModel.vehicles.forEach { vehicle ->
                        val screenX = vehicle.x - cameraX
                        val screenY = vehicle.y - cameraY
                        if (screenX in -150f..(screenWidth / zoomScale + 150f) && screenY in -150f..(screenHeight / zoomScale + 150f)) {
                            drawVehicleInstance(vehicle.type, screenX, screenY, vehicle.angle, vehicle.health > 0)
                        }
                    }

                    // Draw Static Obstacle buildings (PBR styled walls)
                    viewModel.staticBuildings.forEach { building ->
                        val screenX = building.x - cameraX
                        val screenY = building.y - cameraY
                        drawStaticBuilding(building.name, screenX, screenY, building.width, building.height, building.color)
                    }

                    // Draw bullet laser tracers
                    viewModel.bullets.forEach { bullet ->
                        val bulletScreenX = bullet.x - cameraX
                        val bulletScreenY = bullet.y - cameraY
                        drawLine(
                            color = if (bullet.ownerId == player.id) GoldYellow else FireOrange,
                            start = Offset(bulletScreenX - bullet.dx * 16f, bulletScreenY - bullet.dy * 16f),
                            end = Offset(bulletScreenX, bulletScreenY),
                            strokeWidth = 3f
                        )
                    }

                    // Draw Particles (Bloody sprays, flash sparks)
                    viewModel.particles.forEach { particle ->
                        drawCircle(
                            color = particle.color.copy(alpha = particle.alpha),
                            radius = particle.size,
                            center = Offset(particle.x - cameraX, particle.y - cameraY)
                        )
                    }

                    // Draw Combatants (Players and Bots)
                    viewModel.combatants.forEach { combatant ->
                        if (combatant.state == CombatantState.ALIVE) {
                            val screenX = combatant.x - cameraX
                            val screenY = combatant.y - cameraY

                            // If standing inside any bush, apply stealth cover translucent opacity
                            val bushesToUse = if (isLowGraphics) viewModel.coverBushes.take(25) else viewModel.coverBushes
                            val isHidden = bushesToUse.any { bush ->
                                sqrt((combatant.x - bush.x).pow(2) + (combatant.y - bush.y).pow(2)) < 55f
                            }
                            val alpha = if (isHidden) 0.35f else 1.0f

                            if (screenX in -100f..(screenWidth / zoomScale + 100f) && screenY in -100f..(screenHeight / zoomScale + 100f)) {
                                drawCombatantCircle(combatant, screenX, screenY, alpha)
                            }
                        }
                    }

                    // Draw cover bushes on top for depth layering
                    val bushesToDraw = if (isLowGraphics) viewModel.coverBushes.take(25) else viewModel.coverBushes
                    bushesToDraw.forEach { bush ->
                        val screenX = bush.x - cameraX
                        val screenY = bush.y - cameraY
                        if (screenX in -150f..(screenWidth / zoomScale + 150f) && screenY in -150f..(screenHeight / zoomScale + 150f)) {
                            drawCircle(
                                color = GrassGreen.copy(alpha = 0.75f),
                                radius = 55f,
                                center = Offset(screenX, screenY)
                            )
                            drawCircle(
                                color = GrassGreen.copy(alpha = 0.9f),
                                radius = 35f,
                                center = Offset(screenX, screenY)
                            )
                        }
                    }

                    // Draw SafeZone circular border wall
                    val zoneScreenX = safeZoneState.centerX - cameraX
                    val zoneScreenY = safeZoneState.centerY - cameraY
                    drawCircle(
                        color = BloodRed,
                        radius = safeZoneState.currentRadius,
                        center = Offset(zoneScreenX, zoneScreenY),
                        style = Stroke(width = 4f)
                    )
                    
                    // Draw outer hazard plasma shade
                    drawCircle(
                        color = BloodRed.copy(alpha = 0.12f),
                        radius = safeZoneState.currentRadius + 200f,
                        center = Offset(zoneScreenX, zoneScreenY),
                        style = Stroke(width = 400f)
                    )
                }
            }

            // 2. EXTRA AIM SCOPE OVERLAY (Crosshairs when zoomed!)
            if (isAimingScope) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(200.dp)) {
                        drawCircle(color = Color.Black.copy(alpha = 0.3f), radius = 100.dp.toPx())
                        drawCircle(color = NeonOrange, radius = 100.dp.toPx(), style = Stroke(width = 1.5f))
                        drawLine(color = NeonOrange, start = Offset(0f, size.height/2), end = Offset(size.width, size.height/2), strokeWidth = 1f)
                        drawLine(color = NeonOrange, start = Offset(size.width/2, 0f), end = Offset(size.width/2, size.height), strokeWidth = 1f)
                        drawCircle(color = BloodRed, radius = 4f, center = center)
                    }
                }
            }

            // 3. STATS HEADS-UP DISPLAY (Remaining players, kills, match status)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                // Top-Left: Scrolling Killfeed (limited to 3 entries)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .width(180.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    viewModel.killFeed.takeLast(3).forEach { feed ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = "kill", tint = GoldYellow, modifier = Modifier.size(10.dp))
                            Text(
                                text = "${feed.killerName} ➔ ${feed.victimName}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Top-Center: Alive counters and SafeZone contract state
                val closestBuildingName = remember(player.x, player.y) {
                    val building = viewModel.staticBuildings.minByOrNull { b ->
                        val dx = b.x - player.x
                        val dy = b.y - player.y
                        dx * dx + dy * dy
                    }
                    if (building != null) {
                        val dist = sqrt((building.x - player.x).pow(2) + (building.y - player.y).pow(2))
                        if (dist < 400f) building.name else "Wilderness"
                    } else "Wilderness"
                }

                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📍 USK ISLAND • $closestBuildingName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = FireOrange,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CarbonGrey)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, "Alive", tint = FireOrange, modifier = Modifier.size(14.dp))
                            Text("ALIVE: ${viewModel.combatants.count { it.state == CombatantState.ALIVE }}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GpsFixed, "Kills", tint = BloodRed, modifier = Modifier.size(14.dp))
                            Text("KILLS: ${viewModel.currentMatchKills}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Safe zone shrinking warning text
                    if (safeZoneState.warningTimer > 0) {
                        Text(
                            text = "PLAYZONE CONTRACTS IN ${safeZoneState.warningTimer}s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldYellow,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    } else if (safeZoneState.isShrinking) {
                        Text(
                            text = "⚠ PLAYZONE IS CONTRACTING! ⚠",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BloodRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Top-Right: Circular Mini-Radar HUD Map
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, FireOrange, CircleShape)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val scaleRadar = 0.025f // Shrink 3000px map to fit 90px radar
                        val centerRadar = Offset(size.width / 2, size.height / 2)

                        // 1. Draw Player position in center
                        drawCircle(color = GoldYellow, radius = 3f, center = centerRadar)

                        // 2. Draw relative SafeZone
                        val dxZone = safeZoneState.centerX - player.x
                        val dyZone = safeZoneState.centerY - player.y
                        drawCircle(
                            color = BloodRed,
                            radius = safeZoneState.currentRadius * scaleRadar,
                            center = centerRadar + Offset(dxZone * scaleRadar, dyZone * scaleRadar),
                            style = Stroke(width = 1f)
                        )

                        // 3. Draw visible nearby bots as red dots
                        viewModel.combatants.filter { it.state == CombatantState.ALIVE && it.id != player.id }.forEach { bot ->
                            val dxBot = bot.x - player.x
                            val dyBot = bot.y - player.y
                            val dist = sqrt(dxBot.pow(2) + dyBot.pow(2))
                            if (dist < 400f) {
                                drawCircle(
                                    color = BloodRed,
                                    radius = 2f,
                                    center = centerRadar + Offset(dxBot * scaleRadar, dyBot * scaleRadar)
                                )
                            }
                        }

                        // 4. Draw expanding spatial gunfire ripples on radar!
                        viewModel.activeSounds.forEach { sound ->
                            val dxS = sound.x - player.x
                            val dyS = sound.y - player.y
                            val dist = sqrt(dxS.pow(2) + dyS.pow(2))
                            if (dist < 500f) {
                                val progress = (System.currentTimeMillis() - sound.timestamp).toFloat() / 1500f
                                drawCircle(
                                    color = FireOrange.copy(alpha = 1.0f - progress),
                                    radius = (40f * progress).coerceAtLeast(1f),
                                    center = centerRadar + Offset(dxS * scaleRadar, dyS * scaleRadar),
                                    style = Stroke(width = 1f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. INTERACTIVE VIRTUAL CONTROLS HUD (Joystick and Action Buttons)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Bottom-Left: Virtual Touch Joystick for player steering/car acceleration
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.2f))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { },
                                onDragEnd = {
                                    joystickOffset = Offset.Zero
                                    viewModel.handlePlayerJoystick(0f, 0f, player.isSprinting)
                                },
                                onDragCancel = {
                                    joystickOffset = Offset.Zero
                                    viewModel.handlePlayerJoystick(0f, 0f, player.isSprinting)
                                },
                                onDrag = { change, dragAmount ->
                                    val newOffset = joystickOffset + dragAmount
                                    val distance = sqrt(newOffset.x.pow(2) + newOffset.y.pow(2))
                                    joystickOffset = if (distance > maxJoystickRadius) {
                                        Offset(
                                            (newOffset.x / distance) * maxJoystickRadius,
                                            (newOffset.y / distance) * maxJoystickRadius
                                        )
                                    } else {
                                        newOffset
                                    }

                                    // Direct values to the game engine ViewModel
                                    val isSprint = player.isSprinting
                                    viewModel.handlePlayerJoystick(joystickOffset.x, joystickOffset.y, isSprint)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Outer base circle
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(2.dp, FireOrange.copy(alpha = 0.6f), CircleShape)
                            .background(CarbonGrey.copy(alpha = 0.6f))
                    )

                    // Inner thumb hub
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(joystickOffset.x.toInt(), joystickOffset.y.toInt()) }
                            .size(50.dp)
                            .clip(CircleShape)
                            .shadow(4.dp, CircleShape)
                            .background(FireOrange)
                    )
                }

                // Bottom-Center: Vital Player Indicators (HP, Shield, Sprint mode, Reload, Healing Cast indicators)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(220.dp)
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Healing progress caster
                    if (player.isHealing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("HEALING WITH MEDKIT...", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GoldYellow)
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(4.dp),
                                color = GoldYellow,
                                trackColor = Color.Black
                            )
                        }
                    }

                    // Vital 1: Health Bar (HP)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Favorite, "HP", tint = BloodRed, modifier = Modifier.size(14.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black)
                                .border(0.5.dp, Color.DarkGray, RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(player.health / 100f)
                                    .background(BloodRed)
                            )
                        }
                        Text("${player.health} HP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Vital 2: Armor Shield Bar
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Shield, "Armor", tint = BuildingBlue, modifier = Modifier.size(14.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black)
                                .border(0.5.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(player.shield / 100f)
                                    .background(Color(0xFF00E5FF))
                            )
                        }
                        Text("${player.shield}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // HUD controls triggers for Crouch / Sprint / Active speed indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Sprint Toggle
                        IconButton(
                            onClick = { player.isSprinting = !player.isSprinting },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (player.isSprinting) FireOrange else CarbonGrey)
                        ) {
                            Icon(Icons.Default.DirectionsRun, "Sprint", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Crouch Toggle
                        IconButton(
                            onClick = { viewModel.handlePlayerAction(GameAction.CROUCH) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (player.isCrouching) FireOrange else CarbonGrey)
                        ) {
                            Icon(Icons.Default.ArrowDownward, "Crouch", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Jump visual pop
                        IconButton(
                            onClick = { viewModel.handlePlayerAction(GameAction.JUMP) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CarbonGrey)
                        ) {
                            Icon(Icons.Default.ArrowUpward, "Jump", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Bottom-Right: Fire Buttons, Weapon switching, Reload, Healing inventory trigger
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // DYNAMIC ENTER VEHICLE POPUP: Only shows up when close to a vehicle!
                    val isNearVehicle = viewModel.vehicles.any {
                        sqrt((player.x - it.x).pow(2) + (player.y - it.y).pow(2)) < 75f
                    }

                    if (isNearVehicle || isDrivingId != null) {
                        Button(
                            onClick = { viewModel.handlePlayerAction(GameAction.ENTER_EXIT_VEHICLE) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldYellow),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("vehicle_interact_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.DirectionsCar, "Drive", tint = Color.Black)
                                Text(
                                    text = if (isDrivingId != null) "EXIT VEHICLE" else "DRIVE VEHICLE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // Active Weapon Card with Ammo count
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CarbonGrey)
                            .border(1.dp, FireOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.handlePlayerAction(GameAction.CYCLE_WEAPON) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column {
                            Text(player.activeWeapon.weaponName.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FireOrange)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.CropLandscape, "Ammo", tint = Color.Gray, modifier = Modifier.size(12.dp))
                                Text(
                                    text = if (player.activeWeapon == WeaponType.AERO_KATANA) "∞" else "${player.ammoInClip}/${player.totalAmmo}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                        Icon(Icons.Default.SwapHoriz, "Swap Slots", tint = Color.LightGray)
                    }

                    // Primary Action buttons (Aim scope, Reload, Medkit Heal)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Medkit healing trigger
                        IconButton(
                            onClick = { viewModel.handlePlayerAction(GameAction.HEAL) },
                            enabled = player.medkitsCount > 0,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(CarbonGrey)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Healing, "Heal", tint = if (player.medkitsCount > 0) GoldYellow else Color.DarkGray, modifier = Modifier.size(20.dp))
                                Text("${player.medkitsCount}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Reload Button
                        IconButton(
                            onClick = { viewModel.handlePlayerAction(GameAction.RELOAD) },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(CarbonGrey)
                        ) {
                            Icon(Icons.Default.Refresh, "Reload", tint = Color.White, modifier = Modifier.size(22.dp))
                        }

                        // AIM SCOPE TOGGLE
                        IconButton(
                            onClick = { isAimingScope = !isAimingScope },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isAimingScope) GoldYellow else CarbonGrey)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterCenterFocus,
                                contentDescription = "Scope",
                                tint = if (isAimingScope) Color.Black else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // MASSIVE FIRE BUTTON: Tap/Hold to shoot projectiles
                    if (isDrivingId == null) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(BloodRed)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            fireButtonHeld = true
                                            try {
                                                awaitRelease()
                                            } finally {
                                                fireButtonHeld = false
                                            }
                                        }
                                    )
                                }
                                .testTag("fire_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                                    .background(FireOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "FIRE",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Map custom drawing functions
fun DrawScope.drawMapGrid(cameraX: Float, cameraY: Float, mapWidth: Float, mapHeight: Float, isLowGraphics: Boolean) {
    // Solid green grass background
    drawRect(color = Color(0xFF1B4D22), topLeft = Offset(0f - cameraX, 0f - cameraY), size = Size(mapWidth, mapHeight))

    if (!isLowGraphics) {
        // Draw grid tiles for spatial coordinates positioning
        val step = 150f
        var startX = 0f
        while (startX < mapWidth) {
            drawLine(
                color = Color(0xFF143F1B),
                start = Offset(startX - cameraX, 0f - cameraY),
                end = Offset(startX - cameraX, mapHeight - cameraY),
                strokeWidth = 1.5f
            )
            startX += step
        }

        var startY = 0f
        while (startY < mapHeight) {
            drawLine(
                color = Color(0xFF143F1B),
                start = Offset(0f - cameraX, startY - cameraY),
                end = Offset(mapWidth - cameraX, startY - cameraY),
                strokeWidth = 1.5f
            )
            startY += step
        }
    }

    // Boundary warning border walls
    drawRect(
        color = Color(0xFFFF3D00),
        topLeft = Offset(0f - cameraX, 0f - cameraY),
        size = Size(mapWidth, mapHeight),
        style = Stroke(width = 16f)
    )
}

fun DrawScope.drawMapRiver(path: Path, cameraX: Float, cameraY: Float) {
    // Winding River drawn procedurally (Using the pre-allocated static Path and translate to avoid canvas allocations)
    translate(-cameraX, -cameraY) {
        drawPath(
            path = path,
            color = Color(0xFF0D47A1),
            style = Stroke(width = 120f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }

    // Bridge crossings
    // City bridge crossing
    drawRect(
        color = CarbonGrey,
        topLeft = Offset(1450f - cameraX, 1380f - cameraY),
        size = Size(100f, 180f)
    )
    drawRect(
        color = Color.Black,
        topLeft = Offset(1450f - cameraX, 1380f - cameraY),
        size = Size(100f, 180f),
        style = Stroke(width = 4f)
    )
}

fun DrawScope.drawLootItem(path: Path, type: LootType, screenX: Float, screenY: Float) {
    // Draw neon dynamic halo under items
    drawCircle(
        color = GoldYellow.copy(alpha = 0.25f),
        radius = 16f,
        center = Offset(screenX, screenY)
    )

    when (type) {
        LootType.WEAPON_AR, LootType.WEAPON_SMG, LootType.WEAPON_SG, LootType.WEAPON_SNIPER -> {
            // Draw stylized rifle silhouette
            drawRoundRect(
                color = FireOrange,
                topLeft = Offset(screenX - 10f, screenY - 4f),
                size = Size(20f, 6f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawRect(
                color = Color.Black,
                topLeft = Offset(screenX - 4f, screenY),
                size = Size(6f, 8f)
            )
        }
        LootType.WEAPON_MELEE -> {
            // Aero Katana
            drawLine(
                color = Color.White,
                start = Offset(screenX - 8f, screenY + 8f),
                end = Offset(screenX + 8f, screenY - 8f),
                strokeWidth = 3f
            )
        }
        LootType.MEDKIT -> {
            // Medkit box with red cross
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(screenX - 10f, screenY - 10f),
                size = Size(20f, 20f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Red cross
            drawLine(color = BloodRed, start = Offset(screenX - 6f, screenY), end = Offset(screenX + 6f, screenY), strokeWidth = 3f)
            drawLine(color = BloodRed, start = Offset(screenX, screenY - 6f), end = Offset(screenX, screenY + 6f), strokeWidth = 3f)
        }
        LootType.ARMOR -> {
            // Armor vest outline using pre-allocated Path and translate to avoid run-time allocations
            translate(screenX, screenY) {
                drawPath(path = path, color = Color(0xFF00E5FF))
            }
        }
        LootType.AMMO_BOX -> {
            // Ammo container
            drawRoundRect(
                color = Color(0xFF33691E),
                topLeft = Offset(screenX - 10f, screenY - 6f),
                size = Size(20f, 12f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            // Yellow stripe
            drawRect(
                color = GoldYellow,
                topLeft = Offset(screenX - 2f, screenY - 6f),
                size = Size(4f, 12f)
            )
        }
    }
}

fun DrawScope.drawVehicleInstance(type: com.example.game.model.VehicleType, screenX: Float, screenY: Float, angle: Float, active: Boolean) {
    rotate(degrees = angle * 180f / PI.toFloat(), pivot = Offset(screenX, screenY)) {
        // Car chassis
        drawRoundRect(
            color = if (active) type.color else Color.DarkGray,
            topLeft = Offset(screenX - 25f, screenY - 14f),
            size = Size(50f, 28f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Windshield windshield
        drawRect(
            color = Color.Black.copy(alpha = 0.8f),
            topLeft = Offset(screenX + 2f, screenY - 10f),
            size = Size(10f, 20f)
        )
        // Headlights
        drawCircle(color = Color.White, radius = 3f, center = Offset(screenX + 24f, screenY - 8f))
        drawCircle(color = Color.White, radius = 3f, center = Offset(screenX + 24f, screenY + 8f))

        // Wheels
        drawRoundRect(color = Color.Black, topLeft = Offset(screenX - 20f, screenY - 16f), size = Size(10f, 4f))
        drawRoundRect(color = Color.Black, topLeft = Offset(screenX - 20f, screenY + 12f), size = Size(10f, 4f))
        drawRoundRect(color = Color.Black, topLeft = Offset(screenX + 10f, screenY - 16f), size = Size(10f, 4f))
        drawRoundRect(color = Color.Black, topLeft = Offset(screenX + 10f, screenY + 12f), size = Size(10f, 4f))

        // Spoiler if Sports Car
        if (type == com.example.game.model.VehicleType.RAZOR_GT) {
            drawLine(color = Color.Black, start = Offset(screenX - 26f, screenY - 12f), end = Offset(screenX - 26f, screenY + 12f), strokeWidth = 4f)
        }
    }
}

fun DrawScope.drawStaticBuilding(name: String, screenX: Float, screenY: Float, width: Float, height: Float, color: Color) {
    val left = screenX - width / 2f
    val top = screenY - height / 2f

    // Roof shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.3f),
        topLeft = Offset(left + 8f, top + 8f),
        size = Size(width, height),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Outer brick structure walls
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Inner detail layout roof lines
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.2f),
        topLeft = Offset(left + 10f, top + 10f),
        size = Size(width - 20f, height - 20f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Subtle tactical stripes
    drawLine(
        color = Color.White.copy(alpha = 0.15f),
        start = Offset(left, top),
        end = Offset(left + width, top + height),
        strokeWidth = 2f
    )
}

fun DrawScope.drawCombatantCircle(combatant: Combatant, screenX: Float, screenY: Float, alpha: Float) {
    // Draw aiming whisker holding weapon
    rotate(degrees = combatant.angle * 180f / PI.toFloat(), pivot = Offset(screenX, screenY)) {
        // Weapon barrel extension
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(screenX + 8f, screenY - 3f),
            size = Size(18f, 6f)
        )
        // Red glowing muzzle pip if firing
        if (System.currentTimeMillis() - combatant.lastFiredTime < 80L) {
            drawCircle(
                color = FireOrange,
                radius = 8f,
                center = Offset(screenX + 28f, screenY)
            )
        }
    }

    // Outer glow representing armor shield layer
    if (combatant.shield > 0) {
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.4f * alpha),
            radius = 24f,
            center = Offset(screenX, screenY),
            style = Stroke(width = 2f)
        )
    }

    // Main Player body circle
    drawCircle(
        color = combatant.character.color.copy(alpha = alpha),
        radius = 18f,
        center = Offset(screenX, screenY)
    )

    // Inner head outline
    drawCircle(
        color = Color.Black.copy(alpha = 0.4f * alpha),
        radius = 10f,
        center = Offset(screenX, screenY)
    )

    // Vital: Health & Name tag rendered directly above them
    val textOffset = 28f
    drawLine(
        color = Color.Black,
        start = Offset(screenX - 20f, screenY - textOffset),
        end = Offset(screenX + 20f, screenY - textOffset),
        strokeWidth = 4f
    )
    // Dynamic green health bar
    drawLine(
        color = if (combatant.isBot) BloodRed else GrassGreen,
        start = Offset(screenX - 20f, screenY - textOffset),
        end = Offset(screenX - 20f + (40f * combatant.health / 100f), screenY - textOffset),
        strokeWidth = 4f
    )
}

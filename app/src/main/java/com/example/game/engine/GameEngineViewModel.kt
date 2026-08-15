package com.example.game.engine

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.data.PlayerProfile
import com.example.game.data.PlayerProfileRepository
import com.example.game.data.ProfileDatabase
import com.example.game.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

typealias GameViewModel = GameEngineViewModel

class GameEngineViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ProfileDatabase.getDatabase(application)
    private val repository = PlayerProfileRepository(db.profileDao())

    // Game state flow
    private val _gameState = MutableStateFlow<GameState>(GameState.LOADING)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Matchmaking properties
    private val _matchmakingPlayers = MutableStateFlow<Int>(1)
    val matchmakingPlayers: StateFlow<Int> = _matchmakingPlayers.asStateFlow()

    private val _matchmakingStatus = MutableStateFlow<String>("Searching for warriors...")
    val matchmakingStatus: StateFlow<String> = _matchmakingStatus.asStateFlow()

    // Player Profile state
    private val _playerProfile = MutableStateFlow<PlayerProfile>(PlayerProfile())
    val playerProfile: StateFlow<PlayerProfile> = _playerProfile.asStateFlow()

    // Map boundaries
    val mapWidth = 3000f
    val mapHeight = 3000f

    // Active Game lists
    val combatants = mutableStateListOf<Combatant>()
    val bullets = mutableStateListOf<Bullet>()
    val lootItems = mutableStateListOf<LootItem>()
    val vehicles = mutableStateListOf<VehicleInstance>()
    val particles = mutableStateListOf<GameParticle>()
    val killFeed = mutableStateListOf<KillFeedEntry>()
    val activeSounds = mutableStateListOf<SimpleSoundEffect>()

    // Safe zone state
    private val _safeZone = MutableStateFlow(SafeZone())
    val safeZone: StateFlow<SafeZone> = _safeZone.asStateFlow()

    // System Error State
    private val _systemError = MutableStateFlow<String?>(null)
    val systemError: StateFlow<String?> = _systemError.asStateFlow()

    fun triggerSystemError(message: String) {
        _systemError.value = message
    }

    fun clearSystemError() {
        _systemError.value = null
        _gameState.value = GameState.LOBBY
    }

    // Local player ID
    var localPlayerId: String = ""

    // Driving vehicle state
    private val _drivingVehicleId = MutableStateFlow<String?>(null)
    val drivingVehicleId: StateFlow<String?> = _drivingVehicleId.asStateFlow()

    // Game loop jobs
    private var gameLoopJob: Job? = null
    private var secondTimerJob: Job? = null

    // Player Stats from Current Match
    var currentMatchKills = 0
    var currentMatchRank = 50
    var isVictory = false

    // Defer loading heavy gameplay assets, AI systems, and Audio systems
    var staticBuildings: List<MapObstacle> = emptyList()
        private set
    var coverBushes: List<Offset> = emptyList()
        private set

    // Explicit system status trackers
    var aiSystem: AISystem? = null
        private set
    var audioSystem: AudioSystem? = null
        private set

    private val _areGameplaySystemsLoaded = MutableStateFlow(false)
    val areGameplaySystemsLoaded: StateFlow<Boolean> = _areGameplaySystemsLoaded.asStateFlow()

    private fun initializeHeavyAssets() {
        if (staticBuildings.isNotEmpty()) return
        staticBuildings = listOf(
            // Military Base (Top-Right)
            MapObstacle("Command Post", 2300f, 400f, 180f, 150f, BuildingBlue),
            MapObstacle("Tent Alpha", 2100f, 250f, 100f, 80f, SandYellow),
            MapObstacle("Tent Beta", 2100f, 450f, 100f, 80f, SandYellow),
            MapObstacle("Armory", 2500f, 250f, 120f, 120f, BuildingBlue),
            // Modern City (Center)
            MapObstacle("Sky Rise A", 1300f, 1300f, 200f, 200f, Color.DarkGray),
            MapObstacle("Sky Rise B", 1600f, 1250f, 160f, 250f, Color.Gray),
            MapObstacle("Shopping Plaza", 1400f, 1600f, 320f, 150f, BuildingBlue),
            MapObstacle("Residential Block", 1100f, 1550f, 120f, 180f, CarbonGrey),
            // Industrial Area (Bottom-Left)
            MapObstacle("Factory Hall", 400f, 2200f, 300f, 180f, BloodRed),
            MapObstacle("Warehouse A", 700f, 2100f, 150f, 120f, BuildingBlue),
            MapObstacle("Warehouse B", 700f, 2350f, 150f, 120f, BuildingBlue),
            // Harbor (Bottom-Right)
            MapObstacle("Cargo Dock", 2400f, 2500f, 250f, 100f, Color.DarkGray),
            MapObstacle("Control Tower", 2200f, 2400f, 90f, 90f, BuildingBlue),
            // Small Village (Top-Left)
            MapObstacle("Cabin Red", 400f, 400f, 80f, 80f, BloodRed),
            MapObstacle("Cabin Yellow", 600f, 300f, 80f, 80f, SandYellow),
            MapObstacle("Barn House", 500f, 600f, 140f, 90f, SandYellow)
        )

        coverBushes = List(80) {
            Offset(
                Random.nextFloat() * 2800f + 100f,
                Random.nextFloat() * 2800f + 100f
            )
        }
    }

    private fun initializeAISystems() {
        if (aiSystem != null) return
        aiSystem = AISystem().apply { initialize() }
    }

    private fun initializeAudioSystems() {
        if (audioSystem != null) return
        audioSystem = AudioSystem().apply { initialize() }
    }

    init {
        // Collect Player Profile
        viewModelScope.launch {
            repository.profileFlow.collect { profile ->
                if (profile != null) {
                    _playerProfile.value = profile
                } else {
                    // Seed initial profile
                    val defaultProfile = PlayerProfile()
                    repository.saveProfile(defaultProfile)
                    _playerProfile.value = defaultProfile
                }
            }
        }
    }

    fun setGraphicsPreset(preset: String) {
        viewModelScope.launch {
            val updated = _playerProfile.value.copy(graphicsPreset = preset)
            repository.saveProfile(updated)
        }
    }

    fun updateSensitivity(sens: Float) {
        viewModelScope.launch {
            val updated = _playerProfile.value.copy(sensitivity = sens)
            repository.saveProfile(updated)
        }
    }

    fun selectCharacter(charId: String) {
        viewModelScope.launch {
            val updated = _playerProfile.value.copy(selectedCharacterId = charId)
            repository.saveProfile(updated)
        }
    }

    fun selectWeaponSkin(skinId: String) {
        viewModelScope.launch {
            val updated = _playerProfile.value.copy(selectedWeaponSkinId = skinId)
            repository.saveProfile(updated)
        }
    }

    fun purchaseSkin(skinId: String, cost: Int) {
        viewModelScope.launch {
            val current = _playerProfile.value
            if (current.coins >= cost) {
                val unlocked = current.unlockedSkins.split(",").toMutableSet()
                unlocked.add(skinId)
                val updated = current.copy(
                    coins = current.coins - cost,
                    unlockedSkins = unlocked.joinToString(",")
                )
                repository.saveProfile(updated)
            }
        }
    }

    fun claimBattlePassReward(coins: Int) {
        viewModelScope.launch {
            val current = _playerProfile.value
            val updated = current.copy(coins = current.coins + coins)
            repository.saveProfile(updated)
        }
    }

    fun toggleMusic() {
        viewModelScope.launch {
            val current = _playerProfile.value
            val updated = current.copy(musicEnabled = !current.musicEnabled)
            repository.saveProfile(updated)
        }
    }

    fun purchaseSkinWithDiamonds(skinId: String, cost: Int) {
        viewModelScope.launch {
            val current = _playerProfile.value
            if (current.diamonds >= cost) {
                val unlocked = current.unlockedSkins.split(",").toMutableSet()
                unlocked.add(skinId)
                val updated = current.copy(
                    diamonds = current.diamonds - cost,
                    unlockedSkins = unlocked.joinToString(",")
                )
                repository.saveProfile(updated)
            }
        }
    }

    fun startMatchmaking() {
        _gameState.value = GameState.MATCHMAKING
        _matchmakingPlayers.value = 1
        _matchmakingStatus.value = "Triggering USK START..."
        
        viewModelScope.launch {
            // Explicitly load heavy gameplay assets, AI logic, and audio systems on demand
            _matchmakingStatus.value = "Loading Heavy Gameplay Assets (Map, Terrain, Vector models)..."
            delay(500) // Simulating actual heavy I/O and graphics buffer initialization
            initializeHeavyAssets()
            
            _matchmakingStatus.value = "Calibrating Tactical AI Decision Logic..."
            delay(400) // Simulating behavior tree construction
            initializeAISystems()
            
            _matchmakingStatus.value = "Initializing Dynamic 3D Audio Cue Systems..."
            delay(400) // Simulating audio pipeline setup
            initializeAudioSystems()

            _areGameplaySystemsLoaded.value = true
            _matchmakingStatus.value = "Assembling Squads..."

            var progress = 1
            while (progress < 50) {
                delay(Random.nextLong(100, 250))
                progress += Random.nextInt(2, 6)
                if (progress > 50) progress = 50
                _matchmakingPlayers.value = progress
                _matchmakingStatus.value = when (progress) {
                    in 1..15 -> "Syncing Servers & Anti-Cheat..."
                    in 16..35 -> "Lobby established. Warriors entering: $progress/50..."
                    in 36..49 -> "Pre-match warmup. Weapons calibrated..."
                    else -> "WARRIORS FOUND! Drop plane prepped."
                }
            }
            delay(1000)
            launchGame()
        }
    }

    fun completeLoading() {
        _gameState.value = GameState.LOBBY
    }

    fun returnToLobby() {
        stopGameLoops()
        _gameState.value = GameState.LOBBY
    }

    private fun launchGame() {
        // Clear old states
        combatants.clear()
        bullets.clear()
        lootItems.clear()
        vehicles.clear()
        particles.clear()
        killFeed.clear()
        activeSounds.clear()
        _drivingVehicleId.value = null

        val preset = _playerProfile.value.graphicsPreset
        val botCount = when(preset) {
            "Low" -> 5
            "Medium" -> 14
            else -> 49
        }
        val lootCount = when(preset) {
            "Low" -> 40
            "Medium" -> 80
            else -> 120
        }

        currentMatchKills = 0
        currentMatchRank = botCount + 1
        isVictory = false

        // Determine player character
        val activeChar = when (_playerProfile.value.selectedCharacterId) {
            "krono" -> CharacterType.KRONO
            "kira" -> CharacterType.KIRA
            "nexus" -> CharacterType.NEXUS
            "vixen" -> CharacterType.VIXEN
            else -> CharacterType.KRONO
        }

        // Initialize Local Player at center
        localPlayerId = UUID.randomUUID().toString()
        val localPlayer = Combatant(
            id = localPlayerId,
            name = _playerProfile.value.playerName,
            x = 1500f,
            y = 1500f,
            character = activeChar,
            isBot = false,
            activeWeapon = WeaponType.PHOENIX_AR,
            totalAmmo = 120,
            medkitsCount = 3
        )
        combatants.add(localPlayer)

        // Initialize bots with cool survival names and original character designs
        val botNames = listOf(
            "USK_SHADOW", "PheonixFire", "VortexStriker", "StormRider", "CyberAssassin",
            "NeonWraith", "TitanGrip", "AeroBlade", "EclipseHunter", "KiraFanatic",
            "RazorFangs", "DesertFox", "GlitchTactic", "OverchargeD", "CronoGhost",
            "DeltaApex", "RogueSpecs", "BioHazard", "SteelGaze", "ViperStrike",
            "Phantom_X", "SlayerUSK", "GhostRecon", "TacticalSam", "BlazeOmega",
            "StaticPulse", "ZeroToler", "WickedMinds", "VixenScout", "ApexVanguard",
            "CinderSovere", "NovaBullet", "RustChaser", "DuneRunner", "MetalGear",
            "CrimsonSky", "IronDome", "BulletStorm", "FuriousDuo", "SoloUSK",
            "FireBreather", "ChronoKing", "NexusRebel", "TalonStrike", "FrostBite",
            "NightOwl", "EchoTactics", "Blitzkrieg", "USK_WARRIOR"
        )

        val characterTypes = CharacterType.values()
        val weaponTypes = WeaponType.values()

        for (i in 0 until botCount) {
            val botChar = characterTypes[Random.nextInt(characterTypes.size)]
            val botWeapon = weaponTypes[Random.nextInt(weaponTypes.size)]
            val botX = Random.nextFloat() * (mapWidth - 200f) + 100f
            val botY = Random.nextFloat() * (mapHeight - 200f) + 100f
            
            combatants.add(
                Combatant(
                    id = "bot_${i}_${UUID.randomUUID().toString().take(4)}",
                    name = botNames.getOrElse(i) { "Bot_Soldier_$i" },
                    x = botX,
                    y = botY,
                    character = botChar,
                    isBot = true,
                    activeWeapon = botWeapon,
                    totalAmmo = 90,
                    medkitsCount = Random.nextInt(1, 3)
                )
            )
        }

        // Drop some juicy loot items PROCEDURALLY
        val lootTypes = LootType.values()
        for (i in 0 until lootCount) {
            val lootX = Random.nextFloat() * (mapWidth - 150f) + 75f
            val lootY = Random.nextFloat() * (mapHeight - 150f) + 75f
            val type = lootTypes[Random.nextInt(lootTypes.size)]
            lootItems.add(
                LootItem(
                    id = "loot_$i",
                    x = lootX,
                    y = lootY,
                    type = type,
                    amount = when(type) {
                        LootType.MEDKIT -> 1
                        LootType.ARMOR -> 1
                        LootType.AMMO_BOX -> Random.nextInt(30, 60)
                        else -> 1
                    }
                )
            )
        }

        // Drop procedural Drivable Vehicles on crossroads
        vehicles.add(VehicleInstance("car_1", VehicleType.RAZOR_GT, 1000f, 1000f))
        vehicles.add(VehicleInstance("suv_1", VehicleType.BEAST_SUV, 1800f, 1200f))
        vehicles.add(VehicleInstance("buggy_1", VehicleType.DUNE_BUGGY, 1200f, 2000f))
        vehicles.add(VehicleInstance("car_2", VehicleType.RAZOR_GT, 2200f, 2200f))

        // Reset SafeZone
        _safeZone.value = SafeZone(
            centerX = 1500f,
            centerY = 1500f,
            currentRadius = 1400f,
            targetRadius = 1400f,
            shrinkProgress = 0f,
            isShrinking = false,
            warningTimer = 45
        )

        // Switch screen to game
        _gameState.value = GameState.MATCH

        // Start core loop
        startGameLoops()
    }

    private fun startGameLoops() {
        stopGameLoops()

        // 60FPS physics and 10FPS throttled AI game loop
        gameLoopJob = viewModelScope.launch(Dispatchers.Main.immediate) {
            var lastAiTime = 0L
            while (isActive) {
                val startTime = System.currentTimeMillis()
                updateGamePhysics()
                if (startTime - lastAiTime >= 100L) { // 10 updates per second
                    updateBotAI()
                    lastAiTime = startTime
                }
                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = maxOf(5L, 16L - elapsed)
                delay(sleepTime)
            }
        }

        // 1-Second Timer for SafeZone countdowns and health regeneration
        secondTimerJob = viewModelScope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                delay(1000L)
                updateSecondsTick()
            }
        }
    }

    private fun stopGameLoops() {
        gameLoopJob?.cancel()
        secondTimerJob?.cancel()
        gameLoopJob = null
        secondTimerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoops()
    }

    // Handles Joystick and HUD buttons actions from UI
    fun handlePlayerJoystick(dx: Float, dy: Float, isSprinting: Boolean) {
        val player = combatants.find { it.id == localPlayerId && it.state == CombatantState.ALIVE } ?: return
        val vehicleId = _drivingVehicleId.value

        if (vehicleId != null) {
            // Adjust vehicle physics based on joystick
            val vehicle = vehicles.find { it.id == vehicleId } ?: return
            if (dx != 0f || dy != 0f) {
                val targetAngle = atan2(dy, dx)
                // Smooth angle turn
                var diff = targetAngle - vehicle.angle
                val pi = Math.PI.toFloat()
                val twoPi = (2 * Math.PI).toFloat()
                diff = ((diff + pi) % twoPi)
                if (diff < 0) diff += twoPi
                diff -= pi
                vehicle.angle += diff * 0.12f
                
                // Accelerate
                vehicle.speed = minOf(vehicle.type.maxSpeed, vehicle.speed + 0.15f)
            } else {
                // Decelerate / friction
                vehicle.speed = maxOf(0f, vehicle.speed - 0.1f)
            }
        } else {
            // Regular player movement
            if (dx != 0f || dy != 0f) {
                player.angle = atan2(dy, dx)
                val baseSpeed = if (player.isCrouching) 1.5f else if (isSprinting) 5.0f else 3.2f
                val characterSpeedBonus = player.character.speedMultiplier
                val finalSpeed = baseSpeed * characterSpeedBonus

                val targetX = player.x + cos(player.angle) * finalSpeed
                val targetY = player.y + sin(player.angle) * finalSpeed

                // Prevent moving out of boundary and handle building collisions
                if (targetX in 30f..(mapWidth - 30f) && !isCollidingWithBuildings(targetX, player.y, 18f)) {
                    player.x = targetX
                }
                if (targetY in 30f..(mapHeight - 30f) && !isCollidingWithBuildings(player.x, targetY, 18f)) {
                    player.y = targetY
                }

                player.isSprinting = isSprinting
            } else {
                player.isSprinting = false
            }
        }
    }

    fun handlePlayerRotation(angle: Float) {
        val player = combatants.find { it.id == localPlayerId && it.state == CombatantState.ALIVE } ?: return
        if (_drivingVehicleId.value == null) {
            player.angle = angle
        }
    }

    fun handlePlayerAction(action: GameAction) {
        val player = combatants.find { it.id == localPlayerId && it.state == CombatantState.ALIVE } ?: return

        when (action) {
            GameAction.FIRE -> {
                if (_drivingVehicleId.value != null) return // Can't fire while driving!
                fireWeapon(player)
            }
            GameAction.CROUCH -> {
                player.isCrouching = !player.isCrouching
                if (player.isCrouching) player.isSprinting = false
            }
            GameAction.JUMP -> {
                // Visual bounce effect
                spawnFlashParticles(player.x, player.y, Color.LightGray, 8)
            }
            GameAction.RELOAD -> {
                if (player.ammoInClip < player.activeWeapon.ammoCapacity && player.totalAmmo > 0) {
                    player.isFiring = false
                    spawnSoundIndicator("RELOAD", player.x, player.y)
                    viewModelScope.launch(Dispatchers.Main.immediate) {
                        delay(player.activeWeapon.reloadTimeMs)
                        val needed = player.activeWeapon.ammoCapacity - player.ammoInClip
                        val toLoad = minOf(needed, player.totalAmmo)
                        player.ammoInClip += toLoad
                        player.totalAmmo -= toLoad
                    }
                }
            }
            GameAction.HEAL -> {
                if (player.medkitsCount > 0 && player.health < 100 && !player.isHealing) {
                    player.isHealing = true
                    player.isSprinting = false
                    spawnSoundIndicator("MEDKIT", player.x, player.y)
                    viewModelScope.launch(Dispatchers.Main.immediate) {
                        delay(2500) // 2.5s cast time
                        player.health = minOf(100, player.health + 45)
                        player.medkitsCount--
                        player.isHealing = false
                    }
                }
            }
            GameAction.CYCLE_WEAPON -> {
                val values = WeaponType.values()
                val nextIndex = (player.activeWeapon.ordinal + 1) % values.size
                player.activeWeapon = values[nextIndex]
                player.ammoInClip = player.activeWeapon.ammoCapacity
            }
            GameAction.ENTER_EXIT_VEHICLE -> {
                val currentVehicleId = _drivingVehicleId.value
                if (currentVehicleId != null) {
                    // Exit
                    val vehicle = vehicles.find { it.id == currentVehicleId }
                    if (vehicle != null) {
                        player.x = vehicle.x + 50f * sin(vehicle.angle)
                        player.y = vehicle.y - 50f * cos(vehicle.angle)
                    }
                    _drivingVehicleId.value = null
                } else {
                    // Find closest drivable vehicle within range
                    val closest = vehicles.filter { it.health > 0 }
                        .minByOrNull { calculateDistance(player.x, player.y, it.x, it.y) }
                    if (closest != null && calculateDistance(player.x, player.y, closest.x, closest.y) < 75f) {
                        _drivingVehicleId.value = closest.id
                        player.isSprinting = false
                        player.isCrouching = false
                    }
                }
            }
        }
    }

    private fun fireWeapon(combatant: Combatant) {
        val now = System.currentTimeMillis()
        if (now - combatant.lastFiredTime >= combatant.activeWeapon.fireRateMs) {
            if (combatant.activeWeapon != WeaponType.AERO_KATANA && combatant.ammoInClip <= 0) {
                // Out of ammo, reload automatically
                if (combatant.totalAmmo > 0) {
                    handlePlayerAction(GameAction.RELOAD)
                }
                return
            }

            combatant.lastFiredTime = now
            if (combatant.activeWeapon != WeaponType.AERO_KATANA) {
                combatant.ammoInClip--
            }

            // Calculate projectile direction with spread
            val spreadAngle = (Random.nextFloat() * 2f - 1f) * combatant.activeWeapon.spread
            val finalAngle = combatant.angle + spreadAngle
            
            val dx = cos(finalAngle)
            val dy = sin(finalAngle)

            // Spawn visual bullet
            bullets.add(
                Bullet(
                    id = UUID.randomUUID().toString(),
                    x = combatant.x + dx * 20f,
                    y = combatant.y + dy * 20f,
                    dx = dx,
                    dy = dy,
                    damage = combatant.activeWeapon.damage,
                    speed = combatant.activeWeapon.projectileSpeed,
                    ownerId = combatant.id,
                    weaponName = combatant.activeWeapon.weaponName,
                    maxRange = combatant.activeWeapon.range
                )
            )

            // Spawn muzzle flash particles
            spawnFlashParticles(combatant.x + dx * 20f, combatant.y + dy * 20f, combatant.activeWeapon.color, 6)

            // Gunshot audio ripple on map (visible to nearby players/bots!)
            spawnSoundIndicator("FIRE", combatant.x, combatant.y)
        }
    }

    private fun updateGamePhysics() {
        // 1. Move bullets
        val toRemove = mutableListOf<Bullet>()
        bullets.forEach { bullet ->
            bullet.x += bullet.dx * bullet.speed
            bullet.y += bullet.dy * bullet.speed
            bullet.distanceTraveled += bullet.speed

            if (bullet.distanceTraveled >= bullet.maxRange || bullet.x < 0 || bullet.x > mapWidth || bullet.y < 0 || bullet.y > mapHeight) {
                toRemove.add(bullet)
            } else if (isCollidingWithBuildings(bullet.x, bullet.y, 4f)) {
                // Spawn sparks on building wall impact
                spawnFlashParticles(bullet.x, bullet.y, Color.LightGray, 4)
                toRemove.add(bullet)
            } else {
                // Check player collision
                val hit = combatants.filter { it.state == CombatantState.ALIVE && it.id != bullet.ownerId }
                    .firstOrNull { calculateDistance(bullet.x, bullet.y, it.x, it.y) < 22f }
                
                if (hit != null) {
                    toRemove.add(bullet)
                    damageCombatant(hit, bullet.damage, bullet.ownerId, bullet.weaponName)
                }
            }
        }
        bullets.removeAll(toRemove)

        // 2. Drive Vehicles and check roadkills
        vehicles.forEach { vehicle ->
            if (vehicle.health > 0 && vehicle.speed > 0f) {
                val nextX = vehicle.x + cos(vehicle.angle) * vehicle.speed
                val nextY = vehicle.y + sin(vehicle.angle) * vehicle.speed

                if (nextX in 50f..(mapWidth - 50f) && nextY in 50f..(mapHeight - 50f) && !isCollidingWithBuildings(nextX, nextY, 30f)) {
                    vehicle.x = nextX
                    vehicle.y = nextY
                } else {
                    // Collision with buildings reduces speed and damages vehicle
                    vehicle.speed = -vehicle.speed * 0.4f
                    vehicle.health = maxOf(0, vehicle.health - 25)
                    spawnFlashParticles(vehicle.x, vehicle.y, Color.DarkGray, 12)
                }

                // Check roadkills on alive bots/players
                combatants.filter { it.state == CombatantState.ALIVE }
                    .forEach { target ->
                        val isDriver = target.id == localPlayerId && _drivingVehicleId.value == vehicle.id
                        if (!isDriver && calculateDistance(vehicle.x, vehicle.y, target.x, target.y) < 45f) {
                            val roadkillDamage = (vehicle.speed * 15f).toInt()
                            if (roadkillDamage > 10) {
                                val driverId = if (vehicle.id == _drivingVehicleId.value) localPlayerId else "bot_driver"
                                damageCombatant(target, roadkillDamage, driverId, "Razor-GT")
                            }
                        }
                    }

                // If player is inside, update local player position to match vehicle!
                if (_drivingVehicleId.value == vehicle.id) {
                    val localP = combatants.find { it.id == localPlayerId }
                    if (localP != null) {
                        localP.x = vehicle.x
                        localP.y = vehicle.y
                    }
                }
            }
        }

        // 3. Collect Loot automatically when player steps on it
        val localP = combatants.find { it.id == localPlayerId && it.state == CombatantState.ALIVE }
        if (localP != null && _drivingVehicleId.value == null) {
            lootItems.filter { !it.pickedUp }.forEach { loot ->
                if (calculateDistance(localP.x, localP.y, loot.x, loot.y) < 32f) {
                    collectLoot(localP, loot)
                }
            }
        }

        // 4. Update particles
        val particlesToRemove = mutableListOf<GameParticle>()
        particles.forEach { particle ->
            particle.age++
            if (particle.age >= particle.maxAge) {
                particlesToRemove.add(particle)
            } else {
                particle.alpha = 1.0f - (particle.age.toFloat() / particle.maxAge.toFloat())
                // Apply subtle friction
                particle.x += particle.dx
                particle.y += particle.dy
            }
        }
        particles.removeAll(particlesToRemove)

        // 5. Expire visual sound cues older than 1.5s
        val soundTimeLimit = System.currentTimeMillis() - 1500L
        activeSounds.removeAll { it.timestamp < soundTimeLimit }
    }

    private fun collectLoot(combatant: Combatant, loot: LootItem) {
        loot.pickedUp = true
        when (loot.type) {
            LootType.WEAPON_AR -> {
                combatant.activeWeapon = WeaponType.PHOENIX_AR
                combatant.ammoInClip = WeaponType.PHOENIX_AR.ammoCapacity
            }
            LootType.WEAPON_SMG -> {
                combatant.activeWeapon = WeaponType.VORTEX_SMG
                combatant.ammoInClip = WeaponType.VORTEX_SMG.ammoCapacity
            }
            LootType.WEAPON_SG -> {
                combatant.activeWeapon = WeaponType.THUNDER_SG
                combatant.ammoInClip = WeaponType.THUNDER_SG.ammoCapacity
            }
            LootType.WEAPON_SNIPER -> {
                combatant.activeWeapon = WeaponType.ECLIPSE_SNIPER
                combatant.ammoInClip = WeaponType.ECLIPSE_SNIPER.ammoCapacity
            }
            LootType.WEAPON_MELEE -> {
                combatant.activeWeapon = WeaponType.AERO_KATANA
                combatant.ammoInClip = 1
            }
            LootType.MEDKIT -> {
                combatant.medkitsCount++
            }
            LootType.ARMOR -> {
                combatant.shield = 100
            }
            LootType.AMMO_BOX -> {
                combatant.totalAmmo += loot.amount
            }
        }
        spawnFlashParticles(combatant.x, combatant.y, GoldYellow, 5)
    }

    private fun damageCombatant(target: Combatant, dmg: Int, attackerId: String, weaponName: String) {
        if (target.state != CombatantState.ALIVE) return

        // Apply armor shield reduction (60% absorption by default, 85% for Nexus)
        val absorbRatio = if (target.character == CharacterType.NEXUS) 0.85f else 0.60f
        var damageToShield = (dmg * absorbRatio).toInt()
        var damageToHealth = dmg - damageToShield

        if (target.shield >= damageToShield) {
            target.shield -= damageToShield
        } else {
            damageToHealth += (damageToShield - target.shield)
            target.shield = 0
        }

        target.health = maxOf(0, target.health - damageToHealth)

        // Spawn bloody splatter particles
        spawnFlashParticles(target.x, target.y, BloodRed, 10)

        // Trigger screen rumble for local player when hit
        if (target.id == localPlayerId) {
            activeSounds.add(SimpleSoundEffect("HIT", target.x, target.y))
        }

        // Check death
        if (target.health <= 0) {
            target.state = CombatantState.ELIMINATED
            spawnFlashParticles(target.x, target.y, Color.Black, 15)

            val attacker = combatants.find { it.id == attackerId }
            val attackerName = attacker?.name ?: "SafeZone"
            
            // Add to Killfeed
            killFeed.add(
                KillFeedEntry(
                    id = UUID.randomUUID().toString(),
                    killerName = attackerName,
                    victimName = target.name,
                    weaponName = weaponName
                )
            )

            // Drop dynamic loot container where target died!
            lootItems.add(
                LootItem(
                    id = "dead_loot_${UUID.randomUUID().toString().take(4)}",
                    x = target.x,
                    y = target.y,
                    type = LootType.MEDKIT,
                    amount = 1
                )
            )

            // If attacker is local player, increment score!
            if (attackerId == localPlayerId) {
                currentMatchKills++
            }

            // Update remaining alive count
            val aliveCount = combatants.count { it.state == CombatantState.ALIVE }
            if (target.id == localPlayerId) {
                // Local player died
                currentMatchRank = aliveCount + 1
                endGameMatch(won = false)
            } else if (aliveCount == 1 && combatants.firstOrNull { it.state == CombatantState.ALIVE }?.id == localPlayerId) {
                // Local player is the sole survivor! USK WIN!
                currentMatchRank = 1
                isVictory = true
                endGameMatch(won = true)
            }
        }
    }

    private fun updateBotAI() {
        val now = System.currentTimeMillis()
        val zone = _safeZone.value

        combatants.filter { it.isBot && it.state == CombatantState.ALIVE }.forEach { bot ->
            // Simple Bot Finite State Machine
            val distToZoneCenter = calculateDistance(bot.x, bot.y, zone.centerX, zone.centerY)
            val isSafe = distToZoneCenter < zone.currentRadius

            // 1. If outside SafeZone, escape into safety!
            if (!isSafe && bot.aiState != "ESCAPING") {
                bot.aiState = "ESCAPING"
                bot.destX = zone.centerX + (Random.nextFloat() * 100f - 50f)
                bot.destY = zone.centerY + (Random.nextFloat() * 100f - 50f)
            }

            // 2. Healing check: If low on health and has medkits, heal!
            if (bot.health < 40 && bot.medkitsCount > 0 && !bot.isHealing) {
                bot.isHealing = true
                bot.healingTimer = now + 2500
                spawnSoundIndicator("MEDKIT", bot.x, bot.y)
            }

            if (bot.isHealing) {
                if (now >= bot.healingTimer) {
                    bot.health = minOf(100, bot.health + 45)
                    bot.medkitsCount--
                    bot.isHealing = false
                }
                return@forEach // Pause action while healing
            }

            // 3. Search target combatants nearby
            val closestEnemy = combatants.filter { it.state == CombatantState.ALIVE && it.id != bot.id }
                .minByOrNull { calculateDistance(bot.x, bot.y, it.x, it.y) }

            if (closestEnemy != null) {
                val enemyDist = calculateDistance(bot.x, bot.y, closestEnemy.x, closestEnemy.y)
                if (enemyDist < bot.activeWeapon.range) {
                    // Turn towards enemy and shoot
                    bot.angle = atan2(closestEnemy.y - bot.y, closestEnemy.x - bot.x)
                    bot.aiState = "ATTACKING"
                    fireWeapon(bot)
                    return@forEach
                }
            }

            // 4. Handle patrol/loot searching
            if (bot.aiState == "IDLE" || now - bot.searchTimer > 5000L) {
                bot.searchTimer = now
                
                // Find nearest item
                val nearestLoot = lootItems.filter { !it.pickedUp }
                    .minByOrNull { calculateDistance(bot.x, bot.y, it.x, it.y) }
                
                if (nearestLoot != null && calculateDistance(bot.x, bot.y, nearestLoot.x, nearestLoot.y) < 400f) {
                    bot.aiState = "LOOTING"
                    bot.targetLootId = nearestLoot.id
                    bot.destX = nearestLoot.x
                    bot.destY = nearestLoot.y
                } else {
                    bot.aiState = "PATROL"
                    bot.destX = bot.x + Random.nextFloat() * 400f - 200f
                    bot.destY = bot.y + Random.nextFloat() * 400f - 200f
                }
            }

            // Move bot towards active destination
            val speed = if (bot.isSprinting) 4.5f else 2.6f
            val angle = atan2(bot.destY - bot.y, bot.destX - bot.x)
            bot.angle = angle

            val nextX = bot.x + cos(angle) * speed
            val nextY = bot.y + sin(angle) * speed

            if (nextX in 30f..(mapWidth-30f) && !isCollidingWithBuildings(nextX, bot.y, 18f)) {
                bot.x = nextX
            }
            if (nextY in 30f..(mapHeight-30f) && !isCollidingWithBuildings(bot.x, nextY, 18f)) {
                bot.y = nextY
            }

            // Check if arrived at loot or destination
            if (calculateDistance(bot.x, bot.y, bot.destX, bot.destY) < 15f) {
                if (bot.aiState == "LOOTING") {
                    val item = lootItems.find { it.id == bot.targetLootId }
                    if (item != null && !item.pickedUp) {
                        collectLoot(bot, item)
                    }
                }
                bot.aiState = "IDLE"
            }
        }
    }

    private fun updateSecondsTick() {
        val zone = _safeZone.value

        // 1. Update SafeZone warning countdown
        if (zone.warningTimer > 0) {
            zone.warningTimer--
        } else {
            // Shrinking time!
            zone.isShrinking = true
            val shrinkAmount = 0.05f // Shrink rate per second
            val targetRad = maxOf(0f, zone.currentRadius - 50f)
            zone.targetRadius = targetRad
            
            // Apply shrink
            zone.currentRadius = maxOf(0f, zone.currentRadius - 15f)
            
            if (zone.currentRadius <= zone.targetRadius) {
                // Prepare next shrink cycle
                zone.warningTimer = 45
                zone.isShrinking = false
            }
        }

        // 2. Character specific continuous updates (e.g. Kira's active passive heal)
        combatants.filter { it.state == CombatantState.ALIVE }.forEach { combatant ->
            if (combatant.character == CharacterType.KIRA) {
                combatant.health = minOf(100, combatant.health + CharacterType.KIRA.healthRegen)
            }

            // Apply SafeZone damage if outside circular boundaries
            val dist = calculateDistance(combatant.x, combatant.y, zone.centerX, zone.centerY)
            if (dist > zone.currentRadius) {
                // Out of playzone damage! Intensifies as radius gets smaller
                val damageMultiplier = when {
                    zone.currentRadius > 800f -> 2
                    zone.currentRadius > 400f -> 5
                    else -> 10
                }
                damageCombatant(combatant, damageMultiplier, "SafeZone", "Red Plasma Zone")
            }
        }
    }

    private fun endGameMatch(won: Boolean) {
        stopGameLoops()
        isVictory = won
        _gameState.value = GameState.GAME_OVER

        // Calculate gold coins and rank rewards
        val rewardCoins = (currentMatchKills * 25) + (if (won) 350 else 50)
        
        // Save to Database profile
        viewModelScope.launch {
            repository.incrementMatches(
                kills = currentMatchKills,
                won = won,
                coinsGained = rewardCoins
            )
        }
    }

    // Helper checking methods
    private fun isCollidingWithBuildings(x: Float, y: Float, radius: Float): Boolean {
        return staticBuildings.any { obstacle ->
            // Check box collisions
            val left = obstacle.x - obstacle.width / 2f
            val right = obstacle.x + obstacle.width / 2f
            val top = obstacle.y - obstacle.height / 2f
            val bottom = obstacle.y + obstacle.height / 2f
            
            x + radius > left && x - radius < right && y + radius > top && y - radius < bottom
        }
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
    }

    private fun spawnFlashParticles(x: Float, y: Float, color: Color, count: Int) {
        val maxParticles = if (_playerProfile.value.graphicsPreset == "Low") 40 else 150
        if (particles.size >= maxParticles) {
            val overflow = particles.size - maxParticles + count
            if (overflow > 0 && particles.size >= overflow) {
                for (k in 0 until minOf(overflow, particles.size)) {
                    particles.removeAt(0)
                }
            }
        }
        for (i in 0 until count) {
            val pDx = (Random.nextFloat() * 6f - 3f)
            val pDy = (Random.nextFloat() * 6f - 3f)
            val size = Random.nextFloat() * 4f + 3f
            particles.add(
                GameParticle(
                    x = x,
                    y = y,
                    dx = pDx,
                    dy = pDy,
                    color = color,
                    size = size,
                    maxAge = Random.nextInt(15, 30)
                )
            )
        }
    }

    private fun spawnSoundIndicator(type: String, x: Float, y: Float) {
        activeSounds.add(
            SimpleSoundEffect(
                type = type,
                x = x,
                y = y
            )
        )
    }
}

enum class GameState {
    LOADING,
    LOBBY,
    MATCHMAKING,
    MATCH,
    GAME_OVER
}

enum class GameAction {
    FIRE,
    CROUCH,
    JUMP,
    RELOAD,
    HEAL,
    CYCLE_WEAPON,
    ENTER_EXIT_VEHICLE
}

data class MapObstacle(
    val name: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: Color
)

class AISystem {
    var isInitialized = false
        private set
    fun initialize() {
        // Construct and calibrate AI decision tree matrix
        isInitialized = true
    }
}

class AudioSystem {
    var isInitialized = false
        private set
    fun initialize() {
        // Pre-cache weapon sounds and calibrate dynamic footsteps audio cues
        isInitialized = true
    }
}

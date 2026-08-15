package com.example.game.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

// Fictional weapon definitions
enum class WeaponType(
    val weaponName: String,
    val damage: Int,
    val fireRateMs: Long,
    val ammoCapacity: Int,
    val range: Float,
    val reloadTimeMs: Long,
    val projectileSpeed: Float,
    val spread: Float,
    val color: Color
) {
    PHOENIX_AR("Phoenix-AR", 30, 180L, 30, 600f, 1500L, 16f, 0.08f, FireOrange),
    VORTEX_SMG("Vortex-SMG9", 18, 110L, 40, 350f, 1200L, 18f, 0.15f, NeonOrange),
    THUNDER_SG("Thunder-SG9", 85, 800L, 5, 200f, 2200L, 12f, 0.35f, BloodRed),
    ECLIPSE_SNIPER("Eclipse-Bolt", 110, 1500L, 5, 1200f, 2500L, 25f, 0.01f, GoldYellow),
    AERO_KATANA("Aero-Katana", 45, 300L, 1, 60f, 10L, 5f, 0.0f, Color.White)
}

enum class CharacterType(
    val charName: String,
    val skillName: String,
    val skillDesc: String,
    val speedMultiplier: Float,
    val healthRegen: Int,
    val color: Color,
    val desc: String
) {
    KRONO("USK Ranger", "Ranger Agility", "+15% sprint velocity", 1.15f, 0, NeonOrange, "A specialized elite commando optimized for reconnaissance and high-speed assault."),
    KIRA("Fire Nova", "Nova Blast", "Regens 2 HP per sec", 1.0f, 2, BloodRed, "A fire-born bio-combatant utilizing nanotech regeneration fields."),
    NEXUS("Shadow X", "Dark Overcharge", "Double defensive shield absorption", 1.05f, 0, FireOrange, "An elite stealth infiltrator operating with high-density titanium plates."),
    VIXEN("Vixen Scout", "Jungle Stealth", "+10% speed in visual cover", 1.1f, 0, GoldYellow, "A survival master scouting the dense forest and river banks.")
}

enum class VehicleType(
    val modelName: String,
    val maxSpeed: Float,
    val health: Int,
    val color: Color,
    val size: Float
) {
    RAZOR_GT("Razor-GT", 8.5f, 350, GoldYellow, 45f), // Sports car
    BEAST_SUV("Beast-SUV", 6.0f, 600, BloodRed, 52f),  // Tanky SUV
    DUNE_BUGGY("Dune-Buggy", 7.2f, 250, FireOrange, 40f) // Agile buggy
}

enum class LootType {
    WEAPON_AR,
    WEAPON_SMG,
    WEAPON_SG,
    WEAPON_SNIPER,
    WEAPON_MELEE,
    MEDKIT,
    ARMOR,
    AMMO_BOX
}

data class LootItem(
    val id: String,
    var x: Float,
    var y: Float,
    val type: LootType,
    val amount: Int = 1,
    var pickedUp: Boolean = false
)

data class VehicleInstance(
    val id: String,
    val type: VehicleType,
    var x: Float,
    var y: Float,
    var angle: Float = 0f,
    var speed: Float = 0f,
    var health: Int = type.health,
    var fuel: Float = 100f
)

enum class CombatantState {
    ALIVE,
    KNOCKED,
    ELIMINATED
}

data class Combatant(
    val id: String,
    val name: String,
    var x: Float,
    var y: Float,
    var angle: Float = 0f,
    var health: Int = 100,
    var shield: Int = 50,
    val character: CharacterType,
    var isBot: Boolean = true,
    var activeWeapon: WeaponType = WeaponType.AERO_KATANA,
    var ammoInClip: Int = activeWeapon.ammoCapacity,
    var totalAmmo: Int = 90,
    var isCrouching: Boolean = false,
    var isSprinting: Boolean = false,
    var isFiring: Boolean = false,
    var lastFiredTime: Long = 0L,
    var state: CombatantState = CombatantState.ALIVE,
    // Bot AI variables
    var currentTargetId: String? = null,
    var searchTimer: Long = 0L,
    var aiState: String = "IDLE", // IDLE, LOOTING, CHASING, ESCAPING
    var destX: Float = 0f,
    var destY: Float = 0f,
    var targetLootId: String? = null,
    var hasArmor: Boolean = true,
    var medkitsCount: Int = 1,
    var isHealing: Boolean = false,
    var healingTimer: Long = 0L
)

data class Bullet(
    val id: String,
    var x: Float,
    var y: Float,
    val dx: Float,
    val dy: Float,
    val damage: Int,
    val speed: Float,
    val ownerId: String,
    val weaponName: String,
    var distanceTraveled: Float = 0f,
    val maxRange: Float
)

data class SafeZone(
    var centerX: Float = 1500f,
    var centerY: Float = 1500f,
    var currentRadius: Float = 1200f,
    var targetRadius: Float = 1200f,
    var shrinkProgress: Float = 0f,
    var isShrinking: Boolean = false,
    var warningTimer: Int = 45 // Seconds before shrinking starts
)

data class KillFeedEntry(
    val id: String,
    val killerName: String,
    val victimName: String,
    val weaponName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GameParticle(
    var x: Float,
    var y: Float,
    val dx: Float,
    val dy: Float,
    var alpha: Float = 1.0f,
    val color: Color,
    val size: Float,
    var age: Int = 0,
    val maxAge: Int = 30
)

data class SimpleSoundEffect(
    val type: String, // "FIRE", "RELOAD", "HIT", "MEDKIT", "VEHICLE", "WIN", "ELIMINATED"
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)

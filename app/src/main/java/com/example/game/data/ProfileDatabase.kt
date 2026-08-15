package com.example.game.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val playerName: String = "USK_COMMANDO",
    val level: Int = 1,
    val xp: Int = 0,
    val matchesPlayed: Int = 0,
    val uskWins: Int = 0,
    val totalKills: Int = 0,
    val coins: Int = 1000,
    val diamonds: Int = 180,
    val selectedCharacterId: String = "krono",
    val selectedWeaponSkinId: String = "classic",
    val unlockedSkins: String = "classic,vortex,cyber",
    val graphicsPreset: String = "Low",
    val sensitivity: Float = 1.0f,
    val musicEnabled: Boolean = true
)

@Dao
interface PlayerProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: PlayerProfile)
}

@Database(entities = [PlayerProfile::class], version = 2, exportSchema = false)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): PlayerProfileDao

    companion object {
        @Volatile
        private var INSTANCE: ProfileDatabase? = null

        fun getDatabase(context: Context): ProfileDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ProfileDatabase::class.java,
                    "usk_fire_profile_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class PlayerProfileRepository(private val profileDao: PlayerProfileDao) {
    val profileFlow: Flow<PlayerProfile?> = profileDao.getProfile()

    suspend fun getProfileSync(): PlayerProfile {
        return profileDao.getProfileSync() ?: PlayerProfile()
    }

    suspend fun saveProfile(profile: PlayerProfile) {
        profileDao.saveProfile(profile)
    }
    
    suspend fun incrementMatches(kills: Int, won: Boolean, coinsGained: Int) {
        val current = getProfileSync()
        val newKills = current.totalKills + kills
        val newWins = current.uskWins + (if (won) 1 else 0)
        val newMatches = current.matchesPlayed + 1
        val xpGained = kills * 50 + (if (won) 200 else 50)
        
        var newXp = current.xp + xpGained
        var newLevel = current.level
        val xpNeeded = newLevel * 500
        if (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel += 1
        }
        
        val updated = current.copy(
            level = newLevel,
            xp = newXp,
            matchesPlayed = newMatches,
            uskWins = newWins,
            totalKills = newKills,
            coins = current.coins + coinsGained
        )
        profileDao.saveProfile(updated)
    }
}

package com.example.boomflix.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "continue_watching")
data class ContinueWatchingEntity(
    @PrimaryKey val mediaId: String,
    val type: String,
    val title: String,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val progress: Float = 0f,
    val currentTime: Long = 0L,
    val duration: Long = 0L,
    val serverId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayPosterUrl: String? get() = posterPath?.let {
        if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it"
    }
    val displayBackdropUrl: String? get() = backdropPath?.let {
        if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w780$it"
    }
}

@Entity(tableName = "my_list")
data class MyListEntity(
    @PrimaryKey val mediaId: String,
    val type: String,
    val title: String,
    val posterPath: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface ContinueWatchingDao {
    @Query("SELECT * FROM continue_watching ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ContinueWatchingEntity>>

    @Query("SELECT * FROM continue_watching ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ContinueWatchingEntity>

    @Query("SELECT * FROM continue_watching WHERE mediaId = :mediaId")
    suspend fun getById(mediaId: String): ContinueWatchingEntity?

    @Upsert
    suspend fun upsert(entity: ContinueWatchingEntity)

    @Query("DELETE FROM continue_watching WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: String)

    @Query("DELETE FROM continue_watching")
    suspend fun deleteAll()
}

@Dao
interface MyListDao {
    @Query("SELECT * FROM my_list ORDER BY addedAt DESC")
    suspend fun getAll(): List<MyListEntity>

    @Query("SELECT * FROM my_list WHERE mediaId = :mediaId")
    suspend fun getById(mediaId: String): MyListEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM my_list WHERE mediaId = :mediaId)")
    suspend fun exists(mediaId: String): Boolean

    @Upsert
    suspend fun upsert(entity: MyListEntity)

    @Query("DELETE FROM my_list WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: String)
}

@Database(
    entities = [ContinueWatchingEntity::class, MyListEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BoomflixDatabase : RoomDatabase() {
    abstract fun continueWatchingDao(): ContinueWatchingDao
    abstract fun myListDao(): MyListDao

    companion object {
        @Volatile
        private var INSTANCE: BoomflixDatabase? = null

        fun getInstance(context: android.content.Context): BoomflixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BoomflixDatabase::class.java,
                    "boomflix_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

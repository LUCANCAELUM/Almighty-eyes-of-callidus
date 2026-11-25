package com.example.data.local

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

@Entity(tableName = "telemetry_snapshots")
data class TelemetrySnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val theaterId: String,
    val centerLat: Double,
    val centerLon: Double,
    val aircraftCount: Int,
    val satelliteCount: Int,
    val earthquakeCount: Int,
    val naturalEventCount: Int,
    val kpIndex: Double,
    val recordedAtMs: Long = System.currentTimeMillis()
)

@Dao
interface TelemetrySnapshotDao {
    @Query("SELECT * FROM telemetry_snapshots ORDER BY recordedAtMs DESC LIMIT 25")
    fun observeRecentSnapshots(): Flow<List<TelemetrySnapshotEntity>>

    @Query("SELECT COUNT(*) FROM telemetry_snapshots")
    fun observeSnapshotCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: TelemetrySnapshotEntity)

    @Query("DELETE FROM telemetry_snapshots WHERE id NOT IN (SELECT id FROM telemetry_snapshots ORDER BY recordedAtMs DESC LIMIT 50)")
    suspend fun pruneOldSnapshots()
}

@Database(entities = [TelemetrySnapshotEntity::class], version = 1, exportSchema = false)
abstract class CallidusRoomDatabase : RoomDatabase() {
    abstract fun telemetrySnapshotDao(): TelemetrySnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: CallidusRoomDatabase? = null

        fun getInstance(context: Context): CallidusRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CallidusRoomDatabase::class.java,
                    "callidus_telemetry_sql.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class LocalTelemetrySqlRepository(private val dao: TelemetrySnapshotDao) {
    val recentSnapshots: Flow<List<TelemetrySnapshotEntity>> = dao.observeRecentSnapshots()
    val snapshotCount: Flow<Int> = dao.observeSnapshotCount()

    suspend fun recordTelemetrySnapshot(snapshot: TelemetrySnapshotEntity) {
        dao.insertSnapshot(snapshot)
        dao.pruneOldSnapshots()
    }
}

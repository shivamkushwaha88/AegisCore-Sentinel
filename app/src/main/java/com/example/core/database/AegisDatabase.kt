package com.example.core.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRunDao {
    @Query("SELECT * FROM scan_runs ORDER BY startedAt DESC LIMIT 1")
    fun getLatestScanRun(): Flow<ScanRunEntity?>

    @Query("SELECT * FROM scan_runs ORDER BY startedAt DESC")
    fun getAllScanRuns(): Flow<List<ScanRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanRun(scanRun: ScanRunEntity)

    @Query("DELETE FROM scan_runs")
    suspend fun deleteAll()
}

@Dao
interface CheckResultDao {
    @Query("SELECT * FROM check_results WHERE scanRunId = :scanRunId")
    fun getResultsForScan(scanRunId: String): Flow<List<CheckResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<CheckResultEntity>)
}

@Dao
interface AppSnapshotDao {
    @Query("SELECT * FROM app_snapshots WHERE scanRunId = :scanRunId ORDER BY displayName ASC")
    fun getSnapshotsForScan(scanRunId: String): Flow<List<AppSnapshotEntity>>

    @Query("SELECT * FROM app_snapshots ORDER BY lastUpdateTime DESC LIMIT 100")
    fun getRecentSnapshots(): Flow<List<AppSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<AppSnapshotEntity>)
}

@Dao
interface FindingDao {
    @Query("SELECT * FROM findings ORDER BY observedAt DESC")
    fun getAllFindings(): Flow<List<FindingEntity>>

    @Query("SELECT * FROM findings WHERE scanRunId = :scanRunId ORDER BY observedAt DESC")
    fun getFindingsForScan(scanRunId: String): Flow<List<FindingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(findings: List<FindingEntity>)

    @Query("UPDATE findings SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM findings")
    suspend fun deleteAll()
}

@Dao
interface TrustExceptionDao {
    @Query("SELECT * FROM trust_exceptions ORDER BY createdAt DESC")
    fun getAllExceptions(): Flow<List<TrustExceptionEntity>>

    @Query("SELECT * FROM trust_exceptions WHERE enabled = 1")
    fun getActiveExceptions(): Flow<List<TrustExceptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertException(exception: TrustExceptionEntity)

    @Query("UPDATE trust_exceptions SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM trust_exceptions WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [
        ScanRunEntity::class,
        CheckResultEntity::class,
        AppSnapshotEntity::class,
        FindingEntity::class,
        TrustExceptionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AegisTypeConverters::class)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun scanRunDao(): ScanRunDao
    abstract fun checkResultDao(): CheckResultDao
    abstract fun appSnapshotDao(): AppSnapshotDao
    abstract fun findingDao(): FindingDao
    abstract fun trustExceptionDao(): TrustExceptionDao

    companion object {
        @Volatile
        private var INSTANCE: AegisDatabase? = null

        fun getInstance(context: Context): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AegisDatabase::class.java,
                    "aegiscore_sentinel.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

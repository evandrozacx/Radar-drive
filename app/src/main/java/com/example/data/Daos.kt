package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT * FROM rides WHERE dayKey = :dayKey ORDER BY createdAt DESC")
    fun observeRidesForDay(dayKey: String): Flow<List<RideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity): Long

    @Query("DELETE FROM rides WHERE id = :id")
    suspend fun deleteRideById(id: Long)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE dayKey = :dayKey ORDER BY createdAt DESC")
    fun observeExpensesForDay(dayKey: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)
}

@Dao
interface DailyShiftDao {
    @Query("SELECT * FROM daily_shifts WHERE dayKey = :dayKey LIMIT 1")
    fun observeShift(dayKey: String): Flow<DailyShiftEntity?>

    @Query("SELECT * FROM daily_shifts WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getShiftOnce(dayKey: String): DailyShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShift(shift: DailyShiftEntity)
}

@Dao
interface DriverSettingsDao {
    @Query("SELECT * FROM driver_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<DriverSettingsEntity?>

    @Query("SELECT * FROM driver_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): DriverSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: DriverSettingsEntity)

    @Query("UPDATE driver_settings SET voiceEnabled = :enabled WHERE id = 1")
    suspend fun updateVoiceEnabled(enabled: Boolean)
}

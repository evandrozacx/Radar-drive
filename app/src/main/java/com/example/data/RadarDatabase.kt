package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RideEntity::class,
        ExpenseEntity::class,
        DailyShiftEntity::class,
        DriverSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RadarDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun dailyShiftDao(): DailyShiftDao
    abstract fun driverSettingsDao(): DriverSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: RadarDatabase? = null

        fun getInstance(context: Context): RadarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RadarDatabase::class.java,
                    "radar_driver.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

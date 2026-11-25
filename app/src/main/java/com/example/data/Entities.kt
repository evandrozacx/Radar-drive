package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dayKey: String,
    val platform: String,
    val fare: Double,
    val totalKm: Double,
    val totalMinutes: Int,
    val pickupKm: Double? = null,
    val tripKm: Double? = null,
    val ratePerKm: Double,
    val ratePerHour: Double,
    val estimatedNetProfit: Double,
    val verdict: String,
    val isManual: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

object ExpenseType {
    const val COMBUSTIVEL = "COMBUSTIVEL"
    const val PEDAGIO = "PEDAGIO"
    const val EXTRA = "EXTRA"
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dayKey: String,
    val type: String,
    val description: String,
    val amount: Double,
    val liters: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_shifts")
data class DailyShiftEntity(
    @PrimaryKey val dayKey: String,
    val odometerStartKm: Double? = null,
    val odometerEndKm: Double? = null,
    val workedMinutesOverride: Int? = null,
    val shiftStartedAt: Long? = null,
    val shiftEndedAt: Long? = null,
    val isShiftActive: Boolean = false
)

@Entity(tableName = "driver_settings")
data class DriverSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val fuelPricePerLiter: Double = 5.89,
    val fuelConsumptionKmPerL: Double = 11.5,
    val maintenancePerKm: Double = 0.25,
    val dailyFixedCost: Double = 45.00,
    val targetRatePerKm: Double = 2.00,
    val minRatePerKm: Double = 1.40,
    val targetRatePerHour: Double = 60.00,
    val minRatePerHour: Double = 40.00,
    val dailyGoalNet: Double = 250.00,
    val voiceEnabled: Boolean = true,
    val overlayAutoHideSeconds: Int = 18
)

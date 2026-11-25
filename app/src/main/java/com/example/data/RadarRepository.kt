package com.example.data

import com.example.domain.EvaluatedRide
import com.example.domain.RideEvaluator
import com.example.domain.RideOffer
import com.example.domain.RidePlatform
import com.example.domain.currentDayKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

data class DailySummary(
    val dayKey: String = currentDayKey(),
    val ridesCount: Int = 0,
    val grossTotal: Double = 0.0,
    val grossUber: Double = 0.0,
    val gross99: Double = 0.0,
    val grossManual: Double = 0.0,
    val ridesTotalKm: Double = 0.0,
    val odometerKm: Double? = null,
    val effectiveKm: Double = 0.0,
    val workedMinutes: Int = 0,
    val workedHours: Double = 0.0,
    val avgRatePerKm: Double = 0.0,
    val avgRatePerHour: Double = 0.0,
    val fuelCost: Double = 0.0,
    val fuelIsActual: Boolean = false,
    val fuelLitersLogged: Double = 0.0,
    val maintenanceCost: Double = 0.0,
    val tollsAndExtrasCost: Double = 0.0,
    val fixedDailyCost: Double = 0.0,
    val netBalance: Double = 0.0,
    val dailyGoalNet: Double = 250.0
)

class RadarRepository(private val db: RadarDatabase) {
    private val rideDao = db.rideDao()
    private val expenseDao = db.expenseDao()
    private val shiftDao = db.dailyShiftDao()
    private val settingsDao = db.driverSettingsDao()

    val settingsFlow: Flow<DriverSettingsEntity> = settingsDao.observeSettings()
        .map { it ?: DriverSettingsEntity() }

    suspend fun ensureDefaultSettings(): DriverSettingsEntity {
        val existing = settingsDao.getSettingsOnce()
        if (existing != null) return existing
        val defaults = DriverSettingsEntity()
        settingsDao.upsertSettings(defaults)
        return defaults
    }

    suspend fun getSettingsOnce(): DriverSettingsEntity {
        return ensureDefaultSettings()
    }

    suspend fun saveSettings(settings: DriverSettingsEntity) {
        settingsDao.upsertSettings(settings.copy(id = 1))
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        ensureDefaultSettings()
        settingsDao.updateVoiceEnabled(enabled)
    }

    fun observeRides(dayKey: String = currentDayKey()): Flow<List<RideEntity>> =
        rideDao.observeRidesForDay(dayKey)

    fun observeExpenses(dayKey: String = currentDayKey()): Flow<List<ExpenseEntity>> =
        expenseDao.observeExpensesForDay(dayKey)

    fun observeShift(dayKey: String = currentDayKey()): Flow<DailyShiftEntity> =
        shiftDao.observeShift(dayKey).map { it ?: DailyShiftEntity(dayKey = dayKey) }

    fun observeDailySummary(dayKey: String = currentDayKey()): Flow<DailySummary> {
        return combine(
            observeRides(dayKey),
            observeExpenses(dayKey),
            observeShift(dayKey),
            settingsFlow
        ) { rides, expenses, shift, settings ->
            calculateSummary(dayKey, rides, expenses, shift, settings)
        }
    }

    suspend fun acceptEvaluatedRide(
        evaluated: EvaluatedRide,
        dayKey: String = currentDayKey()
    ): Long {
        val offer = evaluated.offer
        val entity = RideEntity(
            dayKey = dayKey,
            platform = offer.platform.label,
            fare = offer.fare,
            totalKm = offer.totalKm,
            totalMinutes = offer.totalMinutes,
            pickupKm = offer.pickupKm,
            tripKm = offer.tripKm,
            ratePerKm = evaluated.ratePerKm,
            ratePerHour = evaluated.ratePerHour,
            estimatedNetProfit = evaluated.estimatedNetProfit,
            verdict = evaluated.verdict.name,
            isManual = offer.platform == RidePlatform.MANUAL
        )
        return rideDao.insertRide(entity)
    }

    suspend fun addManualRide(
        platform: RidePlatform,
        fare: Double,
        totalKm: Double,
        totalMinutes: Int,
        dayKey: String = currentDayKey()
    ): Long {
        val settings = ensureDefaultSettings()
        val offer = RideOffer(
            platform = platform,
            fare = fare,
            totalKm = totalKm,
            totalMinutes = totalMinutes
        )
        val evaluated = RideEvaluator.evaluate(offer, settings)
        val entity = RideEntity(
            dayKey = dayKey,
            platform = platform.label,
            fare = fare,
            totalKm = totalKm,
            totalMinutes = totalMinutes,
            pickupKm = null,
            tripKm = totalKm,
            ratePerKm = evaluated.ratePerKm,
            ratePerHour = evaluated.ratePerHour,
            estimatedNetProfit = evaluated.estimatedNetProfit,
            verdict = evaluated.verdict.name,
            isManual = true
        )
        return rideDao.insertRide(entity)
    }

    suspend fun deleteRide(id: Long) = rideDao.deleteRideById(id)

    suspend fun addFuelExpense(
        amountReais: Double?,
        liters: Double?,
        description: String = "Abastecimento",
        dayKey: String = currentDayKey()
    ): Long {
        val settings = ensureDefaultSettings()
        val price = settings.fuelPricePerLiter.coerceAtLeast(0.01)
        val resolvedAmount: Double
        val resolvedLiters: Double?
        when {
            amountReais != null && amountReais > 0.0 -> {
                resolvedAmount = amountReais
                resolvedLiters = if (liters != null && liters > 0.0) liters else amountReais / price
            }
            liters != null && liters > 0.0 -> {
                resolvedLiters = liters
                resolvedAmount = liters * price
            }
            else -> return 0L
        }
        return expenseDao.insertExpense(
            ExpenseEntity(
                dayKey = dayKey,
                type = ExpenseType.COMBUSTIVEL,
                description = description.ifBlank { "Abastecimento" },
                amount = resolvedAmount,
                liters = resolvedLiters
            )
        )
    }

    suspend fun addTollOrExtraExpense(
        type: String,
        description: String,
        amount: Double,
        dayKey: String = currentDayKey()
    ): Long {
        if (amount <= 0.0) return 0L
        return expenseDao.insertExpense(
            ExpenseEntity(
                dayKey = dayKey,
                type = type,
                description = description.ifBlank {
                    if (type == ExpenseType.PEDAGIO) "Pedágio" else "Gasto extra"
                },
                amount = amount
            )
        )
    }

    suspend fun deleteExpense(id: Long) = expenseDao.deleteExpenseById(id)

    suspend fun updateOdometer(
        startKm: Double?,
        endKm: Double?,
        dayKey: String = currentDayKey()
    ) {
        val current = shiftDao.getShiftOnce(dayKey) ?: DailyShiftEntity(dayKey = dayKey)
        shiftDao.upsertShift(
            current.copy(
                odometerStartKm = startKm,
                odometerEndKm = endKm
            )
        )
    }

    suspend fun updateWorkedMinutesOverride(
        minutes: Int?,
        dayKey: String = currentDayKey()
    ) {
        val current = shiftDao.getShiftOnce(dayKey) ?: DailyShiftEntity(dayKey = dayKey)
        shiftDao.upsertShift(current.copy(workedMinutesOverride = minutes))
    }

    suspend fun toggleShift(dayKey: String = currentDayKey(), now: Long = System.currentTimeMillis()) {
        val current = shiftDao.getShiftOnce(dayKey) ?: DailyShiftEntity(dayKey = dayKey)
        val updated = if (current.isShiftActive) {
            current.copy(
                isShiftActive = false,
                shiftEndedAt = now
            )
        } else {
            current.copy(
                isShiftActive = true,
                shiftStartedAt = current.shiftStartedAt ?: now,
                shiftEndedAt = null
            )
        }
        shiftDao.upsertShift(updated)
    }

    companion object {
        fun calculateSummary(
            dayKey: String,
            rides: List<RideEntity>,
            expenses: List<ExpenseEntity>,
            shift: DailyShiftEntity,
            settings: DriverSettingsEntity,
            nowMillis: Long = System.currentTimeMillis()
        ): DailySummary {
            val grossUber = rides.filter { it.platform.equals(RidePlatform.UBER.label, true) }.sumOf { it.fare }
            val gross99 = rides.filter { it.platform.equals(RidePlatform.APP_99.label, true) }.sumOf { it.fare }
            val grossManual = rides.filter {
                !it.platform.equals(RidePlatform.UBER.label, true) &&
                    !it.platform.equals(RidePlatform.APP_99.label, true)
            }.sumOf { it.fare }
            val grossTotal = rides.sumOf { it.fare }

            val ridesTotalKm = rides.sumOf { it.totalKm }
            val odometerKm = if (
                shift.odometerStartKm != null &&
                shift.odometerEndKm != null &&
                shift.odometerEndKm >= shift.odometerStartKm
            ) {
                shift.odometerEndKm - shift.odometerStartKm
            } else {
                null
            }
            val effectiveKm = odometerKm ?: ridesTotalKm

            val ridesMinutes = rides.sumOf { it.totalMinutes }
            val shiftElapsedMinutes = when {
                shift.workedMinutesOverride != null -> shift.workedMinutesOverride
                shift.isShiftActive && shift.shiftStartedAt != null -> {
                    val elapsed = ((nowMillis - shift.shiftStartedAt) / 60_000L).toInt().coerceAtLeast(0)
                    maxOf(elapsed, ridesMinutes)
                }
                shift.shiftStartedAt != null && shift.shiftEndedAt != null && shift.shiftEndedAt >= shift.shiftStartedAt -> {
                    val elapsed = ((shift.shiftEndedAt - shift.shiftStartedAt) / 60_000L).toInt().coerceAtLeast(0)
                    maxOf(elapsed, ridesMinutes)
                }
                else -> ridesMinutes
            }

            val workedHours = shiftElapsedMinutes / 60.0
            val avgRatePerKm = if (effectiveKm > 0.0) grossTotal / effectiveKm else 0.0
            val avgRatePerHour = if (workedHours > 0.0) grossTotal / workedHours else 0.0

            val fuelExpenses = expenses.filter { it.type == ExpenseType.COMBUSTIVEL }
            val loggedFuelAmount = fuelExpenses.sumOf { it.amount }
            val loggedFuelLiters = fuelExpenses.sumOf { it.liters ?: 0.0 }
            val fuelIsActual = fuelExpenses.isNotEmpty()
            val fuelCost = if (fuelIsActual) {
                loggedFuelAmount
            } else {
                if (settings.fuelConsumptionKmPerL > 0.0) {
                    (effectiveKm / settings.fuelConsumptionKmPerL) * settings.fuelPricePerLiter
                } else 0.0
            }

            val maintenanceCost = effectiveKm * settings.maintenancePerKm
            val tollsAndExtras = expenses
                .filter { it.type == ExpenseType.PEDAGIO || it.type == ExpenseType.EXTRA }
                .sumOf { it.amount }

            val fixedCost = settings.dailyFixedCost
            val netBalance = grossTotal - fuelCost - maintenanceCost - tollsAndExtras - fixedCost

            return DailySummary(
                dayKey = dayKey,
                ridesCount = rides.size,
                grossTotal = (grossTotal * 100.0).roundToInt() / 100.0,
                grossUber = grossUber,
                gross99 = gross99,
                grossManual = grossManual,
                ridesTotalKm = ridesTotalKm,
                odometerKm = odometerKm,
                effectiveKm = effectiveKm,
                workedMinutes = shiftElapsedMinutes,
                workedHours = workedHours,
                avgRatePerKm = avgRatePerKm,
                avgRatePerHour = avgRatePerHour,
                fuelCost = fuelCost,
                fuelIsActual = fuelIsActual,
                fuelLitersLogged = loggedFuelLiters,
                maintenanceCost = maintenanceCost,
                tollsAndExtrasCost = tollsAndExtras,
                fixedDailyCost = fixedCost,
                netBalance = netBalance,
                dailyGoalNet = settings.dailyGoalNet
            )
        }
    }
}

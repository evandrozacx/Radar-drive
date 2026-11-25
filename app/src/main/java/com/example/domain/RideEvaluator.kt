package com.example.domain

import com.example.data.DriverSettingsEntity

enum class SemaphoreVerdict(
    val label: String,
    val speechAction: String
) {
    VERDE("Aceitar", "Aceitar corrida"),
    AMARELO("Avaliar", "Avaliar corrida"),
    VERMELHO("Recusar", "Recusar corrida")
}

enum class RidePlatform(val label: String) {
    UBER("Uber"),
    APP_99("99"),
    MANUAL("Manual");

    companion object {
        fun fromLabel(raw: String): RidePlatform = entries.firstOrNull {
            it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
        } ?: UBER
    }
}

data class RideOffer(
    val platform: RidePlatform,
    val fare: Double,
    val totalKm: Double,
    val totalMinutes: Int,
    val pickupKm: Double? = null,
    val tripKm: Double? = null,
    val pickupMinutes: Int? = null,
    val tripMinutes: Int? = null,
    val isTestSimulation: Boolean = false,
    val detectedAt: Long = System.currentTimeMillis()
)

data class EvaluatedRide(
    val offer: RideOffer,
    val ratePerKm: Double,
    val ratePerHour: Double,
    val estimatedFuelCost: Double,
    val estimatedMaintenanceCost: Double,
    val estimatedNetProfit: Double,
    val verdict: SemaphoreVerdict,
    val reasonText: String
) {
    fun buildSpeechPhrase(): String {
        return "${verdict.speechAction}, ${formatSpeechAmount(offer.fare)}"
    }
}

object RideEvaluator {
    fun evaluate(offer: RideOffer, settings: DriverSettingsEntity): EvaluatedRide {
        val safeKm = offer.totalKm.coerceAtLeast(0.01)
        val safeMinutes = offer.totalMinutes.coerceAtLeast(1)

        val ratePerKm = offer.fare / safeKm
        val ratePerHour = offer.fare / (safeMinutes / 60.0)

        val fuelCost = if (settings.fuelConsumptionKmPerL > 0.0) {
            (offer.totalKm / settings.fuelConsumptionKmPerL) * settings.fuelPricePerLiter
        } else {
            0.0
        }
        val maintenanceCost = offer.totalKm * settings.maintenancePerKm
        val netProfit = offer.fare - fuelCost - maintenanceCost

        val hitsTargetKm = ratePerKm >= settings.targetRatePerKm
        val hitsTargetHour = ratePerHour >= settings.targetRatePerHour
        val aboveMinKm = ratePerKm >= settings.minRatePerKm
        val aboveMinHour = ratePerHour >= settings.minRatePerHour

        val verdict = when {
            hitsTargetKm && hitsTargetHour -> SemaphoreVerdict.VERDE
            (aboveMinKm && aboveMinHour) || hitsTargetKm || hitsTargetHour -> SemaphoreVerdict.AMARELO
            else -> SemaphoreVerdict.VERMELHO
        }

        val reasonText = when (verdict) {
            SemaphoreVerdict.VERDE ->
                "Supera as metas de ${formatRatePerKm(settings.targetRatePerKm)} e ${formatRatePerHour(settings.targetRatePerHour)}"
            SemaphoreVerdict.AMARELO -> when {
                hitsTargetKm && !aboveMinHour -> "R$/km ótimo, mas R$/hora abaixo do mínimo"
                hitsTargetHour && !aboveMinKm -> "R$/hora ótimo, mas R$/km abaixo do mínimo"
                hitsTargetKm -> "Atinge meta de R$/km; R$/hora na tolerância"
                hitsTargetHour -> "Atinge meta de R$/hora; R$/km na tolerância"
                else -> "Dentro da margem de tolerância configurada"
            }
            SemaphoreVerdict.VERMELHO ->
                "Abaixo do mínimo (${formatRatePerKm(settings.minRatePerKm)} ou ${formatRatePerHour(settings.minRatePerHour)})"
        }

        return EvaluatedRide(
            offer = offer,
            ratePerKm = ratePerKm,
            ratePerHour = ratePerHour,
            estimatedFuelCost = fuelCost,
            estimatedMaintenanceCost = maintenanceCost,
            estimatedNetProfit = netProfit,
            verdict = verdict,
            reasonText = reasonText
        )
    }
}

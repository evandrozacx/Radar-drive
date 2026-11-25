package com.example.domain

import kotlin.math.roundToInt

/**
 * Analisa blocos de texto extraídos da árvore de acessibilidade da Uber e da 99
 * para identificar chamadas de corrida com Valor (R$), KM total e Tempo total.
 */
object RideOfferParser {

    private val uberPackages = setOf("com.ubercab.driver")
    private val app99Packages = setOf(
        "com.taxis99",
        "com.ninetynine.driver",
        "com.app99.driver",
        "com.didiglobal.driver"
    )

    // Remove expressões de taxa (ex: "Est. de R$1,65/km" ou "R$ 45,00/h") antes de extrair o valor da corrida
    private val ratePerUnitRegex = Regex(
        """R\$\s*\d+(?:[.,]\d+)?\s*/\s*(?:km|h|hora|min)""",
        RegexOption.IGNORE_CASE
    )
    private val fareRegex = Regex("""R\$\s*(\d{1,4}(?:[.,]\d{3})*[.,]\d{2}|\d{1,4})""")
    private val kmRegex = Regex("""(\d+(?:[.,]\d+)?)\s*km\b""", RegexOption.IGNORE_CASE)
    private val metersRegex = Regex("""(\d+(?:[.,]\d+)?)\s*m\b(?!in)""", RegexOption.IGNORE_CASE)
    private val hoursAndMinutesRegex = Regex(
        """(\d+)\s*h(?:oras?)?\s*(?:e\s*)?(\d+)?\s*(?:min(?:utos?)?|m\b)?""",
        RegexOption.IGNORE_CASE
    )
    private val lessThanOneMinRegex = Regex("""<\s*1\s*min(?:utos?)?\b""", RegexOption.IGNORE_CASE)
    private val minutesRegex = Regex("""(\d+)\s*min(?:utos?)?\b""", RegexOption.IGNORE_CASE)

    fun isSupportedPackage(packageName: String?): Boolean {
        if (packageName == null) return false
        return packageName in uberPackages || packageName in app99Packages
    }

    fun detectPlatform(packageName: String?): RidePlatform? {
        if (packageName == null) return null
        return when (packageName) {
            in uberPackages -> RidePlatform.UBER
            in app99Packages -> RidePlatform.APP_99
            else -> null
        }
    }

    fun parse(packageName: String?, lines: List<String>): RideOffer? {
        val platform = detectPlatform(packageName) ?: return null
        if (lines.isEmpty()) return null

        val sanitizedLines = lines
            .map { ratePerUnitRegex.replace(it, " ").trim() }
            .filter { it.isNotEmpty() }

        if (sanitizedLines.isEmpty()) return null

        val joined = sanitizedLines.joinToString("\n")
        val lower = joined.lowercase()

        val fares = fareRegex.findAll(joined)
            .mapNotNull { parseBrazilianNumber(it.groupValues[1]) }
            .filter { it in 3.0..2500.0 }
            .toList()
        if (fares.isEmpty()) return null

        val primaryFare = fares.maxOrNull() ?: return null

        val distancesKm = extractDistancesKm(sanitizedLines)
        val durationsMin = extractDurationsMinutes(sanitizedLines)

        if (distancesKm.isEmpty() || durationsMin.isEmpty()) return null

        // Em chamadas Uber/99, os 2 primeiros trechos representam embarque + corrida
        val legsKm = distancesKm.take(2)
        val legsMin = durationsMin.take(2)

        val totalKm = legsKm.sum()
        val totalMinutes = legsMin.sum()

        if (totalKm < 0.1 || totalMinutes <= 0) return null

        // Evita falsos positivos em telas longas de extrato/histórico
        if (fares.size > 5 && !lower.contains("aceitar") && !lower.contains("recusar")) {
            return null
        }

        val pickupKm = if (legsKm.size == 2) legsKm[0] else null
        val tripKm = if (legsKm.size == 2) legsKm[1] else legsKm.firstOrNull()
        val pickupMin = if (legsMin.size == 2) legsMin[0] else null
        val tripMin = if (legsMin.size == 2) legsMin[1] else legsMin.firstOrNull()

        return RideOffer(
            platform = platform,
            fare = primaryFare,
            totalKm = (totalKm * 100.0).roundToInt() / 100.0,
            totalMinutes = totalMinutes,
            pickupKm = pickupKm,
            tripKm = tripKm,
            pickupMinutes = pickupMin,
            tripMinutes = tripMin
        )
    }

    private fun extractDistancesKm(lines: List<String>): List<Double> {
        val result = mutableListOf<Double>()
        for (line in lines) {
            kmRegex.findAll(line).forEach { match ->
                parseBrazilianNumber(match.groupValues[1])?.let { km ->
                    if (km in 0.05..500.0) result.add(km)
                }
            }
            metersRegex.findAll(line).forEach { match ->
                parseBrazilianNumber(match.groupValues[1])?.let { meters ->
                    if (meters in 10.0..5000.0) result.add(meters / 1000.0)
                }
            }
        }
        return result
    }

    private fun extractDurationsMinutes(lines: List<String>): List<Int> {
        val result = mutableListOf<Int>()
        for (line in lines) {
            var consumed = false
            hoursAndMinutesRegex.findAll(line).forEach { match ->
                val h = match.groupValues[1].toIntOrNull() ?: 0
                val m = match.groupValues[2].toIntOrNull() ?: 0
                if (h in 1..12) {
                    result.add(h * 60 + m)
                    consumed = true
                }
            }
            if (!consumed && lessThanOneMinRegex.containsMatchIn(line)) {
                result.add(1)
                consumed = true
            }
            if (!consumed) {
                minutesRegex.findAll(line).forEach { match ->
                    val mins = match.groupValues[1].toIntOrNull()
                    if (mins != null && mins in 1..600) {
                        result.add(mins)
                    }
                }
            }
        }
        return result
    }

    internal fun parseBrazilianNumber(raw: String): Double? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        val normalized = when {
            cleaned.contains(",") && cleaned.contains(".") ->
                cleaned.replace(".", "").replace(",", ".")
            cleaned.contains(",") ->
                cleaned.replace(",", ".")
            else -> cleaned
        }
        return normalized.toDoubleOrNull()
    }
}

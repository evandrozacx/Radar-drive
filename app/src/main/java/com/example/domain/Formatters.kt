package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val ptBrLocale = Locale("pt", "BR")
private val ptBrSymbols = DecimalFormatSymbols(ptBrLocale).apply {
    decimalSeparator = ','
    groupingSeparator = '.'
}

fun currentDayKey(timestampMillis: Long = System.currentTimeMillis()): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", ptBrLocale)
    return formatter.format(Date(timestampMillis))
}

fun formatTimeShort(timestampMillis: Long): String {
    val formatter = SimpleDateFormat("HH:mm", ptBrLocale)
    return formatter.format(Date(timestampMillis))
}

fun formatDayDisplay(dayKey: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", ptBrLocale)
        val parsed = parser.parse(dayKey) ?: return dayKey
        val display = SimpleDateFormat("dd 'de' MMMM", ptBrLocale)
        display.format(parsed)
    } catch (_: Exception) {
        dayKey
    }
}

fun formatCurrencyBr(value: Double): String {
    val df = DecimalFormat("#,##0.00", ptBrSymbols)
    return if (value < 0) "-R$ ${df.format(abs(value))}" else "R$ ${df.format(value)}"
}

fun formatNumberBr(value: Double, decimals: Int = 2): String {
    val pattern = if (decimals <= 0) "#,##0" else "#,##0." + "0".repeat(decimals)
    return DecimalFormat(pattern, ptBrSymbols).format(value)
}

fun formatRatePerKm(value: Double): String = "${formatCurrencyBr(value)}/km"

fun formatRatePerHour(value: Double): String = "${formatCurrencyBr(value)}/h"

fun formatKm(km: Double): String = "${formatNumberBr(km, 1)} km"

fun formatDurationMinutes(totalMinutes: Int): String {
    if (totalMinutes < 60) return "$totalMinutes min"
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (mins == 0) "${hours}h" else "${hours}h ${mins}min"
}

fun formatHoursDecimal(hours: Double): String {
    val totalMinutes = (hours * 60.0).roundToInt().coerceAtLeast(0)
    return formatDurationMinutes(totalMinutes)
}

fun formatSpeechAmount(value: Double): String {
    val safe = value.coerceAtLeast(0.0)
    val totalCents = (safe * 100.0).roundToInt()
    val reais = totalCents / 100
    val centavos = totalCents % 100
    val reaisPart = if (reais == 1) "1 real" else "$reais reais"
    return when {
        centavos == 0 -> reaisPart
        centavos == 1 -> "$reaisPart e 1 centavo"
        else -> "$reaisPart e $centavos centavos"
    }
}

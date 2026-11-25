package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseType
import com.example.domain.RidePlatform
import com.example.domain.formatCurrencyBr
import com.example.domain.formatNumberBr
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.OnRadarCyan
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarCyanContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FuelExpenseDialog(
    fuelPricePerLiter: Double,
    onDismiss: () -> Unit,
    onConfirm: (amountReais: Double?, liters: Double?, description: String) -> Unit
) {
    var inputModeReais by remember { mutableStateOf(true) }
    var valueText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("Abastecimento") }

    val parsed = parseDecimal(valueText)
    val previewInfo = remember(parsed, inputModeReais, fuelPricePerLiter) {
        if (parsed == null || parsed <= 0.0) null
        else if (inputModeReais) {
            val litros = if (fuelPricePerLiter > 0) parsed / fuelPricePerLiter else 0.0
            "Equivale a ${formatNumberBr(litros, 2)} L (${formatCurrencyBr(fuelPricePerLiter)}/L)"
        } else {
            val reais = parsed * fuelPricePerLiter
            "Equivale a ${formatCurrencyBr(reais)} (${formatCurrencyBr(fuelPricePerLiter)}/L)"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CockpitSurface,
        title = {
            Text(
                text = "Registrar Combustível",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = inputModeReais,
                        onClick = { inputModeReais = true },
                        label = { Text("Por Valor (R$)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadarCyanContainer,
                            selectedLabelColor = RadarCyan
                        )
                    )
                    FilterChip(
                        selected = !inputModeReais,
                        onClick = { inputModeReais = false },
                        label = { Text("Por Litros (L)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadarCyanContainer,
                            selectedLabelColor = RadarCyan
                        )
                    )
                }

                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text(if (inputModeReais) "Valor abastecido (R$)" else "Quantidade (Litros)") },
                    placeholder = { Text(if (inputModeReais) "Ex: 100,00" else "Ex: 18,5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fuel_value_input")
                )

                if (previewInfo != null) {
                    Text(
                        text = previewInfo,
                        color = RadarCyan,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Observação (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = parseDecimal(valueText) ?: return@Button
                    if (v <= 0.0) return@Button
                    if (inputModeReais) {
                        onConfirm(v, null, descText)
                    } else {
                        onConfirm(null, v, descText)
                    }
                    onDismiss()
                },
                enabled = (parsed ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadarCyan,
                    contentColor = OnRadarCyan
                ),
                modifier = Modifier.testTag("fuel_confirm_button")
            ) {
                Text("Salvar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
fun TollExtraExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, description: String, amount: Double) -> Unit
) {
    var selectedType by remember { mutableStateOf(ExpenseType.PEDAGIO) }
    var amountText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }
    val parsedAmount = parseDecimal(amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CockpitSurface,
        title = {
            Text(
                text = "Pedágios e Gastos Extras",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == ExpenseType.PEDAGIO,
                        onClick = { selectedType = ExpenseType.PEDAGIO },
                        label = { Text("Pedágio") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadarCyanContainer,
                            selectedLabelColor = RadarCyan
                        )
                    )
                    FilterChip(
                        selected = selectedType == ExpenseType.EXTRA,
                        onClick = { selectedType = ExpenseType.EXTRA },
                        label = { Text("Gasto Extra") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadarCyanContainer,
                            selectedLabelColor = RadarCyan
                        )
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor (R$)") },
                    placeholder = { Text("Ex: 12,40") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Descrição") },
                    placeholder = { Text(if (selectedType == ExpenseType.PEDAGIO) "Ex: Rodovia" else "Ex: Lava-jato / Água") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = parsedAmount ?: return@Button
                    if (v <= 0.0) return@Button
                    onConfirm(selectedType, descText, v)
                    onDismiss()
                },
                enabled = (parsedAmount ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadarCyan,
                    contentColor = OnRadarCyan
                ),
                modifier = Modifier.testTag("expense_confirm_button")
            ) {
                Text("Salvar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
fun OdometerDialog(
    initialStartKm: Double?,
    initialEndKm: Double?,
    initialWorkedMinutes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (startKm: Double?, endKm: Double?, workedMinutesOverride: Int?) -> Unit
) {
    var startText by remember { mutableStateOf(initialStartKm?.let { formatNumberBr(it, 1) } ?: "") }
    var endText by remember { mutableStateOf(initialEndKm?.let { formatNumberBr(it, 1) } ?: "") }
    var hoursText by remember {
        mutableStateOf(initialWorkedMinutes?.let { formatNumberBr(it / 60.0, 1) } ?: "")
    }

    val startVal = parseDecimal(startText)
    val endVal = parseDecimal(endText)
    val delta = if (startVal != null && endVal != null && endVal >= startVal) endVal - startVal else null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CockpitSurface,
        title = {
            Text(
                text = "Odômetro e Jornada",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("KM Inicial do dia") },
                    placeholder = { Text("Ex: 48210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("odometer_start_input")
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("KM Final do dia") },
                    placeholder = { Text("Ex: 48435") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("odometer_end_input")
                )
                if (delta != null) {
                    Text(
                        text = "KM rodado pelo odômetro: ${formatNumberBr(delta, 1)} km",
                        color = RadarCyan,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text("Horas trabalhadas (opcional, ex: 8,5)") },
                    placeholder = { Text("Deixe vazio para usar turno/corridas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = parseDecimal(hoursText)?.let { (it * 60.0).toInt().coerceAtLeast(1) }
                    onConfirm(startVal, endVal, mins)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadarCyan,
                    contentColor = OnRadarCyan
                ),
                modifier = Modifier.testTag("odometer_confirm_button")
            ) {
                Text("Salvar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
fun ManualRideDialog(
    onDismiss: () -> Unit,
    onConfirm: (platform: RidePlatform, fare: Double, totalKm: Double, totalMinutes: Int) -> Unit
) {
    var platform by remember { mutableStateOf(RidePlatform.UBER) }
    var fareText by remember { mutableStateOf("") }
    var kmText by remember { mutableStateOf("") }
    var minText by remember { mutableStateOf("") }

    val fare = parseDecimal(fareText)
    val km = parseDecimal(kmText)
    val mins = minText.trim().toIntOrNull()
    val valid = (fare ?: 0.0) > 0.0 && (km ?: 0.0) > 0.0 && (mins ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CockpitSurface,
        title = {
            Text(
                text = "Adicionar Corrida Manual",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(RidePlatform.UBER, RidePlatform.APP_99, RidePlatform.MANUAL).forEach { item ->
                        FilterChip(
                            selected = platform == item,
                            onClick = { platform = item },
                            label = { Text(item.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RadarCyanContainer,
                                selectedLabelColor = RadarCyan
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = fareText,
                    onValueChange = { fareText = it },
                    label = { Text("Valor recebido (R$)") },
                    placeholder = { Text("Ex: 32,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_fare_input")
                )
                OutlinedTextField(
                    value = kmText,
                    onValueChange = { kmText = it },
                    label = { Text("KM total (embarque + corrida)") },
                    placeholder = { Text("Ex: 14,5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_km_input")
                )
                OutlinedTextField(
                    value = minText,
                    onValueChange = { minText = it },
                    label = { Text("Tempo total (minutos)") },
                    placeholder = { Text("Ex: 28") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_minutes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!valid) return@Button
                    onConfirm(platform, fare!!, km!!, mins!!)
                    onDismiss()
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadarCyan,
                    contentColor = OnRadarCyan
                ),
                modifier = Modifier.testTag("manual_confirm_button")
            ) {
                Text("Adicionar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

fun parseDecimal(raw: String): Double? {
    val cleaned = raw.trim()
    if (cleaned.isEmpty()) return null
    val normalized = if (cleaned.contains(",") && cleaned.contains(".")) {
        cleaned.replace(".", "").replace(",", ".")
    } else {
        cleaned.replace(",", ".")
    }
    return normalized.toDoubleOrNull()
}

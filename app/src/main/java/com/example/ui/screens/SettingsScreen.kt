package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverSettingsEntity
import com.example.domain.formatCurrencyBr
import com.example.domain.formatNumberBr
import com.example.ui.components.parseDecimal
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OnRadarCyan
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalRed
import com.example.ui.theme.SignalYellow
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: DriverSettingsEntity,
    onSaveSettings: (DriverSettingsEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var fuelPriceText by remember { mutableStateOf(formatNumberBr(settings.fuelPricePerLiter, 2)) }
    var consumptionText by remember { mutableStateOf(formatNumberBr(settings.fuelConsumptionKmPerL, 1)) }
    var maintenanceText by remember { mutableStateOf(formatNumberBr(settings.maintenancePerKm, 2)) }
    var fixedDailyText by remember { mutableStateOf(formatNumberBr(settings.dailyFixedCost, 2)) }

    var targetKmText by remember { mutableStateOf(formatNumberBr(settings.targetRatePerKm, 2)) }
    var minKmText by remember { mutableStateOf(formatNumberBr(settings.minRatePerKm, 2)) }
    var targetHourText by remember { mutableStateOf(formatNumberBr(settings.targetRatePerHour, 2)) }
    var minHourText by remember { mutableStateOf(formatNumberBr(settings.minRatePerHour, 2)) }
    var dailyGoalText by remember { mutableStateOf(formatNumberBr(settings.dailyGoalNet, 2)) }

    var voiceEnabled by remember { mutableStateOf(settings.voiceEnabled) }
    var autoHideSecondsText by remember { mutableStateOf(settings.overlayAutoHideSeconds.toString()) }

    LaunchedEffect(settings) {
        fuelPriceText = formatNumberBr(settings.fuelPricePerLiter, 2)
        consumptionText = formatNumberBr(settings.fuelConsumptionKmPerL, 1)
        maintenanceText = formatNumberBr(settings.maintenancePerKm, 2)
        fixedDailyText = formatNumberBr(settings.dailyFixedCost, 2)
        targetKmText = formatNumberBr(settings.targetRatePerKm, 2)
        minKmText = formatNumberBr(settings.minRatePerKm, 2)
        targetHourText = formatNumberBr(settings.targetRatePerHour, 2)
        minHourText = formatNumberBr(settings.minRatePerHour, 2)
        dailyGoalText = formatNumberBr(settings.dailyGoalNet, 2)
        voiceEnabled = settings.voiceEnabled
        autoHideSecondsText = settings.overlayAutoHideSeconds.toString()
    }

    val fuelPricePreview = parseDecimal(fuelPriceText) ?: settings.fuelPricePerLiter
    val consumptionPreview = (parseDecimal(consumptionText) ?: settings.fuelConsumptionKmPerL).coerceAtLeast(0.1)
    val maintenancePreview = parseDecimal(maintenanceText) ?: settings.maintenancePerKm
    val costPerKmPreview = (fuelPricePreview / consumptionPreview) + maintenancePreview

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "PARÂMETROS DO VEÍCULO E METAS",
                        color = RadarCyan,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        text = "Configurações",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            // Vehicle Costs Card
            item {
                SettingsGroupCard(title = "Custos Operacionais") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DecimalSettingField(
                            value = fuelPriceText,
                            onValueChange = { fuelPriceText = it },
                            label = "Combustível (R$/L)",
                            modifier = Modifier.weight(1f)
                        )
                        DecimalSettingField(
                            value = consumptionText,
                            onValueChange = { consumptionText = it },
                            label = "Consumo (km/L)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DecimalSettingField(
                            value = maintenanceText,
                            onValueChange = { maintenanceText = it },
                            label = "Manutenção (R$/km)",
                            modifier = Modifier.weight(1f)
                        )
                        DecimalSettingField(
                            value = fixedDailyText,
                            onValueChange = { fixedDailyText = it },
                            label = "Custo fixo diário (R$)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CockpitSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Custo variável rodado: ${formatCurrencyBr(costPerKmPreview)}/km (combustível + manutenção)",
                            color = RadarCyan,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Targets & Semaphore Card
            item {
                SettingsGroupCard(title = "Metas e Regras do Semáforo") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DecimalSettingField(
                            value = targetKmText,
                            onValueChange = { targetKmText = it },
                            label = "Ideal R$/km",
                            modifier = Modifier.weight(1f)
                        )
                        DecimalSettingField(
                            value = minKmText,
                            onValueChange = { minKmText = it },
                            label = "Mínimo R$/km",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DecimalSettingField(
                            value = targetHourText,
                            onValueChange = { targetHourText = it },
                            label = "Ideal R$/hora",
                            modifier = Modifier.weight(1f)
                        )
                        DecimalSettingField(
                            value = minHourText,
                            onValueChange = { minHourText = it },
                            label = "Mínimo R$/hora",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DecimalSettingField(
                        value = dailyGoalText,
                        onValueChange = { dailyGoalText = it },
                        label = "Meta líquida diária (R$)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "VERDE (Aceitar): atinge ideal de R$/km e R$/hora.",
                            color = SignalGreen,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "AMARELO (Avaliar): atinge só 1 meta ou fica na tolerância.",
                            color = SignalYellow,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "VERMELHO (Recusar): abaixo do mínimo configurado.",
                            color = SignalRed,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Voice & Overlay Card
            item {
                SettingsGroupCard(title = "Voz e Balão Flutuante") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voz imediata de navegação",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Fala o veredito e valor em pt-BR ao detectar chamada",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = { voiceEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnRadarCyan,
                                checkedTrackColor = RadarCyan
                            )
                        )
                    }

                    OutlinedTextField(
                        value = autoHideSecondsText,
                        onValueChange = { autoHideSecondsText = it },
                        label = { Text("Tempo do balão na tela (segundos)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val updated = DriverSettingsEntity(
                            id = 1,
                            fuelPricePerLiter = (parseDecimal(fuelPriceText) ?: settings.fuelPricePerLiter).coerceAtLeast(0.1),
                            fuelConsumptionKmPerL = (parseDecimal(consumptionText) ?: settings.fuelConsumptionKmPerL).coerceAtLeast(1.0),
                            maintenancePerKm = (parseDecimal(maintenanceText) ?: settings.maintenancePerKm).coerceAtLeast(0.0),
                            dailyFixedCost = (parseDecimal(fixedDailyText) ?: settings.dailyFixedCost).coerceAtLeast(0.0),
                            targetRatePerKm = (parseDecimal(targetKmText) ?: settings.targetRatePerKm).coerceAtLeast(0.1),
                            minRatePerKm = (parseDecimal(minKmText) ?: settings.minRatePerKm).coerceAtLeast(0.1),
                            targetRatePerHour = (parseDecimal(targetHourText) ?: settings.targetRatePerHour).coerceAtLeast(1.0),
                            minRatePerHour = (parseDecimal(minHourText) ?: settings.minRatePerHour).coerceAtLeast(1.0),
                            dailyGoalNet = (parseDecimal(dailyGoalText) ?: settings.dailyGoalNet).coerceAtLeast(10.0),
                            voiceEnabled = voiceEnabled,
                            overlayAutoHideSeconds = (autoHideSecondsText.trim().toIntOrNull() ?: settings.overlayAutoHideSeconds).coerceIn(5, 60)
                        )
                        onSaveSettings(updated)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadarCyan,
                        contentColor = OnRadarCyan
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_settings_button")
                ) {
                    Text(
                        text = "Salvar configurações",
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CockpitSurface,
        border = BorderStroke(1.dp, CockpitBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            content()
        }
    }
}

@Composable
private fun DecimalSettingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverSettingsEntity
import com.example.domain.RidePlatform
import com.example.domain.formatKm
import com.example.domain.formatTimeShort
import com.example.service.PermissionHelper
import com.example.service.RadarDiagnosticInfo
import com.example.ui.PermissionStatus
import com.example.ui.components.parseDecimal
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OnRadarCyan
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarCyanContainer
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalGreenContainer
import com.example.ui.theme.SignalRed
import com.example.ui.theme.SignalRedContainer
import com.example.ui.theme.SignalYellow
import com.example.ui.theme.SignalYellowContainer
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RadarTestScreen(
    permissions: PermissionStatus,
    settings: DriverSettingsEntity,
    diagnosticInfo: RadarDiagnosticInfo,
    onToggleVoice: () -> Unit,
    onTestVoice: () -> Unit,
    onSimulateRide: (
        platform: RidePlatform,
        fare: Double,
        pickupKm: Double,
        tripKm: Double,
        pickupMinutes: Int,
        tripMinutes: Int
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var simPlatform by remember { mutableStateOf(RidePlatform.UBER) }
    var fareText by remember { mutableStateOf("29,50") }
    var pickupKmText by remember { mutableStateOf("1,4") }
    var tripKmText by remember { mutableStateOf("9,8") }
    var pickupMinText by remember { mutableStateOf("4") }
    var tripMinText by remember { mutableStateOf("20") }

    val fare = parseDecimal(fareText) ?: 0.0
    val pickupKm = parseDecimal(pickupKmText) ?: 0.0
    val tripKm = parseDecimal(tripKmText) ?: 0.0
    val pickupMin = pickupMinText.trim().toIntOrNull() ?: 0
    val tripMin = tripMinText.trim().toIntOrNull() ?: 0

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
                        text = "PERMISSÕES E SIMULAÇÃO",
                        color = RadarCyan,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        text = "Radar Uber & 99",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            // Permission 1: AccessibilityService
            item {
                PermissionStatusCard(
                    icon = Icons.Default.AccessibilityNew,
                    title = "1. Leitura de Tela (Acessibilidade)",
                    description = "Detecta automaticamente chamadas na Uber e na 99 para ler Valor (R$), KM até o embarque + viagem e tempo total.",
                    granted = permissions.accessibilityEnabled,
                    statusText = if (permissions.accessibilityEnabled) "ATIVO" else "PENDENTE",
                    buttonLabel = if (permissions.accessibilityEnabled) {
                        "Gerenciar Acessibilidade"
                    } else {
                        "Ativar Acessibilidade"
                    },
                    onAction = { PermissionHelper.openAccessibilitySettings(context) },
                    testTag = "permission_accessibility_button"
                )
            }

            // Permission 2: SYSTEM_ALERT_WINDOW
            item {
                PermissionStatusCard(
                    icon = Icons.Default.Layers,
                    title = "2. Balão Flutuante (Sobreposição)",
                    description = "Permite exibir o semáforo (Verde/Amarelo/Vermelho), R$/km, R$/h, lucro e botão Aceita por cima da Uber e 99.",
                    granted = permissions.overlayEnabled,
                    statusText = if (permissions.overlayEnabled) "PERMITIDO" else "PENDENTE",
                    buttonLabel = if (permissions.overlayEnabled) {
                        "Gerenciar Sobreposição"
                    } else {
                        "Permitir Sobreposição"
                    },
                    onAction = { PermissionHelper.openOverlayPermissionSettings(context) },
                    testTag = "permission_overlay_button"
                )
            }

            // Xiaomi / HyperOS / Diagnóstico ao vivo
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CockpitSurface,
                    border = BorderStroke(1.dp, CockpitBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Diagnóstico de Leitura (Uber / 99)",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = diagnosticInfo.statusMessage,
                            color = RadarCyan,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (diagnosticInfo.lastPackageSeen != null && diagnosticInfo.lastScanAtMillis != null) {
                            Text(
                                text = "Último app: ${diagnosticInfo.lastPackageSeen} • ${
                                    formatTimeShort(diagnosticInfo.lastScanAtMillis)
                                }",
                                color = TextSecondary,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp
                            )
                        }
                        if (diagnosticInfo.sampleLines.isNotEmpty()) {
                            Text(
                                text = "Textos lidos: ${diagnosticInfo.sampleLines.joinToString(" | ")}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 3
                            )
                        }
                        Text(
                            text = "Xiaomi / POCO / Redmi: se não aparecer sobre a Uber, ative 'Exibir janelas pop-up em segundo plano' e desative a restrição de bateria nos detalhes do app.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        OutlinedButton(
                            onClick = { PermissionHelper.openAppInfoSettings(context) },
                            border = BorderStroke(1.dp, CockpitBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_app_info_button")
                        ) {
                            Text("Abrir detalhes do app (Bateria / Pop-up)", color = TextPrimary)
                        }
                    }
                }
            }

            // Voice & Audio Channel Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CockpitSurface,
                    border = BorderStroke(1.dp, CockpitBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (settings.voiceEnabled) {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    },
                                    contentDescription = null,
                                    tint = if (settings.voiceEnabled) RadarCyan else SignalRed
                                )
                                Text(
                                    text = "3. Voz Imediata (pt-BR • Canal GPS)",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            StatusPill(
                                text = if (settings.voiceEnabled) "VOZ ATIVA" else "SILENCIADA",
                                color = if (settings.voiceEnabled) SignalGreen else SignalRed,
                                container = if (settings.voiceEnabled) SignalGreenContainer else SignalRedContainer
                            )
                        }

                        Text(
                            text = "Fala no canal de navegação GPS sem cortar sua música permanentemente.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onToggleVoice,
                                border = BorderStroke(1.dp, CockpitBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("radar_voice_mute_button")
                            ) {
                                Text(
                                    text = if (settings.voiceEnabled) "Silenciar Voz" else "Ativar Voz",
                                    color = TextPrimary
                                )
                            }
                            OutlinedButton(
                                onClick = onTestVoice,
                                border = BorderStroke(1.dp, RadarCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("radar_voice_test_button")
                            ) {
                                Text(
                                    text = "Testar Áudio",
                                    color = RadarCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Simulator Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CockpitSurface,
                    border = BorderStroke(1.5.dp, RadarCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Simulador de Chamada",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Teste na hora o semáforo, o balão flutuante e a voz de navegação pt-BR.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Quick Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetChip(
                                label = "Verde Ideal",
                                color = SignalGreen,
                                container = SignalGreenContainer,
                                onClick = {
                                    simPlatform = RidePlatform.UBER
                                    fareText = "32,00"
                                    pickupKmText = "1,2"
                                    tripKmText = "10,3"
                                    pickupMinText = "4"
                                    tripMinText = "19"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PresetChip(
                                label = "Amarelo",
                                color = SignalYellow,
                                container = SignalYellowContainer,
                                onClick = {
                                    simPlatform = RidePlatform.APP_99
                                    fareText = "19,50"
                                    pickupKmText = "2,1"
                                    tripKmText = "9,4"
                                    pickupMinText = "6"
                                    tripMinText = "20"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PresetChip(
                                label = "Vermelho",
                                color = SignalRed,
                                container = SignalRedContainer,
                                onClick = {
                                    simPlatform = RidePlatform.UBER
                                    fareText = "11,20"
                                    pickupKmText = "3,2"
                                    tripKmText = "8,8"
                                    pickupMinText = "9"
                                    tripMinText = "22"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Platform selector
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(RidePlatform.UBER, RidePlatform.APP_99).forEach { platform ->
                                FilterChip(
                                    selected = simPlatform == platform,
                                    onClick = { simPlatform = platform },
                                    label = { Text(platform.label, fontWeight = FontWeight.Bold) },
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
                            label = { Text("Valor da corrida (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = pickupKmText,
                                onValueChange = { pickupKmText = it },
                                label = { Text("KM embarque") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = tripKmText,
                                onValueChange = { tripKmText = it },
                                label = { Text("KM corrida") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = pickupMinText,
                                onValueChange = { pickupMinText = it },
                                label = { Text("Min embarque") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = tripMinText,
                                onValueChange = { tripMinText = it },
                                label = { Text("Min corrida") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Total calculado: ${formatKm(pickupKm + tripKm)} • ${pickupMin + tripMin} min",
                            color = RadarCyan,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = {
                                onSimulateRide(
                                    simPlatform,
                                    fare.coerceAtLeast(1.0),
                                    pickupKm.coerceAtLeast(0.0),
                                    tripKm.coerceAtLeast(0.5),
                                    pickupMin.coerceAtLeast(0),
                                    tripMin.coerceAtLeast(1)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadarCyan,
                                contentColor = OnRadarCyan
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("simulate_test_ride_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simular corrida de teste",
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    statusText: String,
    buttonLabel: String,
    onAction: () -> Unit,
    testTag: String
) {
    val accent = if (granted) SignalGreen else SignalYellow
    val container = if (granted) SignalGreenContainer else SignalYellowContainer

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CockpitSurface,
        border = BorderStroke(1.dp, if (granted) SignalGreen.copy(alpha = 0.45f) else CockpitBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent
                    )
                    Text(
                        text = title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                StatusPill(text = statusText, color = accent, container = container)
            }

            Text(
                text = description,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CockpitSurfaceElevated,
                    contentColor = accent
                ),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(testTag)
            ) {
                Icon(
                    imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color, container: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = container
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    color: Color,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        modifier = modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

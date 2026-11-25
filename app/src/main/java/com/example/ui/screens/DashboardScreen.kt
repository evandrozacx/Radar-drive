package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyShiftEntity
import com.example.data.DailySummary
import com.example.data.DriverSettingsEntity
import com.example.data.ExpenseEntity
import com.example.data.ExpenseType
import com.example.data.RideEntity
import com.example.domain.RidePlatform
import com.example.domain.SemaphoreVerdict
import com.example.domain.formatCurrencyBr
import com.example.domain.formatDayDisplay
import com.example.domain.formatDurationMinutes
import com.example.domain.formatHoursDecimal
import com.example.domain.formatKm
import com.example.domain.formatNumberBr
import com.example.domain.formatRatePerHour
import com.example.domain.formatRatePerKm
import com.example.domain.formatTimeShort
import com.example.ui.PermissionStatus
import com.example.ui.components.FuelExpenseDialog
import com.example.ui.components.ManualRideDialog
import com.example.ui.components.OdometerDialog
import com.example.ui.components.TollExtraExpenseDialog
import com.example.ui.theme.App99Accent
import com.example.ui.theme.App99Bg
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UberAccent
import com.example.ui.theme.UberBg

@Composable
fun DashboardScreen(
    summary: DailySummary,
    rides: List<RideEntity>,
    expenses: List<ExpenseEntity>,
    shift: DailyShiftEntity,
    settings: DriverSettingsEntity,
    permissions: PermissionStatus,
    onToggleVoice: () -> Unit,
    onToggleShift: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onAddFuel: (amountReais: Double?, liters: Double?, description: String) -> Unit,
    onAddTollOrExtra: (type: String, description: String, amount: Double) -> Unit,
    onUpdateOdometer: (startKm: Double?, endKm: Double?, workedMinutesOverride: Int?) -> Unit,
    onAddManualRide: (platform: RidePlatform, fare: Double, totalKm: Double, totalMinutes: Int) -> Unit,
    onDeleteRide: (Long) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFuelDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showOdometerDialog by remember { mutableStateOf(false) }
    var showManualRideDialog by remember { mutableStateOf(false) }

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
            // 1. Cockpit Top Bar
            item {
                CockpitHeader(
                    dayKey = summary.dayKey,
                    isVoiceEnabled = settings.voiceEnabled,
                    isShiftActive = shift.isShiftActive,
                    permissions = permissions,
                    onToggleVoice = onToggleVoice,
                    onToggleShift = onToggleShift,
                    onNavigateToRadar = onNavigateToRadar
                )
            }

            // 2. Hero Net Balance Card
            item {
                HeroNetBalanceCard(summary = summary)
            }

            // 3. Telemetry KPIs Grid (2 columns)
            item {
                KpiGridSection(summary = summary)
            }

            // 4. Post-Ride Actions Bar
            item {
                PostRideActionsSection(
                    onFuelClick = { showFuelDialog = true },
                    onTollExtraClick = { showExpenseDialog = true },
                    onOdometerClick = { showOdometerDialog = true },
                    onManualRideClick = { showManualRideDialog = true }
                )
            }

            // 5. Today's Rides Section
            item {
                SectionHeaderRow(
                    title = "Corridas do dia (${rides.size})",
                    subtitle = "Toque no ícone para remover"
                )
            }

            if (rides.isEmpty()) {
                item {
                    EmptyLedgerCard(
                        icon = Icons.Default.DirectionsCar,
                        title = "Nenhuma corrida salva hoje",
                        description = "Aceite chamadas pelo balão flutuante, adicione manualmente ou simule um teste.",
                        actionLabel = "Simular corrida de teste",
                        onAction = onNavigateToRadar
                    )
                }
            } else {
                items(rides, key = { "ride_${it.id}" }) { ride ->
                    RideHistoryCard(
                        ride = ride,
                        onDelete = { onDeleteRide(ride.id) }
                    )
                }
            }

            // 6. Today's Expenses Section
            item {
                SectionHeaderRow(
                    title = "Combustível e Gastos (${expenses.size})",
                    subtitle = if (summary.fuelIsActual) "Combustível real lançado" else "Sem abastecimento hoje (usando estimativa por KM)"
                )
            }

            if (expenses.isEmpty()) {
                item {
                    EmptyLedgerCard(
                        icon = Icons.Default.LocalGasStation,
                        title = "Nenhum gasto pós-corrida lançado",
                        description = "Registre abastecimento (R$ ou L), pedágios ou extras para fechar o saldo líquido real.",
                        actionLabel = "Lançar combustível",
                        onAction = { showFuelDialog = true }
                    )
                }
            } else {
                items(expenses, key = { "exp_${it.id}" }) { expense ->
                    ExpenseHistoryCard(
                        expense = expense,
                        onDelete = { onDeleteExpense(expense.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showFuelDialog) {
        FuelExpenseDialog(
            fuelPricePerLiter = settings.fuelPricePerLiter,
            onDismiss = { showFuelDialog = false },
            onConfirm = onAddFuel
        )
    }
    if (showExpenseDialog) {
        TollExtraExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onConfirm = onAddTollOrExtra
        )
    }
    if (showOdometerDialog) {
        OdometerDialog(
            initialStartKm = shift.odometerStartKm,
            initialEndKm = shift.odometerEndKm,
            initialWorkedMinutes = shift.workedMinutesOverride,
            onDismiss = { showOdometerDialog = false },
            onConfirm = onUpdateOdometer
        )
    }
    if (showManualRideDialog) {
        ManualRideDialog(
            onDismiss = { showManualRideDialog = false },
            onConfirm = onAddManualRide
        )
    }
}

@Composable
private fun CockpitHeader(
    dayKey: String,
    isVoiceEnabled: Boolean,
    isShiftActive: Boolean,
    permissions: PermissionStatus,
    onToggleVoice: () -> Unit,
    onToggleShift: () -> Unit,
    onNavigateToRadar: () -> Unit
) {
    val radarAllReady = permissions.accessibilityEnabled && permissions.overlayEnabled
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "RADAR DRIVER",
                    color = RadarCyan,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp
                )
                Text(
                    text = "Painel de ${formatDayDisplay(dayKey)}",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onToggleVoice,
                    shape = CircleShape,
                    color = if (isVoiceEnabled) RadarCyanContainer else SignalRedContainer,
                    border = BorderStroke(
                        1.dp,
                        if (isVoiceEnabled) RadarCyan.copy(alpha = 0.5f) else SignalRed.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dashboard_voice_toggle")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isVoiceEnabled) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.AutoMirrored.Filled.VolumeOff
                            },
                            contentDescription = if (isVoiceEnabled) "Silenciar voz" else "Ativar voz",
                            tint = if (isVoiceEnabled) RadarCyan else SignalRed
                        )
                    }
                }

                Button(
                    onClick = onToggleShift,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isShiftActive) SignalGreenContainer else CockpitSurfaceElevated,
                        contentColor = if (isShiftActive) SignalGreen else TextPrimary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isShiftActive) SignalGreen else CockpitBorder
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("toggle_shift_button")
                ) {
                    Icon(
                        imageVector = if (isShiftActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isShiftActive) "Turno Ativo" else "Iniciar Turno",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Radar Permission Status Banner
        Surface(
            onClick = onNavigateToRadar,
            shape = RoundedCornerShape(14.dp),
            color = if (radarAllReady) SignalGreenContainer.copy(alpha = 0.6f) else SignalYellowContainer.copy(alpha = 0.55f),
            border = BorderStroke(
                1.dp,
                if (radarAllReady) SignalGreen.copy(alpha = 0.45f) else SignalYellow.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("radar_status_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = if (radarAllReady) SignalGreen else SignalYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (radarAllReady) {
                            "Radar Uber/99 ativo com balão e voz pt-BR"
                        } else {
                            "Permissões pendentes: toque para ativar Acessibilidade / Balão ou testar"
                        },
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (radarAllReady) "OK" else "CONFIGURAR",
                    color = if (radarAllReady) SignalGreen else SignalYellow,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun HeroNetBalanceCard(summary: DailySummary) {
    val isPositive = summary.netBalance >= 0.0
    val accent = if (isPositive) SignalGreen else SignalRed
    val goalProgress = if (summary.dailyGoalNet > 0.0) {
        (summary.netBalance / summary.dailyGoalNet).toFloat().coerceIn(0f, 1f)
    } else 0f
    val goalPercent = (goalProgress * 100f).toInt()

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CockpitSurface,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_net_balance_card")
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.14f),
                            CockpitSurface
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "SALDO LÍQUIDO REAL",
                        color = TextSecondary,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCurrencyBr(summary.netBalance),
                        color = accent,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CockpitSurfaceElevated,
                    border = BorderStroke(1.dp, CockpitBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "META DIÁRIA",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${formatCurrencyBr(summary.dailyGoalNet)} ($goalPercent%)",
                            color = TextPrimary,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { goalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = accent,
                trackColor = CockpitSurfaceElevated
            )

            // Financial Breakdown Formula
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CockpitSurfaceElevated)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BreakdownLine("Faturamento bruto", formatCurrencyBr(summary.grossTotal), TextPrimary)
                BreakdownLine(
                    if (summary.fuelIsActual) {
                        "- Combustível (${formatNumberBr(summary.fuelLitersLogged, 1)} L)"
                    } else {
                        "- Combustível (estimado km)"
                    },
                    "- ${formatCurrencyBr(summary.fuelCost)}",
                    SignalYellow
                )
                BreakdownLine(
                    "- Manutenção (${formatKm(summary.effectiveKm)})",
                    "- ${formatCurrencyBr(summary.maintenanceCost)}",
                    SignalYellow
                )
                BreakdownLine(
                    "- Pedágios e extras",
                    "- ${formatCurrencyBr(summary.tollsAndExtrasCost)}",
                    SignalRed
                )
                BreakdownLine(
                    "- Custo fixo diário",
                    "- ${formatCurrencyBr(summary.fixedDailyCost)}",
                    TextSecondary
                )
            }
        }
    }
}

@Composable
private fun BreakdownLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = valueColor,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun KpiGridSection(summary: DailySummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "FATURAMENTO BRUTO",
                value = formatCurrencyBr(summary.grossTotal),
                subtitle = "Uber ${formatCurrencyBr(summary.grossUber)} • 99 ${formatCurrencyBr(summary.gross99)}",
                accent = RadarCyan,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "CORRIDAS ACEITAS",
                value = "${summary.ridesCount}",
                subtitle = if (summary.grossManual > 0) "Inclui ${formatCurrencyBr(summary.grossManual)} manual" else "Uber e 99",
                accent = SignalGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "KM RODADO",
                value = formatKm(summary.effectiveKm),
                subtitle = if (summary.odometerKm != null) "Via odômetro" else "Soma das corridas",
                accent = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "HORAS TRABALHADAS",
                value = formatHoursDecimal(summary.workedHours),
                subtitle = "${summary.workedMinutes} min no dia",
                accent = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "R$/KM MÉDIO",
                value = formatRatePerKm(summary.avgRatePerKm),
                subtitle = "Bruto / KM rodado",
                accent = RadarCyan,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "R$/HORA MÉDIO",
                value = formatRatePerHour(summary.avgRatePerHour),
                subtitle = "Bruto / Horas",
                accent = RadarCyan,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = CockpitSurface,
        border = BorderStroke(1.dp, CockpitBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = TextMuted,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Text(
                text = value,
                color = accent,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PostRideActionsSection(
    onFuelClick: () -> Unit,
    onTollExtraClick: () -> Unit,
    onOdometerClick: () -> Unit,
    onManualRideClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "FECHAMENTO E GASTOS PÓS-CORRIDA",
            color = TextSecondary,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PostRideActionTile(
                icon = Icons.Default.LocalGasStation,
                label = "Combustível",
                sublabel = "R$ ou L",
                onClick = onFuelClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_add_fuel")
            )
            PostRideActionTile(
                icon = Icons.Default.ReceiptLong,
                label = "Pedágio/Extra",
                sublabel = "Lançar R$",
                onClick = onTollExtraClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_add_expense")
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PostRideActionTile(
                icon = Icons.Default.Speed,
                label = "Odômetro",
                sublabel = "KM inicial/final",
                onClick = onOdometerClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_edit_odometer")
            )
            PostRideActionTile(
                icon = Icons.Default.AddRoad,
                label = "Corrida Manual",
                sublabel = "Uber / 99",
                onClick = onManualRideClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_add_manual_ride")
            )
        }
    }
}

@Composable
private fun PostRideActionTile(
    icon: ImageVector,
    label: String,
    sublabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CockpitSurfaceElevated,
        border = BorderStroke(1.dp, CockpitBorder),
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RadarCyanContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = RadarCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = sublabel,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitle,
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RideHistoryCard(
    ride: RideEntity,
    onDelete: () -> Unit
) {
    val signalColor = when (ride.verdict) {
        SemaphoreVerdict.VERDE.name -> SignalGreen
        SemaphoreVerdict.AMARELO.name -> SignalYellow
        else -> SignalRed
    }
    val is99 = ride.platform.equals(RidePlatform.APP_99.label, ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CockpitSurface,
        border = BorderStroke(1.dp, CockpitBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(signalColor)
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (is99) App99Bg else UberBg
                        ) {
                            Text(
                                text = ride.platform,
                                color = if (is99) App99Accent else UberAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = formatCurrencyBr(ride.fare),
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = formatTimeShort(ride.createdAt),
                            color = TextMuted,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "${formatKm(ride.totalKm)} • ${formatDurationMinutes(ride.totalMinutes)} • ${
                            formatRatePerKm(ride.ratePerKm)
                        } • Líq. ${formatCurrencyBr(ride.estimatedNetProfit)}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remover corrida",
                    tint = TextMuted
                )
            }
        }
    }
}

@Composable
private fun ExpenseHistoryCard(
    expense: ExpenseEntity,
    onDelete: () -> Unit
) {
    val typeLabel = when (expense.type) {
        ExpenseType.COMBUSTIVEL -> "Combustível"
        ExpenseType.PEDAGIO -> "Pedágio"
        else -> "Extra"
    }
    val typeColor = when (expense.type) {
        ExpenseType.COMBUSTIVEL -> SignalYellow
        ExpenseType.PEDAGIO -> RadarCyan
        else -> SignalRed
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CockpitSurface,
        border = BorderStroke(1.dp, CockpitBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = typeLabel.uppercase(),
                        color = typeColor,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = formatCurrencyBr(expense.amount),
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (expense.liters != null) {
                        Text(
                            text = "(${formatNumberBr(expense.liters, 2)} L)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = "${expense.description} • ${formatTimeShort(expense.createdAt)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remover gasto",
                    tint = TextMuted
                )
            }
        }
    }
}

@Composable
private fun EmptyLedgerCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CockpitSurface,
        border = BorderStroke(1.dp, CockpitBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                onClick = onAction,
                border = BorderStroke(1.dp, RadarCyan),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RadarCyan),
                modifier = Modifier.height(48.dp)
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

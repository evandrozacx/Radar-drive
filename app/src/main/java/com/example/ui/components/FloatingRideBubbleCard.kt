package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.EvaluatedRide
import com.example.domain.RidePlatform
import com.example.domain.SemaphoreVerdict
import com.example.domain.formatCurrencyBr
import com.example.domain.formatDurationMinutes
import com.example.domain.formatKm
import com.example.domain.formatRatePerHour
import com.example.domain.formatRatePerKm
import com.example.ui.theme.App99Accent
import com.example.ui.theme.App99Bg
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSignalGreen
import com.example.ui.theme.OnSignalRed
import com.example.ui.theme.OnSignalYellow
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalGreenContainer
import com.example.ui.theme.SignalRed
import com.example.ui.theme.SignalRedContainer
import com.example.ui.theme.SignalYellow
import com.example.ui.theme.SignalYellowContainer
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UberAccent
import com.example.ui.theme.UberBg

@Composable
fun FloatingRideBubbleCard(
    evaluated: EvaluatedRide,
    isVoiceEnabled: Boolean,
    onToggleVoice: () -> Unit,
    onAcceptRide: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onDragDelta: ((dx: Float, dy: Float) -> Unit)? = null
) {
    val signalColor: Color
    val signalContainer: Color
    val onSignalColor: Color
    val verdictTitle: String
    when (evaluated.verdict) {
        SemaphoreVerdict.VERDE -> {
            signalColor = SignalGreen
            signalContainer = SignalGreenContainer
            onSignalColor = OnSignalGreen
            verdictTitle = "ACEITAR"
        }
        SemaphoreVerdict.AMARELO -> {
            signalColor = SignalYellow
            signalContainer = SignalYellowContainer
            onSignalColor = OnSignalYellow
            verdictTitle = "AVALIAR"
        }
        SemaphoreVerdict.VERMELHO -> {
            signalColor = SignalRed
            signalContainer = SignalRedContainer
            onSignalColor = OnSignalRed
            verdictTitle = "RECUSAR"
        }
    }

    val platformBg = when (evaluated.offer.platform) {
        RidePlatform.APP_99 -> App99Bg
        else -> UberBg
    }
    val platformFg = when (evaluated.offer.platform) {
        RidePlatform.APP_99 -> App99Accent
        else -> UberAccent
    }

    val dragModifier = if (onDragDelta != null) {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onDragDelta(dragAmount.x, dragAmount.y)
            }
        }
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .widthIn(min = 290.dp, max = 360.dp)
            .testTag("floating_ride_bubble"),
        shape = RoundedCornerShape(22.dp),
        color = CockpitSurface.copy(alpha = 0.97f),
        border = BorderStroke(2.dp, signalColor),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Traffic-Light Semaphore Bar (Draggable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(signalContainer)
                    .then(dragModifier)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onDragDelta != null) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Arrastar balão",
                            tint = signalColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(signalColor)
                    )
                    Text(
                        text = verdictTitle,
                        color = signalColor,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.8.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = platformBg,
                        border = BorderStroke(1.dp, platformFg.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = evaluated.offer.platform.label,
                            color = platformFg,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    if (evaluated.offer.isTestSimulation) {
                        Text(
                            text = "TESTE",
                            color = RadarCyan,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("bubble_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar balão",
                        tint = TextPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hero Fare + Distance/Time summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "VALOR DA CORRIDA",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = formatCurrencyBr(evaluated.offer.fare),
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${formatKm(evaluated.offer.totalKm)} • ${
                                formatDurationMinutes(evaluated.offer.totalMinutes)
                            }",
                            color = TextPrimary,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (evaluated.offer.pickupKm != null && evaluated.offer.tripKm != null) {
                            Text(
                                text = "Emb. ${formatKm(evaluated.offer.pickupKm)} + Viag. ${
                                    formatKm(evaluated.offer.tripKm)
                                }",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 3-Column Metrics Strip: R$/km, R$/hora, Lucro líquido estimado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CockpitSurfaceElevated)
                        .border(1.dp, signalColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BubbleMetricCell(
                        label = "R$/KM",
                        value = formatRatePerKm(evaluated.ratePerKm),
                        valueColor = signalColor,
                        modifier = Modifier.weight(1f)
                    )
                    BubbleMetricCell(
                        label = "R$/HORA",
                        value = formatRatePerHour(evaluated.ratePerHour),
                        valueColor = signalColor,
                        modifier = Modifier.weight(1f)
                    )
                    BubbleMetricCell(
                        label = "LUCRO LÍQ.",
                        value = formatCurrencyBr(evaluated.estimatedNetProfit),
                        valueColor = if (evaluated.estimatedNetProfit >= 0) SignalGreen else SignalRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = evaluated.reasonText,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Quick Voice Mute + "Aceita" Save Ride Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onToggleVoice,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = CockpitSurfaceElevated,
                            contentColor = if (isVoiceEnabled) RadarCyan else SignalRed
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                1.dp,
                                if (isVoiceEnabled) RadarCyan.copy(alpha = 0.45f)
                                else SignalRed.copy(alpha = 0.45f),
                                CircleShape
                            )
                            .testTag("bubble_voice_toggle")
                    ) {
                        Icon(
                            imageVector = if (isVoiceEnabled) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.AutoMirrored.Filled.VolumeOff
                            },
                            contentDescription = if (isVoiceEnabled) "Silenciar voz" else "Ativar voz"
                        )
                    }

                    Button(
                        onClick = onAcceptRide,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SignalGreen,
                            contentColor = OnSignalGreen
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("bubble_accept_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aceita",
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleMetricCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

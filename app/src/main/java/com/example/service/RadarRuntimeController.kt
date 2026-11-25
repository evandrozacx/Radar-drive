package com.example.service

import android.content.Context
import com.example.data.RadarRepository
import com.example.domain.EvaluatedRide
import com.example.domain.RideEvaluator
import com.example.domain.RideOffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RadarDiagnosticInfo(
    val lastPackageSeen: String? = null,
    val lastScanAtMillis: Long? = null,
    val statusMessage: String = "Aguardando chamada na Uber ou 99",
    val sampleLines: List<String> = emptyList()
)

class RadarRuntimeController(
    context: Context,
    private val repository: RadarRepository,
    val voiceAnnouncer: VoiceAnnouncer
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val overlayManager: OverlayBubbleManager = OverlayBubbleManager(
        appContext = appContext,
        repository = repository,
        controller = this
    )

    private val _activeEvaluatedRide = MutableStateFlow<EvaluatedRide?>(null)
    val activeEvaluatedRide: StateFlow<EvaluatedRide?> = _activeEvaluatedRide.asStateFlow()

    private val _isAccessibilityConnected = MutableStateFlow(false)
    val isAccessibilityConnected: StateFlow<Boolean> = _isAccessibilityConnected.asStateFlow()

    private val _lastEventBanner = MutableStateFlow<String?>(null)
    val lastEventBanner: StateFlow<String?> = _lastEventBanner.asStateFlow()

    private val _diagnosticInfo = MutableStateFlow(RadarDiagnosticInfo())
    val diagnosticInfo: StateFlow<RadarDiagnosticInfo> = _diagnosticInfo.asStateFlow()

    private var lastOfferFingerprint: String? = null
    private var lastOfferTimestamp: Long = 0L

    init {
        scope.launch {
            repository.settingsFlow.collect { settings ->
                voiceAnnouncer.setMuted(!settings.voiceEnabled)
            }
        }
    }

    fun setAccessibilityConnected(connected: Boolean) {
        _isAccessibilityConnected.value = connected
    }

    fun updateDiagnostics(
        packageName: String?,
        statusMessage: String,
        sampleLines: List<String>
    ) {
        _diagnosticInfo.value = RadarDiagnosticInfo(
            lastPackageSeen = packageName,
            lastScanAtMillis = System.currentTimeMillis(),
            statusMessage = statusMessage,
            sampleLines = sampleLines
        )
    }

    fun presentRideOffer(offer: RideOffer, forceShow: Boolean = false) {
        val now = System.currentTimeMillis()
        val fingerprint = "${offer.platform.name}|${offer.fare}|${offer.totalKm}|${offer.totalMinutes}"
        if (!forceShow && fingerprint == lastOfferFingerprint && (now - lastOfferTimestamp) < 8_000L) {
            return
        }
        lastOfferFingerprint = fingerprint
        lastOfferTimestamp = now

        scope.launch {
            val settings = repository.getSettingsOnce()
            val evaluated = RideEvaluator.evaluate(offer, settings)
            _activeEvaluatedRide.value = evaluated

            voiceAnnouncer.setMuted(!settings.voiceEnabled)
            if (settings.voiceEnabled) {
                voiceAnnouncer.speak(evaluated.buildSpeechPhrase())
            }

            if (overlayManager.canShowSystemOverlay()) {
                overlayManager.showOrUpdate()
            }
        }
    }

    fun dismissBubble() {
        _activeEvaluatedRide.value = null
        voiceAnnouncer.stop()
        overlayManager.hide()
    }

    fun acceptActiveRide() {
        val evaluated = _activeEvaluatedRide.value ?: return
        _activeEvaluatedRide.value = null
        voiceAnnouncer.stop()
        overlayManager.hide()
        scope.launch {
            repository.acceptEvaluatedRide(evaluated)
            _lastEventBanner.value =
                "Corrida ${evaluated.offer.platform.label} salva no dia!"
        }
    }

    fun toggleVoiceMuted() {
        scope.launch {
            val settings = repository.getSettingsOnce()
            val nextEnabled = !settings.voiceEnabled
            repository.setVoiceEnabled(nextEnabled)
            voiceAnnouncer.setMuted(!nextEnabled)
        }
    }

    fun clearBanner() {
        _lastEventBanner.value = null
    }
}

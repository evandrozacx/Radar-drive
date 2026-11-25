package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RadarDriverApp
import com.example.data.DailyShiftEntity
import com.example.data.DailySummary
import com.example.data.DriverSettingsEntity
import com.example.data.ExpenseEntity
import com.example.data.RideEntity
import com.example.domain.EvaluatedRide
import com.example.domain.RideOffer
import com.example.domain.RidePlatform
import com.example.domain.currentDayKey
import com.example.service.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionStatus(
    val accessibilityEnabled: Boolean = false,
    val overlayEnabled: Boolean = false
)

class RadarViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RadarDriverApp
    private val repository = app.repository
    private val runtimeController = app.runtimeController

    val dayKey: String = currentDayKey()

    val summary: StateFlow<DailySummary> = repository.observeDailySummary(dayKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailySummary(dayKey = dayKey))

    val rides: StateFlow<List<RideEntity>> = repository.observeRides(dayKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.observeExpenses(dayKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shift: StateFlow<DailyShiftEntity> = repository.observeShift(dayKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyShiftEntity(dayKey = dayKey))

    val settings: StateFlow<DriverSettingsEntity> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriverSettingsEntity())

    val activeBubble: StateFlow<EvaluatedRide?> = runtimeController.activeEvaluatedRide

    val lastEventBanner: StateFlow<String?> = runtimeController.lastEventBanner

    val diagnosticInfo = runtimeController.diagnosticInfo

    fun canShowSystemOverlay(): Boolean = runtimeController.overlayManager.canShowSystemOverlay()

    private val _permissions = MutableStateFlow(PermissionStatus())
    val permissions: StateFlow<PermissionStatus> = combine(
        _permissions,
        runtimeController.isAccessibilityConnected
    ) { local, connected ->
        local.copy(accessibilityEnabled = local.accessibilityEnabled || connected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PermissionStatus())

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultSettings()
        }
        refreshPermissions()
    }

    fun refreshPermissions() {
        _permissions.value = PermissionStatus(
            accessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(app),
            overlayEnabled = PermissionHelper.hasOverlayPermission(app)
        )
    }

    fun simulateTestRide(
        platform: RidePlatform,
        fare: Double,
        pickupKm: Double,
        tripKm: Double,
        pickupMinutes: Int,
        tripMinutes: Int
    ) {
        val totalKm = (pickupKm + tripKm).coerceAtLeast(0.5)
        val totalMinutes = (pickupMinutes + tripMinutes).coerceAtLeast(1)
        val offer = RideOffer(
            platform = platform,
            fare = fare.coerceAtLeast(1.0),
            totalKm = totalKm,
            totalMinutes = totalMinutes,
            pickupKm = pickupKm,
            tripKm = tripKm,
            pickupMinutes = pickupMinutes,
            tripMinutes = tripMinutes,
            isTestSimulation = true
        )
        runtimeController.presentRideOffer(offer, forceShow = true)
    }

    fun acceptActiveRide() {
        runtimeController.acceptActiveRide()
    }

    fun dismissBubble() {
        runtimeController.dismissBubble()
    }

    fun toggleVoiceEnabled() {
        runtimeController.toggleVoiceMuted()
    }

    fun testVoicePhrase() {
        viewModelScope.launch {
            val current = repository.getSettingsOnce()
            if (!current.voiceEnabled) {
                repository.setVoiceEnabled(true)
            }
            app.voiceAnnouncer.setMuted(false)
            app.voiceAnnouncer.speak("Radar Driver pronto. Aceitar corrida, 28 reais e 50 centavos")
        }
    }

    fun addFuelExpense(amountReais: Double?, liters: Double?, description: String) {
        viewModelScope.launch {
            val id = repository.addFuelExpense(amountReais, liters, description, dayKey)
            if (id > 0) {
                _uiMessage.value = "Combustível lançado no dia"
            }
        }
    }

    fun addTollOrExtra(type: String, description: String, amount: Double) {
        viewModelScope.launch {
            val id = repository.addTollOrExtraExpense(type, description, amount, dayKey)
            if (id > 0) {
                _uiMessage.value = "Gasto lançado no dia"
            }
        }
    }

    fun addManualRide(platform: RidePlatform, fare: Double, totalKm: Double, totalMinutes: Int) {
        viewModelScope.launch {
            val id = repository.addManualRide(platform, fare, totalKm, totalMinutes, dayKey)
            if (id > 0) {
                _uiMessage.value = "Corrida manual adicionada"
            }
        }
    }

    fun deleteRide(id: Long) {
        viewModelScope.launch {
            repository.deleteRide(id)
            _uiMessage.value = "Corrida removida"
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            _uiMessage.value = "Gasto removido"
        }
    }

    fun updateOdometer(startKm: Double?, endKm: Double?, workedMinutesOverride: Int?) {
        viewModelScope.launch {
            repository.updateOdometer(startKm, endKm, dayKey)
            repository.updateWorkedMinutesOverride(workedMinutesOverride, dayKey)
            _uiMessage.value = "Odômetro e jornada atualizados"
        }
    }

    fun toggleShift() {
        viewModelScope.launch {
            repository.toggleShift(dayKey)
        }
    }

    fun saveSettings(updated: DriverSettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(updated)
            _uiMessage.value = "Configurações salvas"
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
        runtimeController.clearBanner()
    }
}

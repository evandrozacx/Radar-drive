package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.DriverSettingsEntity
import com.example.data.RadarRepository
import com.example.ui.components.FloatingRideBubbleCard
import com.example.ui.theme.RadarDriverTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Gerencia a exibição do balão flutuante diretamente via WindowManager usando
 * TYPE_ACCESSIBILITY_OVERLAY (quando o AccessibilityService está ativo, evitando
 * bloqueios de ForegroundService em segundo plano no Android 12+/HyperOS) ou
 * TYPE_APPLICATION_OVERLAY (quando SYSTEM_ALERT_WINDOW está concedido).
 */
class OverlayBubbleManager(
    private val appContext: Context,
    private val repository: RadarRepository,
    private val controller: RadarRuntimeController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var accessibilityServiceRef: AccessibilityService? = null
    private var activeWindowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var overlayLifecycleOwner: BubbleLifecycleOwner? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null
    private var autoHideJob: Job? = null

    fun attachAccessibilityService(service: AccessibilityService?) {
        accessibilityServiceRef = service
    }

    fun canShowSystemOverlay(): Boolean {
        return accessibilityServiceRef != null || PermissionHelper.hasOverlayPermission(appContext)
    }

    fun showOrUpdate() {
        val service = accessibilityServiceRef
        val hasAppOverlay = PermissionHelper.hasOverlayPermission(appContext)

        if (service == null && !hasAppOverlay) {
            return
        }

        if (overlayView == null) {
            val hostContext: Context
            val overlayType: Int

            if (service != null) {
                hostContext = service
                overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                hostContext = appContext
                overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
            }

            val wm = hostContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            activeWindowManager = wm

            val owner = BubbleLifecycleOwner().also {
                it.onCreate()
                it.onStart()
                it.onResume()
            }
            overlayLifecycleOwner = owner

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = 0
                y = 140
            }
            windowLayoutParams = params

            val composeView = ComposeView(hostContext).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setContent {
                    val activeRide by controller.activeEvaluatedRide.collectAsStateWithLifecycle()
                    val settings by repository.settingsFlow.collectAsStateWithLifecycle(
                        initialValue = DriverSettingsEntity()
                    )

                    RadarDriverTheme {
                        val ride = activeRide
                        if (ride != null) {
                            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                                FloatingRideBubbleCard(
                                    evaluated = ride,
                                    isVoiceEnabled = settings.voiceEnabled,
                                    onToggleVoice = { controller.toggleVoiceMuted() },
                                    onAcceptRide = { controller.acceptActiveRide() },
                                    onDismiss = { controller.dismissBubble() },
                                    onDragDelta = { dx, dy ->
                                        val lp = windowLayoutParams ?: return@FloatingRideBubbleCard
                                        lp.x += dx.roundToInt()
                                        lp.y += dy.roundToInt()
                                        runCatching {
                                            activeWindowManager?.updateViewLayout(this@apply, lp)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            overlayView = composeView
            val added = runCatching {
                wm.addView(composeView, params)
            }.isSuccess

            if (!added && service != null && hasAppOverlay) {
                // Fallback para TYPE_APPLICATION_OVERLAY caso o sistema restrinja TYPE_ACCESSIBILITY_OVERLAY
                val fallbackWm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                activeWindowManager = fallbackWm
                params.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                runCatching { fallbackWm.addView(composeView, params) }
            }
        }

        autoHideJob?.cancel()
        autoHideJob = scope.launch {
            val seconds = repository.getSettingsOnce().overlayAutoHideSeconds.coerceIn(5, 60)
            delay(seconds * 1000L)
            controller.dismissBubble()
        }
    }

    fun hide() {
        autoHideJob?.cancel()
        autoHideJob = null
        val view = overlayView
        val wm = activeWindowManager
        if (view != null && wm != null) {
            runCatching { wm.removeView(view) }
        }
        overlayView = null
        activeWindowManager = null
        overlayLifecycleOwner?.onDestroy()
        overlayLifecycleOwner = null
        windowLayoutParams = null
    }
}

private class BubbleLifecycleOwner :
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val viewModelStore: ViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}

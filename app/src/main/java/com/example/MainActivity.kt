package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.RadarViewModel
import com.example.ui.components.FloatingRideBubbleCard
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.RadarTestScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarCyanContainer
import com.example.ui.theme.RadarDriverTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.roundToInt

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_RADAR = "radar"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            RadarDriverTheme {
                RadarDriverMainShell()
            }
        }
    }
}

@Composable
fun RadarDriverMainShell(
    viewModel: RadarViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val rides by viewModel.rides.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val shift by viewModel.shift.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val activeBubble by viewModel.activeBubble.collectAsStateWithLifecycle()
    val diagnosticInfo by viewModel.diagnosticInfo.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.lastEventBanner.collectAsStateWithLifecycle()

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissions()
    }

    LaunchedEffect(uiMessage, bannerMessage) {
        val msg = uiMessage ?: bannerMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUiMessage()
        }
    }

    val navItems = remember {
        listOf(
            NavDestinationItem(
                route = ROUTE_DASHBOARD,
                labelRes = R.string.nav_dashboard,
                selectedIcon = Icons.Filled.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                testTag = "nav_dashboard"
            ),
            NavDestinationItem(
                route = ROUTE_RADAR,
                labelRes = R.string.nav_radar,
                selectedIcon = Icons.Filled.Radar,
                unselectedIcon = Icons.Outlined.Radar,
                testTag = "nav_radar"
            ),
            NavDestinationItem(
                route = ROUTE_SETTINGS,
                labelRes = R.string.nav_settings,
                selectedIcon = Icons.Filled.Tune,
                unselectedIcon = Icons.Outlined.Tune,
                testTag = "nav_settings"
            )
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = CockpitSurface,
                        contentColor = TextPrimary
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RadarCyan,
                                    selectedTextColor = RadarCyan,
                                    indicatorColor = RadarCyanContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = CockpitSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = RadarCyan,
                                    selectedTextColor = RadarCyan,
                                    indicatorColor = RadarCyanContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DASHBOARD,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(ROUTE_DASHBOARD) {
                        DashboardScreen(
                            summary = summary,
                            rides = rides,
                            expenses = expenses,
                            shift = shift,
                            settings = settings,
                            permissions = permissions,
                            onToggleVoice = { viewModel.toggleVoiceEnabled() },
                            onToggleShift = { viewModel.toggleShift() },
                            onNavigateToRadar = { navigateTo(ROUTE_RADAR) },
                            onAddFuel = { amount, liters, desc ->
                                viewModel.addFuelExpense(amount, liters, desc)
                            },
                            onAddTollOrExtra = { type, desc, amount ->
                                viewModel.addTollOrExtra(type, desc, amount)
                            },
                            onUpdateOdometer = { start, end, mins ->
                                viewModel.updateOdometer(start, end, mins)
                            },
                            onAddManualRide = { platform, fare, km, mins ->
                                viewModel.addManualRide(platform, fare, km, mins)
                            },
                            onDeleteRide = { viewModel.deleteRide(it) },
                            onDeleteExpense = { viewModel.deleteExpense(it) }
                        )
                    }

                    composable(ROUTE_RADAR) {
                        RadarTestScreen(
                            permissions = permissions,
                            settings = settings,
                            diagnosticInfo = diagnosticInfo,
                            onToggleVoice = { viewModel.toggleVoiceEnabled() },
                            onTestVoice = { viewModel.testVoicePhrase() },
                            onSimulateRide = { platform, fare, pickupKm, tripKm, pickupMin, tripMin ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    permissions.overlayEnabled &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                viewModel.simulateTestRide(
                                    platform = platform,
                                    fare = fare,
                                    pickupKm = pickupKm,
                                    tripKm = tripKm,
                                    pickupMinutes = pickupMin,
                                    tripMinutes = tripMin
                                )
                            }
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            onSaveSettings = { viewModel.saveSettings(it) }
                        )
                    }
                }
            }
        }

        // In-app Floating Bubble Fallback when SYSTEM_ALERT_WINDOW is not yet granted
        // (When granted, FloatingBubbleService displays the exact same card over ANY app)
        var offsetX by remember(activeBubble?.offer?.detectedAt) { mutableFloatStateOf(0f) }
        var offsetY by remember(activeBubble?.offer?.detectedAt) { mutableFloatStateOf(0f) }

        AnimatedVisibility(
            visible = activeBubble != null && !viewModel.canShowSystemOverlay(),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            val evaluated = activeBubble
            if (evaluated != null) {
                Box(
                    modifier = Modifier.background(Color.Transparent)
                ) {
                    FloatingRideBubbleCard(
                        evaluated = evaluated,
                        isVoiceEnabled = settings.voiceEnabled,
                        onToggleVoice = { viewModel.toggleVoiceEnabled() },
                        onAcceptRide = { viewModel.acceptActiveRide() },
                        onDismiss = { viewModel.dismissBubble() },
                        onDragDelta = { dx, dy ->
                            offsetX += dx
                            offsetY += dy
                        },
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

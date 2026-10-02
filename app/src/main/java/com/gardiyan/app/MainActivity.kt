package com.gardiyan.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gardiyan.app.navigation.*
import com.gardiyan.app.service.AccessibilityHealthMonitor
import com.gardiyan.app.service.BlockOverlayService
import com.gardiyan.app.ui.components.BottomBarItem
import com.gardiyan.app.ui.components.LimitraBottomBar
import com.gardiyan.app.ui.components.LimitraIcons
import com.gardiyan.app.ui.screens.AchievementCelebration
import com.gardiyan.app.ui.screens.LocalAchievements
import com.gardiyan.app.ui.screens.rememberAchievementController
import com.gardiyan.app.ui.theme.*
import com.gardiyan.app.viewmodel.GuardianViewModel
import com.gardiyan.app.viewmodel.GuardianViewModelFactory
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MatteSurface
                ) {
                    AppRoot()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Limitra her zaman kilit ekranından çıkış ve kısıtlama yönetimi yolu olmalıdır.
        BlockOverlayService.forceHideLockOverlay("Limitra MainActivity onResume")
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val viewModel: GuardianViewModel = viewModel(
        factory = GuardianViewModelFactory(context.applicationContext)
    )

    // Oturum canlılığını sağla
    LaunchedEffect(Unit) {
        viewModel.ensureServiceAlive(context.applicationContext)
    }

    MainNavigationContent(viewModel = viewModel)
}

@Composable
fun MainNavigationContent(
    viewModel: GuardianViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    var isOverlayEnabled by remember { mutableStateOf(viewModel.hasOverlayPermission(context)) }
    var isUsageEnabled by remember { mutableStateOf(viewModel.hasUsageStatsPermission(context)) }
    var accessibilityStatus by remember { mutableStateOf(viewModel.getAccessibilityServiceStatus(context)) }
    var isBatteryExempted by remember { mutableStateOf(viewModel.isBatteryOptimizationIgnored(context)) }
    var isNotificationsEnabled by remember { mutableStateOf(viewModel.areNotificationsEnabled(context)) }
    var hasCompletedInitialPermissionGate by remember {
        mutableStateOf(isInitialPermissionGateCompleted(context))
    }
    val restrictedApps by viewModel.restrictedApps.collectAsState()
    val isAccessibilityEnabled = accessibilityStatus.isOperational
    val isAccessibilityFailSafeActive = accessibilityStatus.hasFailSafeProtection

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val o = viewModel.hasOverlayPermission(context)
                val u = viewModel.hasUsageStatsPermission(context)
                val newAccessibilityStatus = viewModel.getAccessibilityServiceStatus(context)
                val oldAccessibilityOperational = accessibilityStatus.isOperational
                val oldAccessibilityNeedsReenable = accessibilityStatus.requiresReenable
                val a = newAccessibilityStatus.isOperational
                val b = viewModel.isBatteryOptimizationIgnored(context)
                val n = viewModel.areNotificationsEnabled(context)

                if (o != isOverlayEnabled) {
                    if (o && !isOverlayEnabled) {
                        android.widget.Toast.makeText(context, "✓ " + context.getString(R.string.perm_overlay_title), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    viewModel.logCriticalAction("PERMISSION_CHANGED", "Sistem İzinleri", "Diğer uygulamaların üzerinde çizim izni: " + if (o) "VERİLDİ" else "ALINDI")
                    isOverlayEnabled = o
                }
                if (u != isUsageEnabled) {
                    if (u && !isUsageEnabled) {
                        android.widget.Toast.makeText(context, "✓ " + context.getString(R.string.perm_usage_access_title), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    viewModel.logCriticalAction("PERMISSION_CHANGED", "Sistem İzinleri", "Kullanım erişimi izni: " + if (u) "VERİLDİ" else "ALINDI")
                    isUsageEnabled = u
                }
                if (a != oldAccessibilityOperational || newAccessibilityStatus.requiresReenable != oldAccessibilityNeedsReenable) {
                    if (a && !oldAccessibilityOperational) {
                        android.widget.Toast.makeText(context, "✓ " + context.getString(R.string.perm_accessibility_title), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    viewModel.logCriticalAction("PERMISSION_CHANGED", "Sistem İzinleri", "Erişilebilirlik izni: " + if (a) "VERİLDİ" else "ALINDI")
                    if (newAccessibilityStatus.requiresReenable) {
                        viewModel.logCriticalAction(
                            "ACCESSIBILITY_HEALTH_WARNING",
                            "Sistem Izinleri",
                            context.getString(R.string.accessibility_health_log_reenable)
                        )
                    }
                    accessibilityStatus = newAccessibilityStatus
                }
                if (b != isBatteryExempted) {
                    if (b && !isBatteryExempted) {
                        android.widget.Toast.makeText(context, "✓ " + context.getString(R.string.perm_battery_title), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    viewModel.logCriticalAction("PERMISSION_CHANGED", "Sistem İzinleri", "Pil optimizasyonu muafiyeti: " + if (b) "VERİLDİ" else "ALINDI")
                    isBatteryExempted = b
                }
                if (n != isNotificationsEnabled) {
                    if (n && !isNotificationsEnabled) {
                        android.widget.Toast.makeText(context, "✓ " + context.getString(R.string.perm_notification_title), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    viewModel.logCriticalAction("PERMISSION_CHANGED", "Sistem İzinleri", "Bildirim izni: " + if (n) "VERİLDİ" else "ALINDI")
                    isNotificationsEnabled = n
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            accessibilityStatus = viewModel.getAccessibilityServiceStatus(context)
            delay(2_000L)
        }
    }

    val hasAllPermissions = hasRequiredSetupPermissions(
        isOverlayEnabled = isOverlayEnabled,
        isUsageEnabled = isUsageEnabled,
        isAccessibilityEnabled = isAccessibilityEnabled,
        isBatteryExempted = isBatteryExempted
    )
    val canOpenMainApp = canEnterMainApp(
        hasCompletedInitialPermissionGate = hasCompletedInitialPermissionGate,
        hasRequiredSetupPermissions = hasAllPermissions
    )
    val shouldForceInitialPermissionGate = shouldShowInitialPermissionGate(
        hasCompletedInitialPermissionGate = hasCompletedInitialPermissionGate,
        hasRequiredSetupPermissions = hasAllPermissions
    )

    LaunchedEffect(hasAllPermissions) {
        if (hasAllPermissions && !hasCompletedInitialPermissionGate) {
            markInitialPermissionGateCompleted(context)
            hasCompletedInitialPermissionGate = true
        }
    }

    LaunchedEffect(restrictedApps.isNotEmpty()) {
        if (restrictedApps.isNotEmpty() && !hasCompletedInitialPermissionGate) {
            markInitialPermissionGateCompleted(context)
            hasCompletedInitialPermissionGate = true
        }
    }

    LaunchedEffect(hasCompletedInitialPermissionGate, isAccessibilityEnabled) {
        if (hasCompletedInitialPermissionGate && !isAccessibilityEnabled) {
            AccessibilityHealthMonitor.maybeNotifyReenableRequired(context)
        }
    }

    LaunchedEffect(shouldForceInitialPermissionGate, canOpenMainApp, currentRoute) {
        if (shouldForceInitialPermissionGate) {
            if (currentRoute != ROUTE_PERMISSIONS) {
                navController.navigate(ROUTE_PERMISSIONS) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (canOpenMainApp && currentRoute == ROUTE_PERMISSIONS) {
            navController.navigate(ROUTE_DASHBOARD) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val showBottomBar = canOpenMainApp &&
        (currentRoute == ROUTE_DASHBOARD || currentRoute == ROUTE_PROTECTED || currentRoute == ROUTE_SETTINGS)
    val tabs = listOf(
        BottomBarItem(ROUTE_DASHBOARD, stringResource(R.string.nav_home), LimitraIcons.Home),
        BottomBarItem(ROUTE_PROTECTED, stringResource(R.string.nav_protected), LimitraIcons.Shield),
        BottomBarItem(ROUTE_SETTINGS, stringResource(R.string.nav_profile), LimitraIcons.Award)
    )

    val achievements = rememberAchievementController(viewModel)
    val celebration = achievements.pendingCelebration
    val showCelebration = celebration != null && showBottomBar
    var lastCelebration by remember { mutableStateOf(celebration) }
    if (celebration != null) lastCelebration = celebration

    CompositionLocalProvider(LocalAchievements provides achievements) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        LimitraBottomBar(
                            items = tabs,
                            selectedKey = currentRoute,
                            onSelect = { route ->
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                },
                containerColor = MatteSurface
            ) { innerPadding ->
                Box(modifier = modifier.padding(innerPadding)) {
                    AppNavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        isOverlayEnabled = isOverlayEnabled,
                        isUsageEnabled = isUsageEnabled,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        accessibilityNeedsReenable = accessibilityStatus.requiresReenable,
                        accessibilityFailSafeActive = isAccessibilityFailSafeActive,
                        isBatteryExempted = isBatteryExempted,
                        isNotificationsEnabled = isNotificationsEnabled,
                        canEnterMainApp = canOpenMainApp
                    )
                }
            }

            // Yeni kazanılan çerçevenin tam ekran kutlaması (ana sekmelerdeyken gösterilir).
            AnimatedVisibility(
                visible = showCelebration,
                enter = fadeIn(tween(380)),
                exit = fadeOut(tween(260))
            ) {
                lastCelebration?.let { tier ->
                    AchievementCelebration(
                        tier = tier,
                        streak = achievements.progress.currentStreak,
                        onEquip = {
                            achievements.equip(tier)
                            achievements.dismissCelebrations()
                        },
                        onDismiss = { achievements.dismissCelebrations() }
                    )
                }
            }
        }
    }
}

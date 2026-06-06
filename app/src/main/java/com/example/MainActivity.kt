package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.data.local.entity.RestrictedAppEntity
import com.example.data.local.entity.StatusLogEntity
import com.example.data.local.entity.UserSessionEntity
import com.example.ui.theme.*
import com.example.viewmodel.GuardianViewModel
import com.example.viewmodel.GuardianViewModelFactory
import com.example.service.BlockOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val ROUTE_PERMISSIONS = "permissions"
const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_SETUP = "setup"
const val ROUTE_SETTINGS = "settings"

private const val CANCEL_HOLD_DURATION_MS = 5000L

@Composable
fun AppIconView(packageName: String, modifier: Modifier = Modifier) {
    if (packageName.isEmpty()) {
        Box(modifier = modifier)
        return
    }
    val context = LocalContext.current
    var iconDrawable by remember(packageName) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val icon = pm.getApplicationIcon(packageName)
                iconDrawable = icon
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    if (iconDrawable != null) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            },
            modifier = modifier,
            update = { imageView ->
                imageView.setImageDrawable(iconDrawable)
            }
        )
    } else {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageResource(android.R.drawable.sym_def_app_icon)
                }
            },
            modifier = modifier
        )
    }
}

class MainActivity : ComponentActivity() {
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
    var isAccessibilityEnabled by remember { mutableStateOf(viewModel.isAccessibilityServiceEnabled(context)) }
    var isBatteryExempted by remember { mutableStateOf(viewModel.isBatteryOptimizationIgnored(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            isOverlayEnabled = viewModel.hasOverlayPermission(context)
            isUsageEnabled = viewModel.hasUsageStatsPermission(context)
            isAccessibilityEnabled = viewModel.isAccessibilityServiceEnabled(context)
            isBatteryExempted = viewModel.isBatteryOptimizationIgnored(context)
            kotlinx.coroutines.delay(1000)
        }
    }

    val hasAllPermissions = isOverlayEnabled && isUsageEnabled && isAccessibilityEnabled && isBatteryExempted

    LaunchedEffect(hasAllPermissions) {
        if (!hasAllPermissions) {
            if (currentRoute != ROUTE_PERMISSIONS) {
                navController.navigate(ROUTE_PERMISSIONS) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (currentRoute == ROUTE_PERMISSIONS) {
            navController.navigate(ROUTE_DASHBOARD) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (hasAllPermissions && (currentRoute == ROUTE_DASHBOARD || currentRoute == ROUTE_SETTINGS)) {
                NavigationBar(
                    containerColor = DarkCharcoal,
                    contentColor = PureWhite
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Ana Ekran", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                        selected = currentRoute == ROUTE_DASHBOARD,
                        onClick = {
                            if (currentRoute != ROUTE_DASHBOARD) {
                                navController.navigate(ROUTE_DASHBOARD) {
                                    popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PureWhite,
                            selectedTextColor = PureWhite,
                            indicatorColor = SoftDangerRed,
                            unselectedTextColor = MutedGray,
                            unselectedIconColor = MutedGray
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings/Profile") },
                        label = { Text("Profil & Ayarlar", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                        selected = currentRoute == ROUTE_SETTINGS,
                        onClick = {
                            if (currentRoute != ROUTE_SETTINGS) {
                                navController.navigate(ROUTE_SETTINGS) {
                                    popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PureWhite,
                            selectedTextColor = PureWhite,
                            indicatorColor = SoftDangerRed,
                            unselectedTextColor = MutedGray,
                            unselectedIconColor = MutedGray
                        )
                    )
                }
            }
        },
        containerColor = MatteSurface
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (hasAllPermissions) ROUTE_DASHBOARD else ROUTE_PERMISSIONS,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(ROUTE_PERMISSIONS) {
                PermissionsScreen(
                    viewModel = viewModel,
                    isOverlayEnabled = isOverlayEnabled,
                    isUsageEnabled = isUsageEnabled,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    isBatteryExempted = isBatteryExempted,
                    onNavigateToDashboard = {
                        navController.navigate(ROUTE_DASHBOARD) {
                            popUpTo(ROUTE_PERMISSIONS) { inclusive = true }
                        }
                    }
                )
            }

            composable(ROUTE_DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSetup = { navController.navigate(ROUTE_SETUP) }
                )
            }

            composable(ROUTE_SETUP) {
                SetupTargetScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onCompleted = {
                        navController.navigate(ROUTE_DASHBOARD) {
                            popUpTo(ROUTE_DASHBOARD) { inclusive = true }
                        }
                    }
                )
            }

            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

// ==========================================
// SCREEN 1: PERMISSIONS
// ==========================================
@Composable
fun PermissionsScreen(
    viewModel: GuardianViewModel,
    isOverlayEnabled: Boolean,
    isUsageEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    isBatteryExempted: Boolean,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val hasAllPermissions = isOverlayEnabled && isUsageEnabled && isAccessibilityEnabled && isBatteryExempted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Üst Durum Kalkanı (Asimetrik ve Minimalist Koruma Halkası)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (hasAllPermissions) SuccessGreen.copy(alpha = 0.05f) else DangerRed.copy(alpha = 0.03f))
                    .border(1.5.dp, if (hasAllPermissions) SuccessGreen.copy(alpha = 0.3f) else DangerRed.copy(alpha = 0.15f), CircleShape)
            )
            Icon(
                imageVector = if (hasAllPermissions) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (hasAllPermissions) SuccessGreen else DangerRed,
                modifier = Modifier.size(44.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "GÜVENLİK YETKİLERİ",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = PureBlack,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Gardiyan'ın arka planda kararlı ve engellenemez çalışması için aşağıdaki sistem izinlerini aktif edin.",
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = MutedGray,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 2x2 Bento Grid İzin Kartları
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoPermissionCard(
                    title = "Kullanım",
                    description = "Uygulama açılış tespiti için yedek katman.",
                    isGranted = isUsageEnabled,
                    onClick = { viewModel.openUsageStatsSettings(context) },
                    modifier = Modifier.weight(1f)
                )
                BentoPermissionCard(
                    title = "Erişilebilirlik",
                    description = "Birincil ve anlık kilit ekranı tetikleyicisi.",
                    isGranted = isAccessibilityEnabled,
                    onClick = { viewModel.openAccessibilitySettings(context) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoPermissionCard(
                    title = "Gösterim",
                    description = "Kısıtlı ekranlarda blok overlay arayüzü çizimi.",
                    isGranted = isOverlayEnabled,
                    onClick = { viewModel.openOverlaySettings(context) },
                    modifier = Modifier.weight(1f)
                )
                BentoPermissionCard(
                    title = "Pil Muafiyeti",
                    description = "OEM batarya yöneticisi sonlandırma koruması.",
                    isGranted = isBatteryExempted,
                    onClick = { viewModel.requestBatteryOptimizationIgnore(context) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Başlat Butonu
        Button(
            onClick = { onNavigateToDashboard() },
            enabled = hasAllPermissions,
            colors = ButtonDefaults.buttonColors(
                containerColor = PureWhite,
                contentColor = Color.White,
                disabledContainerColor = BorderGray,
                disabledContentColor = MutedGray.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "KORUMA PANELİNİ BAŞLAT",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun BentoPermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(134.dp)
            .border(1.dp, if (isGranted) SuccessGreen.copy(alpha = 0.2f) else BorderGray, RoundedCornerShape(20.dp))
            .clickable(enabled = !isGranted) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) SuccessGreen.copy(alpha = 0.03f) else DarkCharcoal
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) SuccessGreen else DangerRed)
                )
            }

            Text(
                text = description,
                fontSize = 10.sp,
                color = MutedGray,
                lineHeight = 14.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp)
            )

            Text(
                text = if (isGranted) "AKTİF" else "YETKİ VER",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                color = if (isGranted) SuccessGreen else DangerRed,
                letterSpacing = 0.5.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

// ==========================================
// SCREEN 2: DASHBOARD — Modern Kontrol Paneli
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: GuardianViewModel,
    onNavigateToSetup: () -> Unit
) {
    val session by viewModel.userSession.collectAsState()
    val restrictedApps by viewModel.restrictedApps.collectAsState()
    val activeApps = remember(restrictedApps) { restrictedApps.filter { it.isActive } }
    val isMonitoring by viewModel.isMonitoringActive.collectAsState()

    Scaffold(
        containerColor = MatteSurface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // — HEADER —
            item {
                Spacer(modifier = Modifier.height(16.dp))
                DashboardHeader(session = session, activeCount = activeApps.size, isMonitoring = isMonitoring)
            }

            // — CTA: Yeni Kısıtlama Başlat —
            item {
                NewRestrictionCTA(onNavigateToSetup = onNavigateToSetup)
            }

            // — Aktif Kısıtlamalar Listesi —
            if (activeApps.isEmpty()) {
                item {
                    EmptyDashboardState()
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KORUNAN UYGULAMALAR",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MutedGray,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PureWhite.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${activeApps.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = PureWhite
                            )
                        }
                    }
                }

                items(activeApps, key = { it.id }) { app ->
                    ModernRestrictionCard(app = app)
                }

                // 5 saniye basılı tut iptal butonu
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    FiveSecondHoldCancelButton(
                        onCancelConfirmed = { viewModel.cancelAllWithFiveSecondHold() }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// — Dashboard Header —
@Composable
private fun DashboardHeader(
    session: UserSessionEntity?,
    activeCount: Int,
    isMonitoring: Boolean
) {
    val levelName = when (session?.level ?: 1) {
        1 -> "Çaylak"
        2 -> "Disiplinli"
        3 -> "Usta"
        else -> "Çaylak"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "GARDİYAN",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = PureBlack,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dijital Güvenlik ve Koruma",
                    fontSize = 13.sp,
                    color = MutedGray
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isMonitoring) SuccessGreen.copy(alpha = 0.1f) else BorderGray)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isMonitoring) SuccessGreen else MutedGray)
                    )
                    Text(
                        text = if (isMonitoring) "AKTİF" else "PASİF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = if (isMonitoring) SuccessGreen else MutedGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Asimetrik Bento İstatistikler
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sol Taraf: Büyük Rütbe Kartı
            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .height(134.dp)
                    .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RÜTBE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = MutedGray,
                        letterSpacing = 0.5.sp
                    )
                    Column {
                        Text(
                            text = levelName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = PureBlack
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Level ${session?.level ?: 1}",
                            fontSize = 11.sp,
                            color = MutedGray
                        )
                    }
                }
            }

            // Sağ Taraf: Üst üste iki adet küçük Bento Kartı
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(134.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Üst: Başarı Serisi
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, if (session?.hasRedBadge == true) DangerRed.copy(alpha = 0.3f) else BorderGray, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SERİ",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedGray
                            )
                            Text(
                                text = "${session?.consecutiveSuccessDays ?: 0} Gün",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (session?.hasRedBadge == true) DangerRed else PureBlack
                            )
                        }
                        Text(text = "🔥", fontSize = 16.sp)
                    }
                }

                // Alt: Aktif Kilit
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "KİLİT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedGray
                            )
                            Text(
                                text = "$activeCount Uygulama",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack
                            )
                        }
                        Text(text = "🔒", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// — CTA Kartı —
@Composable
private fun NewRestrictionCTA(onNavigateToSetup: () -> Unit) {
    Card(
        onClick = onNavigateToSetup,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PureWhite.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PureWhite.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Yeni Kısıtlama Başlat",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Uygulama seç, süre sınırını belirle ve koru",
                    fontSize = 12.sp,
                    color = MutedGray,
                    lineHeight = 16.sp
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MutedGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// — Boş Dashboard —
@Composable
private fun EmptyDashboardState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Güvenli Sörf Aktif",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PureBlack
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Şu an kısıtlanmış hiçbir uygulama bulunmuyor. Yeni bir koruma başlatmak için yukarıdaki kartı kullanın.",
                fontSize = 12.sp,
                color = MutedGray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// — Modern Kısıtlama Kartı (Toggle YOK) —
@Composable
private fun ModernRestrictionCard(app: RestrictedAppEntity) {
    val totalSecs = app.remainingSecondsToday.coerceAtLeast(0)
    val isLocked = totalSecs <= 0

    // Süre kalan ilerleme (toplam günlük)
    val dailyProgress = if (app.dailyLimitMinutes > 0) {
        (app.remainingSecondsToday.toFloat() / (app.dailyLimitMinutes * 60).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val mm = totalSecs / 60
    val ss = totalSecs % 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.2.dp,
                if (isLocked) DangerRed.copy(alpha = 0.3f)
                else if (app.isFailed) DangerRed.copy(alpha = 0.3f)
                else SuccessGreen.copy(alpha = 0.2f),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Üst kısım: İkon + Ad + Durum
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // App icon with status ring
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { dailyProgress },
                        modifier = Modifier.size(56.dp),
                        color = if (isLocked) DangerRed else PureWhite,
                        trackColor = if (isLocked) DangerRed.copy(alpha = 0.1f) else BorderGray,
                        strokeWidth = 3.5.dp
                    )
                    AppIconView(
                        packageName = app.packageName,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Günlük limit: ${app.dailyLimitMinutes} dk",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MutedGray
                    )
                    if (app.activeDays.isNotEmpty() && app.activeDays != "Pzt,Sal,Çar,Per,Cum,Cmt,Paz") {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Günler: ${app.activeDays}",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MutedGray.copy(alpha = 0.8f)
                        )
                    }
                }

                // Durum badge'i
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isLocked -> SoftDangerRed
                                app.isFailed -> SoftDangerRed
                                else -> SuccessGreen.copy(alpha = 0.1f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when {
                            isLocked -> "KİLİTLİ"
                            app.isFailed -> "BAŞARISIZ"
                            else -> "AKTİF"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = when {
                            isLocked -> DangerRed
                            app.isFailed -> DangerRed
                            else -> SuccessGreen
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alt kısım: günlük süre ilerleme çubuğu ve Sayaç
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { dailyProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isLocked) DangerRed else PureWhite,
                    trackColor = BorderGray
                )
                Text(
                    text = String.format("%02d:%02d", mm, ss),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isLocked) DangerRed else PureBlack
                )
            }
        }
    }
}

// — 5 Saniye Basılı Tut İptal Butonu —
@Composable
private fun FiveSecondHoldCancelButton(
    onCancelConfirmed: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(completed) {
        if (completed) {
            Toast.makeText(context, "Korumalar devre dışı bırakıldı.", Toast.LENGTH_LONG).show()
            onCancelConfirmed()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DangerRed.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "GÜVENLİK İPTALİ",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = DangerRed,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tüm kısıtlamaları kaldırmak için butona 5 saniye basılı tutun.",
                fontSize = 12.sp,
                color = MutedGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ceza olarak level düşüşü ve kırmızı rozet uygulanacaktır.",
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                color = MutedGray.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 5 saniye basılı tutma butonu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isHolding) DangerRed.copy(alpha = 0.1f) else SoftDangerRed)
                    .border(
                        width = if (isHolding) 1.5.dp else 1.dp,
                        color = DangerRed.copy(alpha = if (isHolding) 0.8f else 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isHolding = true
                            progress = 0f
                            val steps = 50
                            val stepDelay = CANCEL_HOLD_DURATION_MS / steps
                            val timerJob = coroutineScope.launch {
                                for (i in 1..steps) {
                                    delay(stepDelay)
                                    if (!isHolding) return@launch
                                    progress = i / steps.toFloat()
                                }
                                completed = true
                                isHolding = false
                            }
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    if (event.changes.all { !it.pressed }) {
                                        isHolding = false
                                        progress = 0f
                                        timerJob.cancel()
                                        return@awaitEachGesture
                                    }
                                }
                            } finally {
                                timerJob.cancel()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // İlerleme çubuğu (soldan dolma)
                if (isHolding) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(DangerRed.copy(alpha = 0.2f))
                            .align(Alignment.CenterStart)
                    )
                }

                Text(
                    text = when {
                        completed -> "✓ KİLİTLER AÇILDI"
                        isHolding -> "BIRAKMAYIN · ${(progress * 5).toInt() + 1}s"
                        else -> "KORUMAYI KALDIR (5sn BASILI TUT)"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = if (isHolding) DangerRed else PureWhite
                )
            }
        }
    }
}

@Composable
fun SetupTargetScreen(
    viewModel: GuardianViewModel,
    onBack: () -> Unit,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val availableApps = remember { viewModel.getInstalledApps(context) }
    var selectedApp by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    var selectedDurationPreset by remember { mutableStateOf(60) }
    var customDurationText by remember { mutableStateOf("") }
    
    val daysOfWeek = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    var selectedDays by remember { mutableStateOf(daysOfWeek.toSet()) }

    var isAppSheetVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val restrictedApps by viewModel.restrictedApps.collectAsState()

    val presetChoices = listOf(
        Pair("Test (10sn)", 0),
        Pair("15 Dk", 15),
        Pair("30 Dk", 30),
        Pair("1 Saat", 60),
        Pair("2 Saat", 120)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MatteSurface)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkCharcoal)
                            .border(1.dp, BorderGray, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = PureBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "KISITLAMA EKLE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = PureBlack,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Bütünleşik Form Paneli (Tek bir Bento / Minimal Kart yapısı)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // BÖLÜM 1: Uygulama Seçimi
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PureWhite.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "UYGULAMA SEÇİN",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MatteSurface)
                                    .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
                                    .clickable { 
                                        searchQuery = ""
                                        isAppSheetVisible = true 
                                    }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (selectedApp != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            AppIconView(packageName = selectedApp!!.second, modifier = Modifier.size(36.dp))
                                            Column {
                                                Text(text = selectedApp!!.first, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                                                Text(text = selectedApp!!.second, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MutedGray)
                                            }
                                        }
                                    } else {
                                        Text("Korunacak uygulamayı seçin...", fontSize = 13.sp, color = MutedGray)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MutedGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BorderGray, thickness = 1.dp)

                        // BÖLÜM 2: Süre Sınırı
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PureWhite.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "GÜNLÜK KULLANIM LİMİTİ",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(presetChoices) { choice ->
                                    val isSelected = selectedDurationPreset == choice.second && customDurationText.isEmpty()
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (isSelected) PureWhite else MatteSurface)
                                            .border(1.dp, if (isSelected) PureWhite else BorderGray, RoundedCornerShape(999.dp))
                                            .clickable {
                                                selectedDurationPreset = choice.second
                                                customDurationText = ""
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = choice.first,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else PureBlack
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = customDurationText,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() }) {
                                        customDurationText = newValue
                                    }
                                },
                                label = { Text("Özel dakika sınırı girin", color = MutedGray, fontSize = 13.sp) },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PureWhite,
                                    unfocusedBorderColor = BorderGray,
                                    focusedContainerColor = MatteSurface,
                                    unfocusedContainerColor = MatteSurface,
                                    focusedTextColor = PureBlack,
                                    unfocusedTextColor = PureBlack
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        HorizontalDivider(color = BorderGray, thickness = 1.dp)

                        // BÖLÜM 3: Gün Seçimi
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PureWhite.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "KORUMA GÜNLERİ",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                daysOfWeek.forEach { day ->
                                    val isSelected = selectedDays.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) PureWhite else MatteSurface)
                                            .border(1.dp, if (isSelected) PureWhite else BorderGray, CircleShape)
                                            .clickable {
                                                selectedDays = if (isSelected) {
                                                    selectedDays - day
                                                } else {
                                                    selectedDays + day
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else PureBlack
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ekleme ve bitirme butonları
            item {
                val finalDuration = if (customDurationText.isNotEmpty()) {
                    customDurationText.toIntOrNull() ?: selectedDurationPreset
                } else {
                    selectedDurationPreset
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val sel = selectedApp
                            if (sel == null) {
                                Toast.makeText(context, "Önce bir uygulama seçin", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedDays.isEmpty()) {
                                Toast.makeText(context, "Lütfen en az bir gün seçin", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val daysStr = selectedDays.joinToString(",")
                            if (finalDuration == 0) {
                                viewModel.startQuickTest(context, sel.second, sel.first, testSeconds = 10, activeDays = daysStr)
                                Toast.makeText(context, "${sel.first} için hızlı test başlatıldı!", Toast.LENGTH_SHORT).show()
                                onCompleted()
                            } else {
                                viewModel.addRestrictedApp(sel.second, sel.first, finalDuration, activeDays = daysStr)
                                Toast.makeText(context, "${sel.first} başarıyla eklendi", Toast.LENGTH_SHORT).show()
                                selectedApp = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (finalDuration == 0) "HIZLI TESTİ BAŞLAT (10sn)" else "KISITLAMAYI AKTİFLEŞTİR",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (restrictedApps.any { it.isActive }) {
                        OutlinedButton(
                            onClick = onCompleted,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureBlack),
                            border = BorderStroke(1.5.dp, BorderGray),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Text(
                                text = "İŞLEMLERİ TAMAMLA & KORUMAYI AÇ",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Custom Uygulama Arama/Seçim Bottom Sheet Overlay
        if (isAppSheetVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { isAppSheetVisible = false }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(DarkCharcoal)
                    .border(1.dp, BorderGray, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Drag Indicator
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(BorderGray)
                            .align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UYGULAMA SEÇİN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif,
                            color = PureBlack
                        )
                        IconButton(
                            onClick = { isAppSheetVisible = false },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MatteSurface)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = PureBlack, modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Arama Kutusu
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Uygulama adı veya paket adı ara...", color = MutedGray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MutedGray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = BorderGray,
                            focusedContainerColor = MatteSurface,
                            unfocusedContainerColor = MatteSurface,
                            focusedTextColor = PureBlack,
                            unfocusedTextColor = PureBlack
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Filtrelenmiş Uygulamalar Listesi
                    val filteredApps = remember(searchQuery, availableApps) {
                        if (searchQuery.isBlank()) {
                            availableApps
                        } else {
                            availableApps.filter {
                                it.first.contains(searchQuery, ignoreCase = true) ||
                                it.second.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps) { app ->
                            val isSelected = selectedApp?.second == app.second
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) SuccessGreen.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        selectedApp = app
                                        isAppSheetVisible = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                AppIconView(packageName = app.second, modifier = Modifier.size(36.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.first,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = PureBlack
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = app.second,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MutedGray
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// SCREEN 4: SETTINGS
// ==========================================
@Composable
fun SettingsScreen(
    viewModel: GuardianViewModel
) {
    val session by viewModel.userSession.collectAsState()
    val logs by viewModel.allLogs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "KORUMA PROFİLİ",
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                color = PureBlack,
                letterSpacing = 0.5.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 1. Profil Hub (Görsel Avatar & XP Bar)
        item {
            val level = session?.level ?: 1
            val levelName = when (level) {
                1 -> "Çaylak"
                2 -> "Disiplinli"
                3 -> "Usta"
                else -> "Çaylak"
            }
            val shieldEmoji = when (level) {
                1 -> "🛡️"
                2 -> "🥈"
                3 -> "👑"
                else -> "🛡️"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Dairesi
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PureWhite.copy(alpha = 0.05f))
                            .border(1.5.dp, PureWhite.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Text(text = shieldEmoji, fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "LEVEL $level",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = MutedGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = levelName.uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = PureBlack,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // XP Bar (Bir sonraki level'a ilerleme simülasyonu)
                    val progress = when (level) {
                        1 -> 0.3f
                        2 -> 0.6f
                        3 -> 1.0f
                        else -> 0.3f
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Rütbe İlerlemesi", fontSize = 10.sp, color = MutedGray)
                            Text(text = if (level == 3) "Max Rütbe" else "XP: ${(progress * 100).toInt()}/100", fontSize = 10.sp, color = MutedGray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SuccessGreen,
                            trackColor = MatteSurface
                        )
                    }
                }
            }
        }

        // 2. Kalkan Sağlığı / Utanç Rozeti Durumu
        item {
            val hasBadge = session?.hasRedBadge == true
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.2.dp,
                        color = if (hasBadge) DangerRed.copy(alpha = 0.3f) else SuccessGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (hasBadge) DangerRed.copy(alpha = 0.08f) else SuccessGreen.copy(alpha = 0.08f))
                    ) {
                        Text(text = if (hasBadge) "⚠️" else "🛡️", fontSize = 22.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasBadge) "KALKAN HASARLI" else "GÜVENLİK MAKSİMUM",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = if (hasBadge) DangerRed else SuccessGreen,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasBadge) {
                                "Korumayı erken kaldırdığınız için ceza aldınız. Kalkanın onarılmasına ${session?.activeRedemptionDaysLeft ?: 0} gün kaldı."
                            } else {
                                "Sistem koruması kusursuz çalışıyor. Utanç rozetiniz bulunmamaktadır."
                            },
                            fontSize = 11.sp,
                            color = MutedGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 3. Başarı İstatistiği Bento
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "ARDIŞIK BAŞARILI GÜN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MutedGray, letterSpacing = 0.5.sp)
                        Text(text = "${session?.consecutiveSuccessDays ?: 0} Gün", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PureBlack)
                    }
                    Text(text = "🔥", fontSize = 24.sp)
                }
            }
        }

        // 4. Zaman Tüneli Başlığı
        if (logs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ZAMAN TÜNELİ",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = MutedGray,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
                )
            }

            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val logsToShow = logs.take(10)

            items(logsToShow.size) { index ->
                val log = logsToShow[index]
                val timeStr = timeFormat.format(java.util.Date(log.timestamp))

                val title = when (log.eventType) {
                    "RESTRICTION_ADDED" -> "${log.appName} kısıtlaması eklendi"
                    "RESTRICTION_REMOVED" -> "${log.appName} kısıtlaması kaldırıldı"
                    "QUICK_TEST_STARTED" -> "${log.appName} hızlı test başlatıldı"
                    "RESTRICTION_RESET" -> "${log.appName} sayacı sıfırlandı"
                    "RESET_HOLD_5S" -> "Tüm kilitler kaldırıldı"
                    "FAILURE" -> "${log.appName} limiti aşıldı!"
                    "SUCCESS" -> "Günlük başarı sağlandı!"
                    else -> log.eventType
                }

                val statusIcon = when (log.eventType) {
                    "FAILURE" -> "⚠️"
                    "RESET_HOLD_5S" -> "🔓"
                    "SUCCESS" -> "🏆"
                    "RESTRICTION_ADDED", "QUICK_TEST_STARTED" -> "➕"
                    else -> "ℹ️"
                }

                val statusColor = when (log.eventType) {
                    "FAILURE", "RESET_HOLD_5S" -> DangerRed
                    "SUCCESS" -> SuccessGreen
                    else -> MutedGray
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(statusColor.copy(alpha = 0.08f))
                        ) {
                            Text(text = statusIcon, fontSize = 16.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureBlack,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = timeStr,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MutedGray
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = log.details,
                                fontSize = 10.sp,
                                color = MutedGray,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// Header (eski — artık DashboardHeader kullanılıyor, geriye uyumluluk)
// ==========================================
@Composable
fun HeaderSection(session: UserSessionEntity?) {
    // DashboardHeader artık ana header. Bu fonksiyon geriye uyumluluk için korunuyor.
}

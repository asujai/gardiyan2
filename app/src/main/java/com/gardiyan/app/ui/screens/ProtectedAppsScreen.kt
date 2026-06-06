package com.gardiyan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.ui.components.AppIconView
import com.gardiyan.app.ui.components.ModernRestrictionCard
import com.gardiyan.app.ui.theme.*
import com.gardiyan.app.viewmodel.GuardianViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


enum class ProtectedFilter(val displayName: String) {
    ALL("Tümü"),
    ACTIVE("Aktif"),
    LOCKED("Kilitli"),
    REACHED_LIMIT("Limiti Dolanlar")
}

@Composable
fun ProtectedAppsScreen(
    viewModel: GuardianViewModel
) {
    val session by viewModel.userSession.collectAsState()
    val restrictedApps by viewModel.restrictedApps.collectAsState()
    val activeApps = remember(restrictedApps) { restrictedApps.filter { it.isActive } }

    var selectedFilter by remember { mutableStateOf(ProtectedFilter.ALL) }
    var selectedAppForManagement by remember { mutableStateOf<RestrictedAppEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val filteredApps = remember(activeApps, selectedFilter) {
        activeApps.filter { app ->
            val isLocked = app.remainingSecondsToday <= 0
            when (selectedFilter) {
                ProtectedFilter.ALL -> true
                ProtectedFilter.ACTIVE -> !isLocked
                ProtectedFilter.LOCKED -> isLocked
                ProtectedFilter.REACHED_LIMIT -> isLocked || app.isFailed
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MatteSurface,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Area
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "KORUNANLAR",
                                fontSize = 18.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                color = PureBlack,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Süre sınırı koyduğunuz uygulamaları takip edin.",
                                fontSize = 12.sp,
                                color = MutedGray
                            )
                        }
                    }
                }

                // Filter Buttons
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ProtectedFilter.values()) { filter ->
                            val isSelected = selectedFilter == filter
                            val chipBg = if (isSelected) PureBlack else DarkCharcoal
                            val chipText = if (isSelected) Color.White else PureBlack
                            val chipBorder = if (isSelected) PureBlack else BorderGray

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = filter.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = chipText
                                )
                            }
                        }
                    }
                }

                // App Cards or Empty State
                if (filteredApps.isEmpty()) {
                    item {
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
                                    Text(text = "🛡️", fontSize = 32.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No protected apps yet.",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureBlack
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Use the \"Start New Restriction\" card on the Home screen to add your first app.",
                                    fontSize = 11.sp,
                                    color = MutedGray,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredApps, key = { it.id }) { app ->
                        ModernRestrictionCard(
                            app = app,
                            onClick = {
                                selectedAppForManagement = app
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Management Bottom Sheet Overlay
        selectedAppForManagement?.let { app ->
            val latestApp = restrictedApps.firstOrNull { it.id == app.id } ?: app

            var limitMinutes by remember(app.id) { mutableStateOf(latestApp.dailyLimitMinutes) }
            val daysOfWeek = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
            var selectedDays by remember(app.id) {
                val shownDays = latestApp.nextDayActiveDays.ifEmpty { latestApp.activeDays }
                mutableStateOf(shownDays.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet())
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { selectedAppForManagement = null }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
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
                            text = "KISITLAMA YÖNETİMİ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif,
                            color = PureBlack
                        )
                        IconButton(
                            onClick = { selectedAppForManagement = null },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MatteSurface)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = PureBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Uygulama Bilgileri Kartı
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = MatteSurface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    AppIconView(packageName = latestApp.packageName, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = latestApp.appName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = latestApp.packageName,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MutedGray
                                        )
                                    }
                                }
                            }
                        }

                        // Durum ve Kalan Süre
                        item {
                            val totalSecs = latestApp.remainingSecondsToday.coerceAtLeast(0)
                            val mm = totalSecs / 60
                            val ss = totalSecs % 60
                            val isLocked = totalSecs <= 0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = MatteSurface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Kalan Süre",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack
                                        )
                                        Text(
                                            text = String.format("%02d:%02d", mm, ss),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isLocked) DangerRed else PureBlack
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Durum",
                                            fontSize = 13.sp,
                                            color = MutedGray
                                        )
                                        Text(
                                            text = when {
                                                isLocked -> "Limit Doldu"
                                                latestApp.isFailed -> "Denge Süreci"
                                                else -> "Korunuyor"
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isLocked || latestApp.isFailed -> DangerRed
                                                else -> SuccessGreen
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Günlük Limit Düzenleme
                        item {
                            val totalSecs = latestApp.remainingSecondsToday.coerceAtLeast(0)
                            val isLocked = totalSecs <= 0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = MatteSurface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Günlük Limit",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack
                                        )
                                        Text(
                                            text = "$limitMinutes dk",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(DarkCharcoal)
                                                .clickable { if (limitMinutes > 5) limitMinutes -= 5 }
                                                .border(1.dp, BorderGray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("-", color = PureBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Slider(
                                            value = limitMinutes.toFloat(),
                                            onValueChange = { 
                                                if (isLocked) {
                                                    if (it.toInt() <= latestApp.dailyLimitMinutes) {
                                                        limitMinutes = it.toInt()
                                                    }
                                                } else {
                                                    limitMinutes = it.toInt()
                                                }
                                            },
                                            valueRange = 5f..180f,
                                            steps = 34,
                                            enabled = !isLocked,
                                            colors = SliderDefaults.colors(
                                                thumbColor = PureBlack,
                                                activeTrackColor = PureBlack,
                                                inactiveTrackColor = BorderGray,
                                                disabledThumbColor = MutedGray,
                                                disabledActiveTrackColor = BorderGray
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(DarkCharcoal)
                                                .clickable(enabled = !isLocked) { if (limitMinutes < 180) limitMinutes += 5 }
                                                .border(1.dp, if (isLocked) BorderGray.copy(alpha = 0.5f) else BorderGray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", color = if (isLocked) MutedGray else PureBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (limitMinutes > latestApp.dailyLimitMinutes) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Limit increases will take effect from tomorrow.",
                                            color = MutedGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Aktif Günler
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = MatteSurface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Aktif Günler",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureBlack
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        daysOfWeek.forEach { day ->
                                            val isSelected = selectedDays.contains(day)
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) PureBlack else DarkCharcoal)
                                                    .border(1.dp, if (isSelected) PureBlack else BorderGray, CircleShape)
                                                    .clickable {
                                                        selectedDays = if (isSelected) {
                                                            if (selectedDays.size > 1) selectedDays - day else selectedDays
                                                        } else {
                                                            selectedDays + day
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = day,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else PureBlack
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Kısıtlamayı Tamamen Sil
                        item {
                            HoldToDeleteButton(
                                appName = latestApp.appName,
                                onDeleteConfirmed = {
                                    viewModel.removeRestrictedApp(latestApp.id)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("${latestApp.appName} kısıtlaması tamamen silindi.")
                                    }
                                    selectedAppForManagement = null
                                },
                                onHoldStarted = {
                                    viewModel.logCriticalAction(
                                        "CRITICAL_ACTION_STARTED",
                                        latestApp.appName,
                                        "${latestApp.appName} kısıtlamasını silme işlemi basılı tutularak başlatıldı."
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Kaydet / Kapat Butonları
                    Button(
                        onClick = {
                            val daysStr = selectedDays.joinToString(",")
                            viewModel.updateRestrictionSettings(latestApp.id, limitMinutes, daysStr)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Değişiklikler başarıyla kaydedildi.")
                            }
                            selectedAppForManagement = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "DEĞİŞİKLİKLERİ KAYDET",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldToDeleteButton(
    appName: String,
    onDeleteConfirmed: () -> Unit,
    onHoldStarted: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(completed) {
        if (completed) {
            onDeleteConfirmed()
        }
    }

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MutedGray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "KORUMA AYARI",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = MutedGray,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Bu uygulamanın kısıtlamasını kaldırmak için butona 5 saniye basılı tutun.",
                fontSize = 11.sp,
                color = MutedGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isHolding) BorderGray.copy(alpha = 0.1f) else MatteSurface)
                    .border(
                        width = 1.dp,
                        color = BorderGray,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isHolding = true
                            progress = 0f
                            onHoldStarted()
                            val steps = 50
                            val stepDelay = 5000L / steps
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
                if (isHolding) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(BorderGray.copy(alpha = 0.2f))
                            .align(Alignment.CenterStart)
                    )
                }

                Text(
                    text = when {
                        completed -> "✓ KALDIRILDI"
                        isHolding -> "BIRAKMAYIN · ${(progress * 5).toInt() + 1}s"
                        else -> "KISITLAMAYI KALDIR (5sn BASILI TUT)"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = PureBlack
                )
            }
        }
    }
}

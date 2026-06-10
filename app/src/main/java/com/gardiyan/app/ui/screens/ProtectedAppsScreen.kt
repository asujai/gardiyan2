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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.res.stringResource
import com.gardiyan.app.R
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.data.repository.GuardianRepository
import com.gardiyan.app.ui.components.AppIconView
import com.gardiyan.app.ui.components.ModernRestrictionCard
import com.gardiyan.app.ui.components.DurationWheelPicker
import com.gardiyan.app.ui.theme.*
import com.gardiyan.app.viewmodel.GuardianViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale


enum class ProtectedFilter(val titleRes: Int) {
    ALL(R.string.log_filter_all),
    ACTIVE(R.string.overlay_active),
    LOCKED(R.string.log_type_app_locked),
    REACHED_LIMIT(R.string.filter_limit_reached)
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = stringResource(R.string.protected_apps_title),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureBlack
                        )
                    }
                }

                // Filter Buttons
                item {
                    val filters = listOf(
                        ProtectedFilter.ACTIVE to stringResource(R.string.protected_tab_active_format, "Limitra"),
                        ProtectedFilter.REACHED_LIMIT to stringResource(R.string.protected_tab_reached_limit)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filters) { (filter, label) ->
                            val isSelected = selectedFilter == filter
                            val chipBg = if (isSelected) PureBlack else DarkCharcoal
                            val chipText = if (isSelected) OnPureBlack else PureBlack
                            val chipBorder = if (isSelected) PureBlack else BorderGray

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(99.dp))
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
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
                                    text = stringResource(R.string.protected_apps_empty),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureBlack
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.protected_apps_empty_desc),
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

            var limitHours by remember(app.id) { mutableStateOf(latestApp.dailyLimitMinutes / 60) }
            var limitMinsOnly by remember(app.id) { mutableStateOf(latestApp.dailyLimitMinutes % 60) }
            val daysOfWeek = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
            val daysMap = mapOf(
                "Pzt" to R.string.day_mon,
                "Sal" to R.string.day_tue,
                "Çar" to R.string.day_wed,
                "Per" to R.string.day_thu,
                "Cum" to R.string.day_fri,
                "Cmt" to R.string.day_sat,
                "Paz" to R.string.day_sun
            )
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
                            text = stringResource(R.string.protected_apps_mgmt),
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
                                contentDescription = stringResource(R.string.btn_close),
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
                                            text = stringResource(R.string.protected_apps_remaining_time),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack
                                        )
                                        Text(
                                            text = String.format(Locale.ROOT, "%02d:%02d", mm, ss),
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
                                            text = stringResource(R.string.perm_status),
                                            fontSize = 13.sp,
                                            color = MutedGray
                                        )
                                        Text(
                                            text = when {
                                                isLocked -> stringResource(R.string.protected_apps_limit_reached)
                                                latestApp.isFailed -> stringResource(R.string.protected_apps_discipline_process)
                                                else -> stringResource(R.string.status_protected)
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

                            val durationText = buildString {
                                if (limitHours > 0) {
                                    append(stringResource(R.string.protected_apps_hours, limitHours))
                                    append(" ")
                                }
                                append(stringResource(R.string.protected_apps_minutes, limitMinsOnly))
                            }

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
                                            text = stringResource(R.string.protected_apps_daily_limit),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack
                                        )
                                        Text(
                                            text = durationText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(if (isLocked) 0.5f else 1f)
                                            .pointerInput(isLocked) {
                                                if (isLocked) {
                                                    awaitPointerEventScope {
                                                        while (true) {
                                                            val event = awaitPointerEvent()
                                                            event.changes.forEach { it.consume() }
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        DurationWheelPicker(
                                            initialHours = limitHours,
                                            initialMinutes = limitMinsOnly,
                                            onDurationChanged = { h, m ->
                                                limitHours = h
                                                limitMinsOnly = m
                                            },
                                            modifier = Modifier.fillMaxWidth()
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
                                        text = stringResource(R.string.protected_apps_active_days),
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
                                                    text = stringResource(daysMap[day]!!),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) OnPureBlack else PureBlack
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
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.log_desc_restriction_deleted, latestApp.appName)
                                        )
                                    }
                                    selectedAppForManagement = null
                                },
                                onHoldStarted = {
                                    viewModel.logCriticalAction(
                                        "CRITICAL_ACTION_STARTED",
                                        latestApp.appName,
                                        context.getString(R.string.log_desc_critical_start, latestApp.appName)
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
                            val newLimitRaw = limitHours * 60 + limitMinsOnly
                            val newLimit = if (newLimitRaw <= 0) 5 else newLimitRaw
                            
                            if (newLimit > latestApp.dailyLimitMinutes) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.protected_apps_limit_error)
                                    )
                                }
                            } else {
                                viewModel.updateRestrictionSettings(latestApp.id, newLimit, daysStr)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.protected_apps_save_success)
                                    )
                                }
                                selectedAppForManagement = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.protected_apps_save_btn),
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
                    text = stringResource(R.string.protected_apps_settings),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = MutedGray,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.protected_apps_remove_instruction),
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
                    .background(if (isHolding) DangerRed.copy(alpha = 0.15f) else MatteSurface)
                    .border(
                        width = if (isHolding) 1.5.dp else 1.dp,
                        color = if (isHolding) DangerRed.copy(alpha = 0.5f) else BorderGray,
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
                            .background(DangerRed.copy(alpha = 0.25f))
                            .align(Alignment.CenterStart)
                    )
                }

                Text(
                    text = when {
                        completed -> stringResource(R.string.protected_apps_removed)
                        isHolding -> stringResource(R.string.protected_apps_dont_release, (progress * 5).toInt() + 1)
                        else -> stringResource(R.string.protected_apps_remove_btn)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = if (isHolding) DangerRed else PureBlack
                )
            }
        }
    }
}

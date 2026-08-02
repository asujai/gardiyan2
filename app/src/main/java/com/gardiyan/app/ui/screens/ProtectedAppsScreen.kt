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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.gardiyan.app.R
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.data.model.RestrictionSchedule
import com.gardiyan.app.data.model.isScheduledAt
import com.gardiyan.app.ui.components.AppIconView
import com.gardiyan.app.ui.components.DurationWheelPicker
import com.gardiyan.app.ui.components.localizedHours
import com.gardiyan.app.ui.components.localizedMinutes
import com.gardiyan.app.ui.theme.*
import com.gardiyan.app.viewmodel.GuardianViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private data class RestrictionGroupUi(
    val id: String,
    val name: String,
    val apps: List<RestrictedAppEntity>
) {
    val representative: RestrictedAppEntity get() = apps.first()
    val isLimitReached: Boolean get() = apps.any { it.remainingSecondsToday <= 0 || it.isFailed }
}

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
    val restrictedApps by viewModel.restrictedApps.collectAsState()
    val activeApps = remember(restrictedApps) { restrictedApps.filter { it.isActive } }
    val restrictionGroups = remember(activeApps) {
        activeApps
            .groupBy { it.restrictionGroupId.ifBlank { it.packageName } }
            .map { (groupId, apps) ->
                RestrictionGroupUi(
                    id = groupId,
                    name = apps.first().restrictionName.ifBlank { apps.first().appName },
                    apps = apps.sortedBy { it.appName.lowercase(Locale.getDefault()) }
                )
            }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    var selectedFilter by remember { mutableStateOf(ProtectedFilter.ALL) }
    var selectedAppForManagement by remember { mutableStateOf<RestrictedAppEntity?>(null) }
    var expandedGroupIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var scheduleClockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            scheduleClockMillis = System.currentTimeMillis()
        }
    }

    val filteredGroups = remember(restrictionGroups, selectedFilter) {
        restrictionGroups.filter { group ->
            val isLocked = group.isLimitReached
            when (selectedFilter) {
                ProtectedFilter.ALL -> true
                ProtectedFilter.ACTIVE -> !isLocked
                ProtectedFilter.LOCKED -> isLocked
                ProtectedFilter.REACHED_LIMIT -> isLocked
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
                if (filteredGroups.isEmpty()) {
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
                                val hasAppsOutsideFilter = activeApps.isNotEmpty()
                                Text(
                                    text = stringResource(
                                        if (hasAppsOutsideFilter) R.string.profile_timeline_filter_empty
                                        else R.string.protected_apps_empty
                                    ),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureBlack
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(
                                        if (hasAppsOutsideFilter) R.string.protected_apps_desc
                                        else R.string.protected_apps_empty_desc
                                    ),
                                    fontSize = 11.sp,
                                    color = MutedGray,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredGroups, key = { it.id }) { group ->
                        RestrictionGroupCard(
                            group = group,
                            nowMillis = scheduleClockMillis,
                            expanded = group.id in expandedGroupIds,
                            onToggle = {
                                expandedGroupIds = if (group.id in expandedGroupIds) {
                                    expandedGroupIds - group.id
                                } else {
                                    expandedGroupIds + group.id
                                }
                            },
                            onAppClick = { app -> selectedAppForManagement = app }
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
            // Not: Aktif gün seçimi yalnızca yeni kısıtlama oluştururken yapılabilir.
            // Mevcut bir kısıtlama düzenlenirken günler DEĞİŞTİRİLEMEZ; kullanıcı bugünü
            // pasifleştirip korumadan kaçamasın diye burada gün düzenleme arayüzü yoktur.
            // Kayıtlı aktif günler veritabanında korunur ve koruma onlara göre çalışır.

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
                                    append(context.localizedHours(limitHours))
                                    append(" ")
                                }
                                append(context.localizedMinutes(limitMinsOnly))
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
                            // Aktif günler düzenleme ekranında değiştirilemez: mevcut kayıtlı
                            // günleri olduğu gibi geçir (isActiveDaysChanged her zaman false olur,
                            // gün bilgisi korunur). Sadece günlük limit düzenlenebilir.
                            val daysStr = latestApp.nextDayActiveDays.ifEmpty { latestApp.activeDays }
                            val newLimit = limitHours * 60 + limitMinsOnly
                            
                            if (newLimit <= 0) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.setup_target_error_zero_duration)
                                    )
                                }
                            } else if (newLimit > latestApp.dailyLimitMinutes) {
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
private fun RestrictionGroupCard(
    group: RestrictionGroupUi,
    nowMillis: Long,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAppClick: (RestrictedAppEntity) -> Unit
) {
    val app = group.representative
    val isSingleApp = group.apps.size == 1
    val currentlyActive = group.apps.any { it.isScheduledAt(nowMillis) }
    val timeText = if (app.activeWindowEnabled) {
        String.format(
            Locale.ROOT,
            "%02d:%02d – %02d:%02d",
            app.activeStartMinutes / 60,
            app.activeStartMinutes % 60,
            app.activeEndMinutes / 60,
            app.activeEndMinutes % 60
        )
    } else {
        stringResource(R.string.protected_group_all_day)
    }
    val selectedDays = app.activeDays.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val daysText = if (selectedDays.isEmpty() || selectedDays.size == RestrictionSchedule.dayLabels.size) {
        stringResource(R.string.protected_group_every_day)
    } else {
        selectedDays.joinToString(" · ")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isSingleApp) {
                    AppRemainingProgress(
                        app = app,
                        modifier = Modifier.size(52.dp),
                        showPercentage = false
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.10f))
                            .border(1.5.dp, SuccessGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(23.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$timeText  ·  $daysText",
                        fontSize = 12.sp,
                        color = MutedGray,
                        maxLines = 2,
                        lineHeight = 16.sp
                    )
                    if (isSingleApp) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = stringResource(
                                R.string.protected_apps_time_left,
                                formatRemainingTime(app.remainingSecondsToday)
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (app.remainingSecondsToday <= 0) DangerRed else PureBlack
                        )
                    }
                }

                Surface(
                    color = if (group.isLimitReached) DangerRed.copy(alpha = 0.10f) else SuccessGreen.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text(
                        text = when {
                            group.isLimitReached -> stringResource(R.string.protected_apps_limit_reached)
                            currentlyActive -> stringResource(R.string.protected_group_active)
                            else -> stringResource(R.string.protected_group_scheduled)
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (group.isLimitReached) DangerRed else SuccessGreen
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MutedGray
                )
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = BorderGray)
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.protected_group_apps),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedGray,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = stringResource(R.string.protected_group_app_count, group.apps.size),
                        fontSize = 12.sp,
                        color = MutedGray
                    )
                    group.apps.forEach { protectedApp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MatteSurface)
                                .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
                                .clickable { onAppClick(protectedApp) }
                                .padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppIconView(
                                packageName = protectedApp.packageName,
                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = protectedApp.appName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureBlack
                                )
                                Text(
                                    text = if (protectedApp.remainingSecondsToday <= 0) {
                                        stringResource(R.string.protected_apps_limit_reached)
                                    } else {
                                        stringResource(
                                            R.string.protected_apps_time_left,
                                            formatRemainingTime(protectedApp.remainingSecondsToday)
                                        )
                                    },
                                    fontSize = 11.sp,
                                    color = if (protectedApp.remainingSecondsToday <= 0) DangerRed else MutedGray
                                )
                            }
                            AppRemainingProgress(
                                app = protectedApp,
                                modifier = Modifier.size(48.dp),
                                showPercentage = true
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRemainingProgress(
    app: RestrictedAppEntity,
    modifier: Modifier = Modifier,
    showPercentage: Boolean
) {
    val totalSeconds = (app.dailyLimitMinutes * 60).coerceAtLeast(1)
    val remainingSeconds = app.remainingSecondsToday.coerceIn(0, totalSeconds)
    val progress = remainingSeconds.toFloat() / totalSeconds.toFloat()
    val percentage = (progress * 100).toInt()
    val progressColor = if (remainingSeconds <= 0) DangerRed else SuccessGreen
    val progressDescription = stringResource(R.string.protected_apps_usage_progress, percentage)

    Box(
        modifier = modifier.semantics { contentDescription = progressDescription },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = progressColor,
            trackColor = BorderGray.copy(alpha = 0.45f),
            strokeWidth = 3.5.dp
        )
        if (showPercentage) {
            Text(
                text = "$percentage%",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = PureBlack
            )
        } else {
            AppIconView(
                packageName = app.packageName,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
            )
        }
    }
}

private fun formatRemainingTime(remainingSeconds: Int): String {
    val roundedMinutes = if (remainingSeconds <= 0) 0 else (remainingSeconds + 59) / 60
    return String.format(Locale.ROOT, "%02d:%02d", roundedMinutes / 60, roundedMinutes % 60)
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
                            val steps = 3000
                            val stepDelay = 300000L / steps
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
                        isHolding -> stringResource(R.string.protected_apps_dont_release, 300 - (progress * 300).toInt())
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

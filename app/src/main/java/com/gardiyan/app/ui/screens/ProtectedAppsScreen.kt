package com.gardiyan.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import com.gardiyan.app.ui.components.AnimatedSheet
import com.gardiyan.app.ui.components.Hairline
import com.gardiyan.app.ui.components.IconBadge
import com.gardiyan.app.ui.components.LimitraCard
import com.gardiyan.app.ui.components.LimitraIcons
import com.gardiyan.app.ui.components.LimitraPrimaryButton
import com.gardiyan.app.ui.components.ScreenHeader
import com.gardiyan.app.ui.components.StatusPill
import com.gardiyan.app.ui.components.entrance
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
import java.util.Calendar
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
import kotlinx.coroutines.Job
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
    // Tek uyarı kuralı: yeni mesaj öncekini iptal eder, kuyruk oluşmaz.
    // showSnackbar askıya alan bir çağrıdır; iptal edilmeyen her tıklama sıraya girer.
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    val showMessage: (String) -> Unit = { message ->
        snackbarJob?.cancel()
        snackbarJob = coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

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

    val played = remember { mutableSetOf<Int>() }
    // Kapanış animasyonu süresince son seçilen uygulama gösterilmeye devam eder.
    var lastManagedApp by remember { mutableStateOf<RestrictedAppEntity?>(null) }
    if (selectedAppForManagement != null) lastManagedApp = selectedAppForManagement

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = MatteSurface) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Area
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ScreenHeader(
                        title = stringResource(R.string.protected_apps_title),
                        subtitle = stringResource(R.string.protected_apps_desc),
                        modifier = Modifier.entrance(0, played)
                    )
                }

                // Filter Buttons
                item {
                    val filters = listOf(
                        ProtectedFilter.ACTIVE to stringResource(R.string.protected_tab_active_format, "Limitra"),
                        ProtectedFilter.REACHED_LIMIT to stringResource(R.string.protected_tab_reached_limit)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .entrance(1, played)
                    ) {
                        items(filters) { (filter, label) ->
                            val isSelected = selectedFilter == filter
                            val chipBg by animateColorAsState(if (isSelected) PureBlack else DarkCharcoal, label = "chipBg")
                            val chipText by animateColorAsState(if (isSelected) OnPureBlack else PureBlack, label = "chipText")
                            val chipBorder = if (isSelected) PureBlack else BorderGray

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(99.dp))
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 18.dp, vertical = 11.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
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
                        LimitraCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .entrance(2, played),
                            elevated = false
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 44.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconBadge(
                                    icon = LimitraIcons.Shield,
                                    tint = CopperAccent,
                                    size = 64.dp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                val hasAppsOutsideFilter = activeApps.isNotEmpty()
                                Text(
                                    text = stringResource(
                                        if (hasAppsOutsideFilter) R.string.profile_timeline_filter_empty
                                        else R.string.protected_apps_empty
                                    ),
                                    fontFamily = LimitraDisplay,
                                    fontSize = 22.sp,
                                    lineHeight = 26.sp,
                                    color = PureBlack,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(
                                        if (hasAppsOutsideFilter) R.string.protected_apps_desc
                                        else R.string.protected_apps_empty_desc
                                    ),
                                    fontSize = 13.sp,
                                    color = MutedGray,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(filteredGroups, key = { _, group -> group.id }) { index, group ->
                        RestrictionGroupCard(
                            group = group,
                            nowMillis = scheduleClockMillis,
                            expanded = group.apps.size > 1 && group.id in expandedGroupIds,
                            onToggle = {
                                if (group.apps.size == 1) {
                                    selectedAppForManagement = group.representative
                                } else {
                                    expandedGroupIds = if (group.id in expandedGroupIds) {
                                        expandedGroupIds - group.id
                                    } else {
                                        expandedGroupIds + group.id
                                    }
                                }
                            },
                            onAppClick = { app -> selectedAppForManagement = app },
                            modifier = Modifier
                                .animateItem()
                                .entrance(index + 2, played)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Management Bottom Sheet Overlay
        AnimatedSheet(
            visible = selectedAppForManagement != null,
            onDismiss = { selectedAppForManagement = null },
            heightFraction = 0.88f
        ) {
            val app = lastManagedApp ?: return@AnimatedSheet
            val locale = LocalConfiguration.current.locales[0]
            val latestApp = restrictedApps.firstOrNull { it.id == app.id } ?: app

            var limitHours by remember(app.id) { mutableStateOf(latestApp.dailyLimitMinutes / 60) }
            var limitMinsOnly by remember(app.id) { mutableStateOf(latestApp.dailyLimitMinutes % 60) }
            // Not: Aktif gün seçimi yalnızca yeni kısıtlama oluştururken yapılabilir.
            // Mevcut bir kısıtlama düzenlenirken günler DEĞİŞTİRİLEMEZ; kullanıcı bugünü
            // pasifleştirip korumadan kaçamasın diye burada gün düzenleme arayüzü yoktur.
            // Kayıtlı aktif günler veritabanında korunur ve koruma onlara göre çalışır.

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.protected_apps_mgmt).lowercase(locale)
                        .replaceFirstChar { it.titlecase(locale) },
                    fontFamily = LimitraDisplay,
                    fontSize = 24.sp,
                    color = PureBlack
                )
                IconButton(
                    onClick = { selectedAppForManagement = null },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MatteSurface)
                        .size(40.dp)
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Uygulama bilgisi ve kalan süre
                item {
                    val totalSecs = latestApp.remainingSecondsToday.coerceAtLeast(0)
                    val mm = totalSecs / 60
                    val ss = totalSecs % 60
                    val isLocked = totalSecs <= 0

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(MatteSurface)
                            .border(1.dp, BorderGray, RoundedCornerShape(22.dp))
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AppRemainingProgress(
                                app = latestApp,
                                modifier = Modifier.size(58.dp),
                                showPercentage = false
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = latestApp.appName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureBlack
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = latestApp.packageName,
                                    fontSize = 11.sp,
                                    color = MutedGray,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Hairline()
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.protected_apps_remaining_time),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MutedGray
                                )
                                Text(
                                    text = String.format(Locale.ROOT, "%02d:%02d", mm, ss),
                                    fontFamily = LimitraDisplay,
                                    fontSize = 40.sp,
                                    lineHeight = 44.sp,
                                    color = if (isLocked) DangerRed else PureBlack
                                )
                            }
                            StatusPill(
                                text = when {
                                    isLocked -> stringResource(R.string.protected_apps_limit_reached)
                                    latestApp.isFailed -> stringResource(R.string.protected_apps_discipline_process)
                                    else -> stringResource(R.string.status_protected)
                                },
                                color = when {
                                    isLocked || latestApp.isFailed -> DangerRed
                                    else -> SuccessGreen
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(MatteSurface)
                            .border(1.dp, BorderGray, RoundedCornerShape(22.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.protected_apps_daily_limit),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack
                            )
                            Text(
                                text = durationText,
                                fontFamily = LimitraDisplay,
                                fontSize = 20.sp,
                                color = CopperAccent
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

                // Kısıtlamayı Tamamen Sil
                item {
                    HoldToDeleteButton(
                        appName = latestApp.appName,
                        onDeleteConfirmed = {
                            viewModel.removeRestrictedApp(latestApp.id)
                            showMessage(context.getString(R.string.log_desc_restriction_deleted, latestApp.appName))
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

            // Kaydet butonu
            LimitraPrimaryButton(
                text = stringResource(R.string.protected_apps_save_btn),
                onClick = {
                    // Aktif günler düzenleme ekranında değiştirilemez: mevcut kayıtlı
                    // günleri olduğu gibi geçir (isActiveDaysChanged her zaman false olur,
                    // gün bilgisi korunur). Sadece günlük limit düzenlenebilir.
                    val daysStr = latestApp.nextDayActiveDays.ifEmpty { latestApp.activeDays }
                    val newLimit = limitHours * 60 + limitMinsOnly

                    if (newLimit <= 0) {
                        showMessage(context.getString(R.string.setup_target_error_zero_duration))
                    } else if (newLimit > latestApp.dailyLimitMinutes) {
                        showMessage(context.getString(R.string.protected_apps_limit_error))
                    } else {
                        viewModel.updateRestrictionSettings(latestApp.id, newLimit, daysStr)
                        showMessage(context.getString(R.string.protected_apps_save_success))
                        selectedAppForManagement = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        // Alt sayfa Scaffold'un üstüne çizildiği için host burada; aksi halde uyarı sayfa kapanana kadar görünmez.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun RestrictionGroupCard(
    group: RestrictionGroupUi,
    nowMillis: Long,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAppClick: (RestrictedAppEntity) -> Unit,
    modifier: Modifier = Modifier
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
    val dayResMap = mapOf(
        Calendar.MONDAY to R.string.day_mon,
        Calendar.TUESDAY to R.string.day_tue,
        Calendar.WEDNESDAY to R.string.day_wed,
        Calendar.THURSDAY to R.string.day_thu,
        Calendar.FRIDAY to R.string.day_fri,
        Calendar.SATURDAY to R.string.day_sat,
        Calendar.SUNDAY to R.string.day_sun
    )
    val localizedDayStrings = selectedDays.map { raw ->
        val calDay = RestrictionSchedule.normalizeToCalendarDay(raw)
        calDay?.let { dayResMap[it] }?.let { stringResource(it) } ?: raw
    }
    val daysText = if (selectedDays.isEmpty() || selectedDays.size >= RestrictionSchedule.dayLabels.size) {
        stringResource(R.string.protected_group_every_day)
    } else {
        localizedDayStrings.joinToString(" · ")
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "chevron"
    )

    LimitraCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isSingleApp) {
                AppRemainingProgress(
                    app = app,
                    modifier = Modifier.size(54.dp),
                    showPercentage = false
                )
            } else {
                IconBadge(
                    icon = LimitraIcons.Shield,
                    tint = if (group.isLimitReached) DangerRed else SuccessGreen,
                    size = 52.dp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$timeText  ·  $daysText",
                    fontSize = 12.sp,
                    color = MutedGray,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        text = when {
                            group.isLimitReached -> stringResource(R.string.protected_apps_limit_reached)
                            currentlyActive -> stringResource(R.string.protected_group_active)
                            else -> stringResource(R.string.protected_group_scheduled)
                        },
                        color = when {
                            group.isLimitReached -> DangerRed
                            currentlyActive -> SuccessGreen
                            else -> MutedGray
                        }
                    )
                    if (isSingleApp) {
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
            }

            Icon(
                imageVector = if (isSingleApp) LimitraIcons.ChevronRight else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MutedGray,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = if (isSingleApp) 0f else chevronRotation }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 400f)) + fadeIn(),
            exit = shrinkVertically(spring(stiffness = 600f)) + fadeOut()
        ) {
            Column {
                Hairline(modifier = Modifier.padding(horizontal = 16.dp))
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.protected_group_app_count, group.apps.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedGray
                    )
                    group.apps.forEach { protectedApp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MatteSurface)
                                .clickable { onAppClick(protectedApp) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppIconView(
                                packageName = protectedApp.packageName,
                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
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
                                    fontSize = 12.sp,
                                    color = if (protectedApp.remainingSecondsToday <= 0) DangerRed else MutedGray
                                )
                            }
                            AppRemainingProgress(
                                app = protectedApp,
                                modifier = Modifier.size(46.dp),
                                showPercentage = true
                            )
                            Icon(LimitraIcons.ChevronRight, contentDescription = null, tint = MutedGray, modifier = Modifier.size(18.dp))
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
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900),
        label = "remainingRing"
    )

    Box(
        modifier = modifier.semantics { contentDescription = progressDescription },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = progressColor,
            trackColor = BorderGray.copy(alpha = 0.6f),
            strokeWidth = 3.dp,
            strokeCap = StrokeCap.Round
        )
        if (showPercentage) {
            Text(
                text = "$percentage%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = PureBlack
            )
        } else {
            AppIconView(
                packageName = app.packageName,
                modifier = Modifier
                    .fillMaxSize(0.74f)
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
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(completed) {
        if (completed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDeleteConfirmed()
        }
    }
    val holdScale by animateFloatAsState(
        targetValue = if (isHolding) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "holdScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, DangerRed.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .background(DangerRed.copy(alpha = 0.04f))
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
                tint = DangerRed,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = stringResource(R.string.protected_apps_settings),
                fontSize = 11.sp,
                color = DangerRed,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.protected_apps_remove_instruction),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MutedGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer {
                    scaleX = holdScale
                    scaleY = holdScale
                }
                .clip(RoundedCornerShape(16.dp))
                .background(if (isHolding) DangerRed.copy(alpha = 0.10f) else DarkCharcoal)
                .border(
                    width = if (isHolding) 1.5.dp else 1.dp,
                    color = if (isHolding) DangerRed.copy(alpha = 0.6f) else DangerRed.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp)
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
                        .background(DangerRed.copy(alpha = 0.22f))
                        .align(Alignment.CenterStart)
                )
            }

            Text(
                text = when {
                    completed -> stringResource(R.string.protected_apps_removed)
                    isHolding -> stringResource(R.string.protected_apps_dont_release, 300 - (progress * 300).toInt())
                    else -> stringResource(R.string.protected_apps_remove_btn)
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DangerRed
            )
        }
    }
}

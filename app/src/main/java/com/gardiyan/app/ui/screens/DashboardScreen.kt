package com.gardiyan.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.gardiyan.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.data.achievements.AchievementProgress
import com.gardiyan.app.data.achievements.FrameTier
import com.gardiyan.app.data.model.AppUsageSummary
import com.gardiyan.app.data.model.UsagePeriod
import com.gardiyan.app.data.model.DayStatus
import com.gardiyan.app.data.model.DisciplineWindow
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.ui.components.DisciplineChain
import com.gardiyan.app.ui.components.DisciplineChainLink
import com.gardiyan.app.ui.components.DisciplineDayBox
import com.gardiyan.app.ui.components.FrameEmblem
import com.gardiyan.app.ui.components.LimitraCard
import com.gardiyan.app.ui.components.LimitraIcons
import com.gardiyan.app.ui.components.LimitraPrimaryButton
import com.gardiyan.app.ui.components.SectionHeader
import com.gardiyan.app.ui.components.StatusPill
import com.gardiyan.app.ui.components.UsageRankingSection
import com.gardiyan.app.ui.components.entrance
import com.gardiyan.app.ui.components.frameContentColor
import com.gardiyan.app.ui.components.nameRes
import com.gardiyan.app.ui.components.pressable
import com.gardiyan.app.ui.theme.DashboardDanger as DangerRed
import com.gardiyan.app.ui.theme.DashboardInk as PureBlack
import com.gardiyan.app.ui.theme.DashboardIvory as MatteSurface
import com.gardiyan.app.ui.theme.DashboardMuted as MutedGray
import com.gardiyan.app.ui.theme.DashboardSuccess as SuccessGreen
import com.gardiyan.app.ui.theme.CopperAccent
import com.gardiyan.app.ui.theme.LimitraDisplay
import com.gardiyan.app.ui.theme.WarmGray
import com.gardiyan.app.viewmodel.GuardianViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun DashboardScreen(
    viewModel: GuardianViewModel,
    onNavigateToSetup: () -> Unit,
    onNavigateToProtected: () -> Unit,
    onNavigateToUsageDetails: () -> Unit = {},
    onNavigateToDisciplineDetail: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {}
) {
    val session by viewModel.userSession.collectAsState()
    val restrictedApps by viewModel.restrictedApps.collectAsState()
    val logs by viewModel.allLogs.collectAsState()
    val activeApps = remember(restrictedApps) { restrictedApps.filter { it.isActive } }
    val appLimits = remember(activeApps) { activeApps.associate { it.packageName to it.dailyLimitMinutes } }
    val exceededPackages = remember(activeApps) { getExceededPackageNames(activeApps) }

    val totalSavedMillis by produceState<Long>(initialValue = 0L, key1 = restrictedApps) {
        value = withContext(Dispatchers.IO) {
            val averageList = viewModel.getUsageRanking(UsagePeriod.AVERAGE)
            val averageMap = averageList.associate { it.packageName to it.usageMillis }

            activeApps.sumOf { app ->
                val avgUsage = averageMap[app.packageName] ?: 0L
                val limitMillis = app.dailyLimitMinutes * 60_000L
                (avgUsage - limitMillis).coerceAtLeast(0L)
            }
        }
    }

    var selectedPeriod by remember { mutableStateOf(UsagePeriod.DAILY) }
    val periodUsage by produceState<List<AppUsageSummary>>(
        initialValue = emptyList(),
        key1 = selectedPeriod
    ) {
        value = withContext(Dispatchers.IO) { viewModel.getUsageRanking(selectedPeriod) }
    }
    val exceededCount = exceededPackages.size

    val todayHasViolation = remember(logs) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        logs.any { it.timestamp in todayStart..todayEnd && it.eventType in DayStatus.FAILURE_EVENT_TYPES }
    }

    // Aktif serinin başlangıç günü (Day 1): en eski log / kısıtlama oluşturma / oturum başlangıcı.
    // 100 günlük detay ekranıyla AYNI çapa; ana özet bunun kayan 21 günlük penceresidir.
    val startMillis = remember(logs, restrictedApps, session?.lastCheckedMillis) {
        val cal = Calendar.getInstance()
        val minTimestamp = buildList {
            logs.minOfOrNull { it.timestamp }?.let(::add)
            restrictedApps.minOfOrNull { it.createdAtMillis }?.let(::add)
            session?.lastCheckedMillis?.takeIf { it > 0L }?.let(::add)
        }.minOrNull()
        if (minTimestamp != null && minTimestamp < cal.timeInMillis) {
            cal.timeInMillis = minTimestamp
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    // Bugünün seri içindeki gün numarası (1 = serinin ilk günü).
    val todayDayNumber = remember(startMillis) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val diff = todayStart - startMillis
        val diffDays = if (diff < 0) 0 else ((diff + 2 * 60 * 60 * 1000) / (24 * 60 * 60 * 1000)).toInt()
        diffDays + 1
    }

    // 21 kutucuk: serinin kayan penceresi. Kutu 0 = pencerenin ilk günü (sol üst),
    // bugün ilk 21 günde 1..21. kutuda ilerler, sonra sağ alt (index 20) kutuda kalır.
    val dayStatuses = remember(logs, restrictedApps, todayHasViolation, startMillis, todayDayNumber) {
        val firstDay = DisciplineWindow.firstVisibleDayNumber(todayDayNumber)
        List(DisciplineWindow.SUMMARY_DAYS) { cellIndex ->
            val dayNum = firstDay + cellIndex
            val isToday = dayNum == todayDayNumber
            val isFuture = dayNum > todayDayNumber
            if (isFuture) {
                DayStatus.NONE
            } else {
                val calStart = Calendar.getInstance().apply {
                    timeInMillis = startMillis
                    add(Calendar.DAY_OF_YEAR, dayNum - 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val calEnd = Calendar.getInstance().apply {
                    timeInMillis = startMillis
                    add(Calendar.DAY_OF_YEAR, dayNum - 1)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val dayLogs = logs.filter { it.timestamp in calStart.timeInMillis..calEnd.timeInMillis }
                DayStatus.evaluate(
                    isFuture = false,
                    isToday = isToday,
                    hasActiveTargets = activeApps.isNotEmpty(),
                    todayHasViolation = todayHasViolation,
                    dayHasSuccessLog = dayLogs.any { it.eventType in DayStatus.SUCCESS_EVENT_TYPES },
                    dayHasFailureLog = dayLogs.any { it.eventType in DayStatus.FAILURE_EVENT_TYPES }
                )
            }
        }
    }

    val achievements = achievementController(viewModel)
    val played = remember { mutableSetOf<Int>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            DashboardGreeting(modifier = Modifier.entrance(0, played))
        }
        item {
            TodayOverviewCard(
                protectedCount = activeApps.size,
                exceededCount = exceededCount,
                todayHasViolation = todayHasViolation,
                progress = achievements.progress,
                equipped = achievements.equipped,
                onAddRestriction = onNavigateToSetup,
                onOpenAchievements = onNavigateToAchievements,
                modifier = Modifier.entrance(1, played)
            )
        }
        item {
            UsageRankingSection(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { selectedPeriod = it },
                usageItems = periodUsage,
                appLimits = appLimits,
                exceededPackages = exceededPackages,
                onSeeAll = onNavigateToUsageDetails,
                modifier = Modifier.entrance(2, played)
            )
        }
        item {
            DisciplineSummary(
                dayStatuses = dayStatuses,
                todayCellIndex = DisciplineWindow.todayCellIndex(todayDayNumber),
                onDetailClick = onNavigateToDisciplineDetail,
                modifier = Modifier.entrance(3, played)
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

internal fun getExceededPackageNames(activeApps: List<RestrictedAppEntity>): Set<String> =
    activeApps
        .asSequence()
        .filter { it.remainingSecondsToday <= 0 || it.isFailed }
        .mapTo(mutableSetOf()) { it.packageName }

/** Tarih ve günün saatine göre selamlama. */
@Composable
private fun DashboardGreeting(modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val now = remember { Calendar.getInstance() }
    val dateText = remember(locale) {
        java.text.SimpleDateFormat("EEEE, d MMMM", locale).format(now.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val greetingRes = when (now.get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> R.string.greeting_morning
        in 12..17 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = dateText,
            color = MutedGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(greetingRes),
            color = PureBlack,
            fontFamily = LimitraDisplay,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.6).sp
        )
    }
}

/**
 * Günün özeti: takılı çerçeve içinde güncel seri, bugünün durumu, koruma sayısı ve bir
 * sonraki çerçeveye kalan yol. Kart başarılar ekranına götürür.
 */
@Composable
private fun TodayOverviewCard(
    protectedCount: Int,
    exceededCount: Int,
    todayHasViolation: Boolean,
    progress: AchievementProgress,
    equipped: FrameTier?,
    onAddRestriction: () -> Unit,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        protectedCount == 0 -> MutedGray
        todayHasViolation || exceededCount > 0 -> DangerRed
        else -> SuccessGreen
    }
    val statusText = when {
        protectedCount == 0 -> stringResource(R.string.dashboard_today_idle)
        todayHasViolation || exceededCount > 0 -> stringResource(R.string.dashboard_today_violated)
        else -> stringResource(R.string.dashboard_today_clean)
    }
    LimitraCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(onClick = onOpenAchievements)
                .padding(start = 14.dp, end = 18.dp, top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FrameEmblem(
                tier = equipped,
                modifier = Modifier.size(120.dp)
            ) {
                val contentColor = if (equipped != null) frameContentColor(equipped, false) else PureBlack
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedCount(
                        value = progress.currentStreak,
                        color = contentColor
                    )
                    Icon(
                        imageVector = LimitraIcons.Flame,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.achievements_current_streak),
                    color = MutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = pluralStringResource(R.plurals.achievements_days, progress.currentStreak, progress.currentStreak),
                    color = PureBlack,
                    fontFamily = LimitraDisplay,
                    fontSize = 28.sp,
                    lineHeight = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusPill(text = statusText, color = statusColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildString {
                        append(stringResource(R.string.dashboard_active_protection_val, protectedCount))
                        if (exceededCount > 0) {
                            append(" · ")
                            append(stringResource(R.string.dashboard_exceeded_val, exceededCount))
                        }
                    },
                    color = MutedGray,
                    fontSize = 12.sp
                )
            }
        }
        progress.next?.let { next ->
            val animatedProgress by animateFloatAsState(
                targetValue = progress.progressToNext,
                animationSpec = tween(1000),
                label = "dashNext"
            )
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.achievements_next, stringResource(next.nameRes)),
                        modifier = Modifier.weight(1f),
                        color = PureBlack,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = pluralStringResource(R.plurals.achievements_days_left, progress.daysToNext, progress.daysToNext),
                        color = MutedGray,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(WarmGray)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(CopperAccent)
                    )
                }
            }
        }
        LimitraPrimaryButton(
            text = stringResource(R.string.dashboard_add_restriction),
            onClick = onAddRestriction,
            icon = LimitraIcons.Plus,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp)
        )
    }
}

/** Sayının değişiminde yukarı/aşağı kayan geçiş. */
@Composable
private fun AnimatedCount(value: Int, color: Color) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            } else {
                (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            }
        },
        label = "streakCount"
    ) { count ->
        Text(
            text = count.toString(),
            color = color,
            fontFamily = LimitraDisplay,
            fontSize = 34.sp,
            lineHeight = 36.sp
        )
    }
}

@Composable
private fun DisciplineSummary(
    dayStatuses: List<DayStatus>,
    todayCellIndex: Int,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayLabel = stringResource(R.string.timeline_today)
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.dashboard_discipline_summary),
            actionText = stringResource(R.string.usage_see_all),
            onAction = onDetailClick
        )
        Spacer(modifier = Modifier.height(12.dp))
        LimitraCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDetailClick
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { rowIndex ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            repeat(7) { colIndex ->
                                // Kayan pencere sırası: sol-üst = pencerenin ilk günü.
                                // Günler soldan sağa, satır bitince alt satırın solundan ilerler.
                                // "Bugün" kutusu (todayCellIndex) hafif çerçeveyle vurgulanır.
                                val cellIndex = rowIndex * 7 + colIndex
                                val status = dayStatuses.getOrNull(cellIndex) ?: DayStatus.NONE

                                // Ardışık başarılı günler halka ile bağlanır; kopuş
                                // (ihlal veya boş gün) bağın yokluğuyla görünür olur.
                                if (colIndex > 0) {
                                    val previousStatus =
                                        dayStatuses.getOrNull(cellIndex - 1) ?: DayStatus.NONE
                                    DisciplineChainLink(
                                        link = DisciplineChain.link(previousStatus, status),
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }

                                val cellColor = when (status) {
                                    DayStatus.SUCCESS -> SuccessGreen
                                    DayStatus.FAILURE -> DangerRed
                                    DayStatus.PROGRESS -> CopperAccent
                                    DayStatus.NONE -> WarmGray
                                }
                                DisciplineDayBox(
                                    fillColor = cellColor,
                                    isToday = cellIndex == todayCellIndex,
                                    todayContentDescription = todayLabel,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.discipline_summary_days_ago),
                        color = MutedGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(SuccessGreen, stringResource(R.string.discipline_summary_success))
                        LegendDot(DangerRed, stringResource(R.string.discipline_summary_failure))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            color = MutedGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

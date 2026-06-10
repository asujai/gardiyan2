package com.gardiyan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import com.gardiyan.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.data.model.AppUsageSummary
import com.gardiyan.app.data.model.UsagePeriod
import com.gardiyan.app.data.model.DayStatus
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.ui.components.UsageRankingSection
import com.gardiyan.app.ui.components.formatUsageDuration
import com.gardiyan.app.ui.theme.DashboardBorder as BorderGray
import com.gardiyan.app.ui.theme.DashboardCard as DarkCharcoal
import com.gardiyan.app.ui.theme.DashboardDanger as DangerRed
import com.gardiyan.app.ui.theme.DashboardInk as PureBlack
import com.gardiyan.app.ui.theme.DashboardIvory as MatteSurface
import com.gardiyan.app.ui.theme.DashboardMuted as MutedGray
import com.gardiyan.app.ui.theme.DashboardSoftDanger as SoftDangerRed
import com.gardiyan.app.ui.theme.DashboardSuccess as SuccessGreen
import com.gardiyan.app.ui.theme.CopperAccent
import com.gardiyan.app.ui.theme.OnPureBlack
import com.gardiyan.app.ui.theme.SoftCopper
import com.gardiyan.app.ui.theme.WarmGray
import com.gardiyan.app.ui.theme.WineAccent
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
    onNavigateToDisciplineDetail: () -> Unit = {}
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

    val dayBounds = remember {
        List(21) { index ->
            val daysAgo = 20 - index
            val calStart = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calEnd = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            calStart.timeInMillis to calEnd.timeInMillis
        }
    }

    val todayHasViolation = remember(restrictedApps) {
        restrictedApps.any { it.isActive && it.isFailed }
    }

    val dayStatuses = remember(logs, restrictedApps, todayHasViolation) {
        List(21) { index ->
            val start = dayBounds[index].first
            val end = dayBounds[index].second
            val isToday = (index == 20)
            val dayLogs = logs.filter { it.timestamp in start..end }
            
            val hasSuccess = dayLogs.any { it.eventType in setOf("SUCCESS", "DAILY_SUCCESS", "SUCCESS_DAY") }
            val hasFailure = dayLogs.any { it.eventType in setOf("FAILURE", "VIOLATION", "DAILY_FAILURE", "RESET_HOLD_5S", "CRITICAL_ACTION_COMPLETED") }
            
            when {
                hasFailure -> DayStatus.FAILURE
                hasSuccess -> DayStatus.SUCCESS
                isToday -> {
                    if (activeApps.isNotEmpty()) {
                        if (todayHasViolation) DayStatus.FAILURE else DayStatus.PROGRESS
                    } else {
                        DayStatus.NONE
                    }
                }
                else -> DayStatus.NONE
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            TodayOverviewCard(
                protectedCount = activeApps.size,
                exceededCount = exceededCount,
                onAddRestriction = onNavigateToSetup
            )
        }
        item {
            UsageRankingSection(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { selectedPeriod = it },
                usageItems = periodUsage,
                appLimits = appLimits,
                exceededPackages = exceededPackages,
                onSeeAll = onNavigateToUsageDetails
            )
        }
        item {
            DisciplineSummary(
                dayStatuses = dayStatuses,
                onDetailClick = onNavigateToDisciplineDetail
            )
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

internal fun getExceededPackageNames(activeApps: List<RestrictedAppEntity>): Set<String> =
    activeApps
        .asSequence()
        .filter { it.remainingSecondsToday <= 0 || it.isFailed }
        .mapTo(mutableSetOf()) { it.packageName }

@Composable
private fun TodayOverviewCard(
    protectedCount: Int,
    exceededCount: Int,
    onAddRestriction: () -> Unit
) {
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
                .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onAddRestriction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureBlack,
                    contentColor = OnPureBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "+ " + stringResource(R.string.dashboard_add_restriction),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DisciplineSummary(
    dayStatuses: List<DayStatus>,
    onDetailClick: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.dashboard_discipline_summary),
            color = PureBlack,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderGray, RoundedCornerShape(24.dp))
                .clickable { onDetailClick() },
            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { rowIndex ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            repeat(7) { colIndex ->
                                val cellIndex = rowIndex * 7 + colIndex
                                val status = dayStatuses.getOrNull(cellIndex) ?: DayStatus.NONE
                                val cellColor = when (status) {
                                    DayStatus.SUCCESS -> SuccessGreen
                                    DayStatus.FAILURE -> DangerRed
                                    DayStatus.PROGRESS -> CopperAccent
                                    DayStatus.NONE -> WarmGray
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(cellColor)
                                        .border(0.5.dp, BorderGray.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.discipline_summary_days_ago),
                        color = MutedGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                            Text(
                                text = stringResource(R.string.discipline_summary_success),
                                color = MutedGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(DangerRed)
                            )
                            Text(
                                text = stringResource(R.string.discipline_summary_failure),
                                color = MutedGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

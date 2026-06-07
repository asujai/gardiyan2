package com.gardiyan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.ui.components.UsageRankingSection
import com.gardiyan.app.ui.theme.DashboardBorder as BorderGray
import com.gardiyan.app.ui.theme.DashboardCard as DarkCharcoal
import com.gardiyan.app.ui.theme.DashboardDanger as DangerRed
import com.gardiyan.app.ui.theme.DashboardInk as PureBlack
import com.gardiyan.app.ui.theme.DashboardIvory as MatteSurface
import com.gardiyan.app.ui.theme.DashboardMuted as MutedGray
import com.gardiyan.app.ui.theme.DashboardSoftDanger as SoftDangerRed
import com.gardiyan.app.ui.theme.DashboardSuccess as SuccessGreen
import com.gardiyan.app.ui.theme.CopperAccent
import com.gardiyan.app.ui.theme.SoftCopper
import com.gardiyan.app.ui.theme.WarmGray
import com.gardiyan.app.ui.theme.WineAccent
import com.gardiyan.app.viewmodel.GuardianViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(
    viewModel: GuardianViewModel,
    onNavigateToSetup: () -> Unit,
    onNavigateToProtected: () -> Unit,
    onNavigateToUsageDetails: () -> Unit = {}
) {
    val session by viewModel.userSession.collectAsState()
    val restrictedApps by viewModel.restrictedApps.collectAsState()
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
    val levelName = when (session?.level ?: 1) {
        1 -> stringResource(R.string.level_rookie)
        2 -> stringResource(R.string.level_disciplined)
        3 -> stringResource(R.string.level_master)
        else -> stringResource(R.string.level_rookie)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            DashboardHeader()
        }
        item {
            TodayOverviewCard(
                totalSavedMillis = totalSavedMillis,
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
                levelName = levelName,
                streak = session?.consecutiveSuccessDays ?: 0,
                protectedCount = activeApps.size,
                onProtectedClick = onNavigateToProtected
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
private fun DashboardHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "LİMİTRA",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = PureBlack,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.dashboard_subtitle),
                fontSize = 13.sp,
                color = MutedGray
            )
        }
    }
}

@Composable
private fun TodayOverviewCard(
    totalSavedMillis: Long,
    protectedCount: Int,
    exceededCount: Int,
    onAddRestriction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dashboard_summary_title),
                        color = WineAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.9.sp
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    if (totalSavedMillis > 0L) {
                        Text(
                             text = formatSavedDuration(totalSavedMillis),
                             color = PureBlack,
                             fontSize = 30.sp,
                             fontWeight = FontWeight.Black
                        )
                        Text(
                             text = stringResource(R.string.dashboard_today_gain),
                             color = MutedGray,
                             fontSize = 11.sp
                        )
                    } else {
                        Text(
                             text = stringResource(R.string.dashboard_no_gain),
                             color = PureBlack,
                             fontSize = 26.sp,
                             fontWeight = FontWeight.Black
                        )
                        Text(
                             text = stringResource(R.string.dashboard_no_gain_desc),
                             color = MutedGray,
                             fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoftCopper),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CopperAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OverviewMetric(
                    label = stringResource(R.string.dashboard_active_protection),
                    value = stringResource(R.string.dashboard_active_protection_val, protectedCount),
                    modifier = Modifier.weight(1f)
                )
                OverviewMetric(
                    label = stringResource(R.string.dashboard_limit_status),
                    value = if (exceededCount > 0) stringResource(R.string.dashboard_exceeded_val, exceededCount) else stringResource(R.string.dashboard_no_exceed),
                    isWarning = exceededCount > 0,
                    modifier = Modifier.weight(1f)
                )
            }

            if (exceededCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.dashboard_exceeded_warning),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SoftDangerRed)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    color = DangerRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAddRestriction,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureBlack,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.size(7.dp))
                Text(
                    text = stringResource(R.string.dashboard_add_restriction),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OverviewMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (isWarning) SoftDangerRed else WarmGray)
            .padding(horizontal = 11.dp, vertical = 10.dp)
    ) {
        Text(text = label, color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = if (isWarning) DangerRed else PureBlack,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DisciplineSummary(
    levelName: String,
    streak: Int,
    protectedCount: Int,
    onProtectedClick: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.dashboard_discipline_summary),
            color = PureBlack,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(title = stringResource(R.string.dashboard_rank), value = levelName, modifier = Modifier.weight(1f))
            SummaryCard(title = stringResource(R.string.dashboard_streak), value = stringResource(R.string.dashboard_streak_val, streak), modifier = Modifier.weight(1f))
            SummaryCard(
                title = stringResource(R.string.dashboard_protected),
                value = stringResource(R.string.dashboard_protected_val, protectedCount),
                modifier = Modifier.weight(1f),
                onClick = onProtectedClick
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier
            .height(72.dp)
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = PureBlack, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun formatSavedDuration(savedMillis: Long): String {
    val totalMinutes = savedMillis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.duration_saved_hours_mins, hours, minutes)
    } else {
        stringResource(R.string.duration_saved_mins, minutes)
    }
}

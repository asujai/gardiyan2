package com.gardiyan.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.R
import com.gardiyan.app.data.model.AppUsageSummary
import com.gardiyan.app.data.model.UsagePeriod
import com.gardiyan.app.ui.components.Hairline
import com.gardiyan.app.ui.components.LimitraCard
import com.gardiyan.app.ui.components.ScreenHeader
import com.gardiyan.app.ui.components.UsageAppIcon
import com.gardiyan.app.ui.components.UsageEmptyState
import com.gardiyan.app.ui.components.UsagePeriodSelector
import com.gardiyan.app.ui.components.formatUsageDuration
import com.gardiyan.app.viewmodel.GuardianViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.gardiyan.app.ui.theme.DashboardInk as PureBlack
import com.gardiyan.app.ui.theme.DashboardIvory as MatteSurface
import com.gardiyan.app.ui.theme.DashboardMuted as MutedGray
import com.gardiyan.app.ui.theme.CopperAccent
import com.gardiyan.app.ui.theme.LimitraDisplay
import com.gardiyan.app.ui.theme.WarmGray

@Composable
fun UsageDetailsScreen(
    viewModel: GuardianViewModel,
    onBack: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(UsagePeriod.DAILY) }
    val periodUsage by produceState<List<AppUsageSummary>>(
        initialValue = emptyList(),
        key1 = selectedPeriod
    ) {
        value = withContext(Dispatchers.IO) { viewModel.getUsageRanking(selectedPeriod) }
    }

    val sortedUsage = remember(periodUsage) {
        periodUsage.sortedByDescending { it.usageMillis }
    }
    val maxUsage = sortedUsage.firstOrNull()?.usageMillis?.coerceAtLeast(1L) ?: 1L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            ScreenHeader(
                title = stringResource(R.string.usage_ranking_title),
                subtitle = stringResource(R.string.usage_ranking_desc),
                onBack = onBack
            )
        }
        item {
            UsagePeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { selectedPeriod = it }
            )
        }

        if (sortedUsage.isEmpty()) {
            item {
                UsageEmptyState()
            }
        } else {
            item {
                LimitraCard(modifier = Modifier.fillMaxWidth()) {
                    sortedUsage.forEachIndexed { index, item ->
                        if (index > 0) Hairline(modifier = Modifier.padding(start = 104.dp, end = 16.dp))
                        UsageDetailRow(
                            rank = index + 1,
                            item = item,
                            share = (item.usageMillis.toFloat() / maxUsage).coerceIn(0f, 1f)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UsageDetailRow(rank: Int, item: AppUsageSummary, share: Float) {
    val animatedShare by animateFloatAsState(
        targetValue = share,
        animationSpec = tween(durationMillis = 800, delayMillis = (rank * 40).coerceAtMost(400)),
        label = "usageShare"
    )
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            modifier = Modifier.width(30.dp),
            fontFamily = LimitraDisplay,
            fontSize = 20.sp,
            color = if (rank <= 3) CopperAccent else MutedGray
        )
        UsageAppIcon(packageName = item.packageName, appName = item.appName)
        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.appName,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = PureBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatUsageDuration(item.usageMillis),
                    color = PureBlack,
                    fontFamily = LimitraDisplay,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(WarmGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedShare)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CopperAccent.copy(alpha = if (rank <= 3) 1f else 0.55f))
                )
            }
        }
    }
}

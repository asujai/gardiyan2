package com.gardiyan.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.getValue
import com.gardiyan.app.ui.theme.IsDarkUi
import com.gardiyan.app.ui.theme.LimitraDisplay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.gardiyan.app.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.gardiyan.app.data.model.AppUsageSummary
import com.gardiyan.app.data.model.UsagePeriod
import com.gardiyan.app.ui.theme.DashboardBorder as BorderGray
import com.gardiyan.app.ui.theme.DashboardCard as DarkCharcoal
import com.gardiyan.app.ui.theme.DashboardDanger as DangerRed
import com.gardiyan.app.ui.theme.DashboardInk as PureBlack
import com.gardiyan.app.ui.theme.DashboardMuted as MutedGray
import com.gardiyan.app.ui.theme.DashboardSoftDanger as SoftDangerRed
import com.gardiyan.app.ui.theme.OnPureBlack
import com.gardiyan.app.ui.theme.CopperAccent
import com.gardiyan.app.ui.theme.SoftCopper
import com.gardiyan.app.ui.theme.WarmGray
import com.gardiyan.app.ui.theme.WineAccent

private const val MAX_VISIBLE_APPS = 3

@Composable
fun UsageRankingSection(
    selectedPeriod: UsagePeriod,
    onPeriodSelected: (UsagePeriod) -> Unit,
    usageItems: List<AppUsageSummary>,
    appLimits: Map<String, Int>,
    exceededPackages: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
    onSeeAll: () -> Unit = {}
) {
    val sortedItems = remember(usageItems) {
        usageItems.sortedByDescending { it.usageMillis }
    }
    val visibleItems = sortedItems.take(MAX_VISIBLE_APPS)
    val maxUsage = sortedItems.firstOrNull()?.usageMillis?.coerceAtLeast(1L) ?: 1L

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.usage_ranking_title),
            actionText = if (sortedItems.size > MAX_VISIBLE_APPS) stringResource(R.string.usage_see_all) else null,
            onAction = onSeeAll
        )
        Spacer(modifier = Modifier.height(12.dp))

        UsagePeriodSelector(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (visibleItems.isEmpty()) {
            UsageEmptyState()
        } else {
            LimitraCard(modifier = Modifier.fillMaxWidth()) {
                visibleItems.forEachIndexed { index, item ->
                    if (index > 0) {
                        Hairline(modifier = Modifier.padding(start = 70.dp, end = 16.dp))
                    }
                    UsageRankingRow(
                        rank = index + 1,
                        item = item,
                        limitMinutes = appLimits[item.packageName],
                        isLimitExceeded = item.packageName in exceededPackages,
                        progress = (item.usageMillis.toFloat() / maxUsage.toFloat()).coerceIn(0f, 1f)
                    )
                }
            }
        }
    }
}

/** Dönem seçici: seçili dönemin arkasında yumuşak bir hap kayar. */
@Composable
internal fun UsagePeriodSelector(
    selectedPeriod: UsagePeriod,
    onPeriodSelected: (UsagePeriod) -> Unit
) {
    val periods = UsagePeriod.entries
    val selectedIndex = periods.indexOf(selectedPeriod).coerceAtLeast(0)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(99.dp))
            .background(WarmGray)
            .padding(4.dp)
    ) {
        val segment = maxWidth / periods.size
        val offset by animateDpAsState(
            targetValue = segment * selectedIndex,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 450f),
            label = "periodOffset"
        )
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(segment)
                .height(36.dp)
                .shadow(if (IsDarkUi) 0.dp else 3.dp, RoundedCornerShape(99.dp))
                .clip(RoundedCornerShape(99.dp))
                .background(if (IsDarkUi) PureBlack.copy(alpha = 0.12f) else DarkCharcoal)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            periods.forEach { period ->
                val selected = period == selectedPeriod
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onPeriodSelected(period) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(period.labelResId),
                        color = if (selected) PureBlack else MutedGray,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageRankingRow(
    rank: Int,
    item: AppUsageSummary,
    limitMinutes: Int?,
    isLimitExceeded: Boolean,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900, delayMillis = 80 * rank),
        label = "usageProgress"
    )
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UsageAppIcon(packageName = item.packageName, appName = item.appName)
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    color = if (isLimitExceeded) DangerRed else PureBlack,
                    fontFamily = LimitraDisplay,
                    fontSize = 18.sp
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
                        .fillMaxWidth(animatedProgress)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (isLimitExceeded) DangerRed else CopperAccent)
                )
            }
            Spacer(modifier = Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = limitMinutes?.let { stringResource(R.string.usage_limit_prefix, formatMinutes(it)) } ?: stringResource(R.string.usage_limit_not_set),
                    color = MutedGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isLimitExceeded) {
                    Text(
                        text = stringResource(R.string.usage_limit_exceeded),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftDangerRed)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        color = DangerRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
internal fun UsageAppIcon(packageName: String, appName: String) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }.getOrNull()
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(SoftCopper),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = appName,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = appName.firstOrNull()?.uppercase() ?: "?",
                color = WineAccent,
                fontFamily = LimitraDisplay,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
internal fun UsageEmptyState() {
    LimitraCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBadge(icon = LimitraIcons.Hourglass, tint = CopperAccent, size = 52.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.usage_empty_title),
                color = PureBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.usage_empty_desc),
                color = MutedGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun formatUsageDuration(usageMillis: Long): String {
    val totalMinutes = (usageMillis / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val hourUnit = stringResource(R.string.unit_hour)
    val minuteUnit = stringResource(R.string.unit_minute)
    return when {
        hours > 0 && minutes > 0 -> "${hours}${hourUnit} ${minutes}${minuteUnit}"
        hours > 0 -> "${hours}${hourUnit}"
        else -> "${minutes}${minuteUnit}"
    }
}

@Composable
private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val hourUnit = stringResource(R.string.unit_hour)
    val minuteUnit = stringResource(R.string.unit_minute)
    return when {
        hours > 0 && minutes > 0 -> "${hours}${hourUnit} ${minutes}${minuteUnit}"
        hours > 0 -> "${hours}${hourUnit}"
        else -> "${minutes}${minuteUnit}"
    }
}

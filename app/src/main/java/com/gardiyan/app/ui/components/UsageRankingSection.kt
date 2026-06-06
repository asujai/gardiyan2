package com.gardiyan.app.ui.components

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

private const val MAX_VISIBLE_APPS = 6

@Composable
fun UsageRankingSection(
    selectedPeriod: UsagePeriod,
    onPeriodSelected: (UsagePeriod) -> Unit,
    usageItems: List<AppUsageSummary>,
    appLimits: Map<String, Int>,
    modifier: Modifier = Modifier,
    onSeeAll: () -> Unit = {}
) {
    val sortedItems = remember(usageItems) {
        usageItems.sortedByDescending { it.usageMillis }
    }
    val visibleItems = sortedItems.take(MAX_VISIBLE_APPS)
    val maxUsage = sortedItems.firstOrNull()?.usageMillis?.coerceAtLeast(1L) ?: 1L

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Kullanım Sıralaması",
            fontSize = 21.sp,
            fontWeight = FontWeight.Black,
            color = PureBlack
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "En çok zaman geçirdiğin uygulamaları takip et.",
            fontSize = 13.sp,
            color = MutedGray
        )
        Spacer(modifier = Modifier.height(14.dp))

        PeriodSelector(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (visibleItems.isEmpty()) {
            UsageEmptyState()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleItems.forEach { item ->
                    UsageRankingRow(
                        item = item,
                        limitMinutes = appLimits[item.packageName],
                        progress = (item.usageMillis.toFloat() / maxUsage.toFloat()).coerceIn(0f, 1f)
                    )
                }
            }
        }

        if (sortedItems.size > MAX_VISIBLE_APPS) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tümünü Gör",
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = WineAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: UsagePeriod,
    onPeriodSelected: (UsagePeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WarmGray)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        UsagePeriod.entries.forEach { period ->
            val selected = period == selectedPeriod
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) PureBlack else Color.Transparent)
                    .clickable { onPeriodSelected(period) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label,
                    color = if (selected) OnPureBlack else MutedGray,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun UsageRankingRow(
    item: AppUsageSummary,
    limitMinutes: Int?,
    progress: Float
) {
    val isLimitExceeded = limitMinutes != null && item.usageMillis > limitMinutes * 60_000L

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(17.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(17.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = item.packageName, appName = item.appName)
            Spacer(modifier = Modifier.width(11.dp))

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
                        color = PureBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatUsageDuration(item.usageMillis),
                        color = PureBlack,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
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
                            .fillMaxWidth(progress)
                            .height(4.dp)
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
                        text = limitMinutes?.let { "Limit: ${formatMinutes(it)}" } ?: "Limit tanımlı değil",
                        color = MutedGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (isLimitExceeded) {
                        Text(
                            text = "Limit aşıldı",
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftDangerRed)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            color = DangerRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String, appName: String) {
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
                contentDescription = "$appName ikonu",
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = appName.firstOrNull()?.uppercase() ?: "?",
                color = WineAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun UsageEmptyState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SoftCopper),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "—", color = CopperAccent, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Bugün henüz kullanım verisi oluşmadı.",
                color = PureBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Uygulamaları kullandıkça burada sıralaman görünecek.",
                color = MutedGray,
                fontSize = 11.sp
            )
        }
    }
}

fun formatUsageDuration(usageMillis: Long): String {
    val totalMinutes = (usageMillis / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}sa ${minutes}dk"
        hours > 0 -> "${hours}sa"
        else -> "${minutes}dk"
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}sa ${minutes}dk"
        hours > 0 -> "${hours}sa"
        else -> "${minutes}dk"
    }
}

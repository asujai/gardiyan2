package com.gardiyan.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.gardiyan.app.R
import com.gardiyan.app.data.model.AppUsageSummary
import com.gardiyan.app.data.model.UsagePeriod
import com.gardiyan.app.ui.components.formatUsageDuration
import com.gardiyan.app.viewmodel.GuardianViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.gardiyan.app.ui.theme.DashboardBorder as BorderGray
import com.gardiyan.app.ui.theme.DashboardCard as DarkCharcoal
import com.gardiyan.app.ui.theme.DashboardDanger as DangerRed
import com.gardiyan.app.ui.theme.DashboardInk as PureBlack
import com.gardiyan.app.ui.theme.DashboardIvory as MatteSurface
import com.gardiyan.app.ui.theme.DashboardMuted as MutedGray
import com.gardiyan.app.ui.theme.DashboardSoftDanger as SoftDangerRed
import com.gardiyan.app.ui.theme.CopperAccent
import com.gardiyan.app.ui.theme.SoftCopper
import com.gardiyan.app.ui.theme.WarmGray
import com.gardiyan.app.ui.theme.WineAccent
import com.gardiyan.app.ui.theme.OnPureBlack

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.usage_ranking_title),
                        fontWeight = FontWeight.Bold,
                        color = PureBlack,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back_desc),
                            tint = PureBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MatteSurface
                )
            )
        },
        containerColor = MatteSurface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it }
                )
            }

            if (sortedUsage.isEmpty()) {
                item {
                    UsageEmptyState()
                }
            } else {
                items(sortedUsage) { item ->
                    UsageDetailRow(item = item)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
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
            .border(1.dp, BorderGray, RoundedCornerShape(99.dp))
            .background(Color.Transparent)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        UsagePeriod.entries.forEach { period ->
            val selected = period == selectedPeriod
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (selected) PureBlack else Color.Transparent)
                    .clickable { onPeriodSelected(period) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(period.labelResId),
                    color = if (selected) OnPureBlack else PureBlack.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun UsageDetailRow(item: AppUsageSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = item.packageName, appName = item.appName)
            Spacer(modifier = Modifier.width(14.dp))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.appName,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    color = PureBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatUsageDuration(item.usageMillis),
                    color = PureBlack,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
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
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(SoftCopper),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = appName,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = appName.firstOrNull()?.uppercase() ?: "?",
                color = WineAccent,
                fontSize = 16.sp,
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
            .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SoftCopper),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "—", color = CopperAccent, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
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

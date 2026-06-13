package com.gardiyan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.R
import com.gardiyan.app.data.model.DayStatus
import com.gardiyan.app.viewmodel.GuardianViewModel
import java.util.Calendar
import com.gardiyan.app.ui.theme.DashboardBorder as BorderGray
import com.gardiyan.app.ui.theme.DashboardCard as DarkCharcoal
import com.gardiyan.app.ui.theme.DashboardDanger as DangerRed
import com.gardiyan.app.ui.theme.DashboardInk as PureBlack
import com.gardiyan.app.ui.theme.DashboardIvory as MatteSurface
import com.gardiyan.app.ui.theme.DashboardMuted as MutedGray
import com.gardiyan.app.ui.theme.DashboardSuccess as SuccessGreen
import com.gardiyan.app.ui.theme.CopperAccent
import com.gardiyan.app.ui.theme.WarmGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisciplineDetailScreen(
    viewModel: GuardianViewModel,
    onBack: () -> Unit
) {
    val logs by viewModel.allLogs.collectAsState()
    val restrictedApps by viewModel.restrictedApps.collectAsState()
    val session by viewModel.userSession.collectAsState()

    // 1. Başlangıç tarihini (Day 1) bul
    val startCalendar = remember(logs, restrictedApps, session?.lastCheckedMillis) {
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
        cal
    }
    val startMillis = startCalendar.timeInMillis

    // 2. Bugünün başlangıcını bul
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val todayStartMillis = todayStart.timeInMillis

    // 3. Toplam gün sayısını hesapla
    val diffDays = remember(startMillis, todayStartMillis) {
        val diff = todayStartMillis - startMillis
        if (diff < 0) 0 else ((diff + 2 * 60 * 60 * 1000) / (24 * 60 * 60 * 1000)).toInt()
    }

    val activeSeriesNumber = (diffDays / 100) + 1
    val completedSeriesCount = diffDays / 100

    var selectedSeries by remember { mutableStateOf<Int?>(null) }
    val currentViewingSeries = selectedSeries ?: activeSeriesNumber

    val startDayOfSeries = (currentViewingSeries - 1) * 100 + 1

    val todayHasViolation = remember(restrictedApps) {
        restrictedApps.any { it.isActive && it.isFailed }
    }
    val hasActiveApps = remember(restrictedApps) {
        restrictedApps.any { it.isActive }
    }

    // 100 günlük durum listesi
    val seriesDayStatuses = remember(logs, restrictedApps, todayHasViolation, hasActiveApps, currentViewingSeries, startMillis, diffDays) {
        List(100) { index ->
            val dayIndex = startDayOfSeries + index
            val isToday = (dayIndex == diffDays + 1)
            val isFuture = (dayIndex > diffDays + 1)

            if (isFuture) {
                DayStatus.NONE
            } else {
                val calStart = Calendar.getInstance().apply {
                    timeInMillis = startMillis
                    add(Calendar.DAY_OF_YEAR, dayIndex - 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val calEnd = Calendar.getInstance().apply {
                    timeInMillis = startMillis
                    add(Calendar.DAY_OF_YEAR, dayIndex - 1)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val start = calStart.timeInMillis
                val end = calEnd.timeInMillis

                val dayLogs = logs.filter { it.timestamp in start..end }
                val hasSuccess = dayLogs.any { it.eventType in setOf("SUCCESS", "DAILY_SUCCESS", "SUCCESS_DAY") }
                val hasFailure = dayLogs.any { it.eventType in setOf("FAILURE", "VIOLATION", "DAILY_FAILURE", "RESET_HOLD_5S", "CRITICAL_ACTION_COMPLETED") }

                when {
                    hasFailure -> DayStatus.FAILURE
                    hasSuccess -> DayStatus.SUCCESS
                    isToday -> {
                        if (hasActiveApps) {
                            if (todayHasViolation) DayStatus.FAILURE else DayStatus.PROGRESS
                        } else {
                            DayStatus.NONE
                        }
                    }
                    else -> DayStatus.NONE
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.discipline_detail_title),
                        fontWeight = FontWeight.Bold,
                        color = PureBlack,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            // Tamamlanan Seriler Başlığı ve İkonları
            if (completedSeriesCount > 0) {
                item {
                    Text(
                        text = stringResource(R.string.discipline_detail_completed_series),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(completedSeriesCount) { index ->
                            val seriesNum = index + 1
                            val isSelected = (selectedSeries == seriesNum)
                            Card(
                                modifier = Modifier
                                    .clickable { selectedSeries = seriesNum }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) CopperAccent else BorderGray,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) CopperAccent.copy(alpha = 0.15f) else DarkCharcoal
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "🛡️",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.discipline_detail_series_label, seriesNum),
                                        color = if (isSelected) CopperAccent else PureBlack,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Aktif Seri Seçeneği (Geçmiş bir seri seçildiğinde aktif seriye dönmek için bir buton gibi)
                        item {
                            val isSelected = (selectedSeries == null)
                            Card(
                                modifier = Modifier
                                    .clickable { selectedSeries = null }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) CopperAccent else BorderGray,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) CopperAccent.copy(alpha = 0.15f) else DarkCharcoal
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "🔥",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.discipline_detail_active_series) + " (${activeSeriesNumber})",
                                        color = if (isSelected) CopperAccent else PureBlack,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Görüntülenen Serinin Adı ve Bilgi Kartı
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(20.dp)
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
                                text = if (selectedSeries == null) {
                                    stringResource(R.string.discipline_detail_active_series) + " (${activeSeriesNumber})"
                                } else {
                                    stringResource(R.string.discipline_detail_series_label, currentViewingSeries)
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack
                              )

                            if (selectedSeries != null) {
                                Text(
                                    text = stringResource(R.string.discipline_detail_back_to_active),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CopperAccent,
                                    modifier = Modifier
                                        .clickable { selectedSeries = null }
                                        .padding(4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 100 Gün Izgarası
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            repeat(10) { rowIndex ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    repeat(10) { colIndex ->
                                        val cellIndex = rowIndex * 10 + colIndex
                                        val status = seriesDayStatuses.getOrNull(cellIndex) ?: DayStatus.NONE
                                        val dayNum = startDayOfSeries + cellIndex

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
                                                .border(0.5.dp, BorderGray.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (status == DayStatus.NONE) MutedGray else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Renk Kılavuzu (Legend)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LegendItem(SuccessGreen, stringResource(R.string.discipline_summary_success))
                                LegendItem(DangerRed, stringResource(R.string.discipline_summary_failure))
                                LegendItem(CopperAccent, stringResource(R.string.today_status_active))
                                LegendItem(WarmGray, stringResource(R.string.no_records))
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .border(0.5.dp, BorderGray.copy(alpha = 0.4f), CircleShape)
        )
        Text(
            text = text,
            color = MutedGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

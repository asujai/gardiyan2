package com.gardiyan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.gardiyan.app.R
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.gardiyan.app.data.local.entity.UserSessionEntity
import com.gardiyan.app.ui.theme.*
import com.gardiyan.app.viewmodel.GuardianViewModel
import java.util.Calendar

@Composable
fun ProfileScreen(
    viewModel: GuardianViewModel,
    isOverlayEnabled: Boolean,
    isUsageEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    isBatteryExempted: Boolean,
    isNotificationsEnabled: Boolean
) {
    val session by viewModel.userSession.collectAsState()
    val logs by viewModel.allLogs.collectAsState()
    val restrictedApps by viewModel.restrictedApps.collectAsState()

    var selectedFilter by remember { mutableStateOf(LogFilter.ALL) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var isTimelineVisible by remember { mutableStateOf(false) }

    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showDataUsageDialog by remember { mutableStateOf(false) }
    
    var isLanguageExpanded by remember { mutableStateOf(false) }
    var isAboutExpanded by remember { mutableStateOf(false) }

    // 1. Temel Hesaplamalar
    val totalSuccessDays = remember(logs) {
        logs.count { it.eventType == "DAILY_SUCCESS" || it.eventType == "SUCCESS" || it.eventType == "SUCCESS_DAY" }
    }

    val filteredLogs = remember(logs, selectedFilter) {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        logs.filter { log ->
            when (selectedFilter) {
                LogFilter.ALL -> true
                LogFilter.TODAY -> log.timestamp >= startOfToday
                LogFilter.SUCCESSES -> log.eventType == "DAILY_SUCCESS" || log.eventType == "SUCCESS" || log.eventType == "SUCCESS_DAY"
                LogFilter.VIOLATIONS -> log.eventType == "DAILY_FAILURE" || log.eventType == "FAILURE" || log.eventType == "VIOLATION"
                LogFilter.CHANGES -> log.eventType == "RESTRICTION_ADDED" || log.eventType == "RESTRICTION_REMOVED" || log.eventType == "RESTRICTION_DELETED" || log.eventType == "RESTRICTION_RESET" || log.eventType == "LIMIT_CHANGED" || log.eventType == "DAYS_CHANGED" || log.eventType == "ACTIVE_DAYS_CHANGED" || log.eventType == "PERMISSION_CHANGED"
                LogFilter.LOCKS -> log.eventType == "OVERLAY_TRIGGERED" || log.eventType == "OVERLAY_SHOWN" || log.eventType == "SESSION_STARTED" || log.eventType == "SESSION_CLOSED" || log.eventType == "SERVICE_STARTED" || log.eventType == "SERVICE_RESTARTED" || log.eventType == "SERVICE_STOPPED"
                LogFilter.CANCELS -> log.eventType == "RESET_HOLD_5S" || log.eventType == "CRITICAL_ACTION_STARTED" || log.eventType == "CRITICAL_ACTION_COMPLETED"
            }
        }.take(100)
    }

    val groupedLogs = remember(filteredLogs) {
        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val calYesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val yesterday = calYesterday.timeInMillis

        val calWeek = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val aWeekAgo = calWeek.timeInMillis

        filteredLogs.groupBy { log ->
            when {
                log.timestamp >= today -> "today"
                log.timestamp >= yesterday -> "yesterday"
                log.timestamp >= aWeekAgo -> "this_week"
                else -> "older"
            }
        }
    }

    val activeApps = remember(restrictedApps) { restrictedApps.filter { it.isActive } }
    val todayHasViolation = remember(restrictedApps) {
        restrictedApps.any { it.isActive && it.isFailed }
    }
    val todayStatusKey = when {
        activeApps.isEmpty() -> "no_restrictions"
        todayHasViolation -> "violated"
        else -> "active"
    }
    val todayStatusColor = when {
        activeApps.isEmpty() -> MutedGray
        todayHasViolation -> DangerRed
        else -> SuccessGreen
    }
    val todayStatusIcon = when {
        activeApps.isEmpty() -> "☕"
        todayHasViolation -> "⚠️"
        else -> "🛡️"
    }

    val lastSuccessLog = remember(logs) {
        logs.firstOrNull { it.eventType == "DAILY_SUCCESS" || it.eventType == "SUCCESS" || it.eventType == "SUCCESS_DAY" }
    }
    val lastViolationLog = remember(logs) {
        logs.firstOrNull { it.eventType == "DAILY_FAILURE" || it.eventType == "FAILURE" || it.eventType == "VIOLATION" || it.eventType == "RESET_HOLD_5S" || it.eventType == "CRITICAL_ACTION_COMPLETED" }
    }
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMMM yyyy HH:mm", java.util.Locale.getDefault()) }
    val noRecordsStr = stringResource(R.string.no_records)
    val lastSuccessDateStr = remember(lastSuccessLog, noRecordsStr) {
        lastSuccessLog?.let { dateFormat.format(java.util.Date(it.timestamp)) } ?: noRecordsStr
    }
    val lastViolationDateStr = remember(lastViolationLog, noRecordsStr) {
        lastViolationLog?.let { dateFormat.format(java.util.Date(it.timestamp)) } ?: noRecordsStr
    }

    val level = session?.level ?: 1
    val hasBadge = session?.hasRedBadge == true
    val motivationMessage = remember(level, hasBadge, todayHasViolation) {
        when {
            hasBadge -> "motivation_red_badge"
            todayHasViolation -> "motivation_violated"
            level == 3 -> "motivation_level_master"
            level == 2 -> "motivation_level_disciplined"
            else -> "motivation_level_rookie"
        }
    }
    val motivationText = when (motivationMessage) {
        "motivation_red_badge" -> stringResource(R.string.motivation_red_badge)
        "motivation_violated" -> stringResource(R.string.motivation_violated)
        "motivation_level_master" -> stringResource(R.string.motivation_level_master)
        "motivation_level_disciplined" -> stringResource(R.string.motivation_level_disciplined)
        else -> stringResource(R.string.motivation_level_rookie)
    }

    val activeRedemptionDaysLeft = session?.activeRedemptionDaysLeft ?: 0
    val redemptionStreakGoal = session?.redemptionStreakGoal?.coerceAtLeast(1) ?: 2
    val consecutiveSuccessDays = session?.consecutiveSuccessDays ?: 0

    val progress: Float
    val progressLabel: String
    val progressText: String
    val progressColor: Color

    if (hasBadge && activeRedemptionDaysLeft > 0) {
        val completedDays = (redemptionStreakGoal - activeRedemptionDaysLeft).coerceAtLeast(0)
        progress = completedDays.toFloat() / redemptionStreakGoal.toFloat()
        progressLabel = stringResource(R.string.redemption_progress_label)
        progressText = stringResource(R.string.redemption_progress_text, completedDays, redemptionStreakGoal)
        progressColor = DangerRed
    } else {
        progressLabel = stringResource(R.string.rank_progress_label)
        progressColor = SuccessGreen
        when (level) {
            1 -> {
                progress = (consecutiveSuccessDays.toFloat() / 3f).coerceAtMost(1.0f)
                progressText = stringResource(R.string.rank_progress_text_rookie, consecutiveSuccessDays)
            }
            2 -> {
                progress = (consecutiveSuccessDays.toFloat() / 7f).coerceAtMost(1.0f)
                progressText = stringResource(R.string.rank_progress_text_disciplined, consecutiveSuccessDays)
            }
            else -> {
                progress = 1.0f
                progressText = stringResource(R.string.rank_progress_max)
            }
        }
    }

    val isNoDataYet = logs.isEmpty()
    val context = LocalContext.current

    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.profile_clear_confirm_title),
                    fontWeight = FontWeight.Bold,
                    color = PureBlack,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.profile_clear_confirm_desc),
                    color = MutedGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDataDialog = false
                        viewModel.clearAllUserData(context)
                        android.widget.Toast.makeText(context, context.getString(R.string.profile_clear_success), android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = OnPureBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_clean), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDataDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MutedGray)
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showDataUsageDialog) {
        AlertDialog(
            onDismissRequest = { showDataUsageDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.profile_data_usage_privacy),
                    fontWeight = FontWeight.Bold,
                    color = PureBlack,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.profile_data_usage_desc1) +
                           stringResource(R.string.profile_data_usage_desc2) +
                           stringResource(R.string.profile_data_usage_desc3) +
                           stringResource(R.string.profile_data_usage_desc4),
                    color = MutedGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDataUsageDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_accept_disclosure), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isTimelineVisible -> stringResource(R.string.profile_timeline)
                        isSettingsVisible -> stringResource(R.string.profile_settings)
                        else -> stringResource(R.string.profile_title)
                    },
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    color = PureBlack,
                    letterSpacing = 0.5.sp
                )
                IconButton(
                    onClick = {
                        if (isTimelineVisible) {
                            isTimelineVisible = false
                        } else {
                            isSettingsVisible = !isSettingsVisible
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isTimelineVisible || isSettingsVisible) Icons.Default.Close else Icons.Default.Settings,
                        contentDescription = if (isTimelineVisible || isSettingsVisible) stringResource(R.string.btn_close) else stringResource(R.string.protected_apps_settings),
                        tint = PureBlack
                    )
                }
            }
        }

        if (isTimelineVisible) {
            // Zaman Tüneli Filtreleri ve Başlığı
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(LogFilter.entries.toTypedArray()) { filter ->
                            val isSelected = selectedFilter == filter
                            val chipBg = if (isSelected) PureBlack else DarkCharcoal
                            val chipText = if (isSelected) OnPureBlack else PureBlack
                            val chipBorder = if (isSelected) PureBlack else BorderGray

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(filter.titleRes),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = chipText
                                )
                            }
                        }
                    }
                }
            }

            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

            if (groupedLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = MatteSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = if (isNoDataYet) stringResource(R.string.profile_timeline_empty) else stringResource(R.string.profile_timeline_filter_empty),
                                fontSize = 12.sp,
                                color = MutedGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                groupedLogs.forEach { (groupName, groupLogs) ->
                    item {
                        val displayGroupName = when (groupName) {
                            "today" -> stringResource(R.string.timeline_today)
                            "yesterday" -> stringResource(R.string.timeline_yesterday)
                            "this_week" -> stringResource(R.string.timeline_this_week)
                            "older" -> stringResource(R.string.timeline_older)
                            else -> groupName
                        }
                        Text(
                            text = displayGroupName.uppercase(),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MutedGray,
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(groupLogs.size) { index ->
                        val log = groupLogs[index]
                        val timeStr = timeFormat.format(java.util.Date(log.timestamp))

                        val title = when (log.eventType) {
                            "RESTRICTION_ADDED" -> stringResource(R.string.log_type_restriction_added)
                            "RESTRICTION_REMOVED" -> stringResource(R.string.log_type_restriction_removed)
                            "RESTRICTION_DELETED" -> stringResource(R.string.log_type_restriction_deleted)
                            "QUICK_TEST_STARTED" -> stringResource(R.string.log_type_test_started)
                            "RESTRICTION_RESET" -> stringResource(R.string.log_type_counter_reset)
                            "RESET_HOLD_5S" -> stringResource(R.string.log_type_all_cleared)
                            "FAILURE", "DAILY_FAILURE", "VIOLATION" -> stringResource(R.string.log_type_violation)
                            "SUCCESS", "DAILY_SUCCESS", "SUCCESS_DAY" -> stringResource(R.string.log_type_day_success)
                            "LIMIT_CHANGED" -> stringResource(R.string.log_type_limit_changed)
                            "DAYS_CHANGED", "ACTIVE_DAYS_CHANGED" -> stringResource(R.string.log_type_days_changed)
                            "OVERLAY_SHOWN", "OVERLAY_TRIGGERED" -> stringResource(R.string.log_type_app_locked)
                            "SERVICE_STARTED" -> stringResource(R.string.log_type_engine_started)
                            "SERVICE_RESTARTED" -> stringResource(R.string.log_type_engine_restarted)
                            "SERVICE_STOPPED" -> stringResource(R.string.log_type_engine_stopped)
                            "SUSPICIOUS_STATE_DETECTED" -> stringResource(R.string.log_type_engine_fast)
                            "ENGINE_RESYNCED" -> stringResource(R.string.log_type_engine_sync)
                            "A11Y_EVENT_RECEIVED" -> stringResource(R.string.log_type_app_entry)
                            "USAGE_STATS_FALLBACK" -> stringResource(R.string.log_type_fallback_check)
                            "SESSION_STARTED" -> stringResource(R.string.log_type_session_started)
                            "SESSION_UPDATED" -> stringResource(R.string.log_type_session_updated)
                            "SESSION_CLOSED" -> stringResource(R.string.log_type_session_closed)
                            "STALE_SESSION_CLEANED" -> stringResource(R.string.log_type_stale_cleaned)
                            "USAGE_PROCESSED" -> stringResource(R.string.log_type_usage_processed)
                            "CRITICAL_ACTION_STARTED" -> stringResource(R.string.log_type_critical_start)
                            "CRITICAL_ACTION_COMPLETED" -> stringResource(R.string.log_type_critical_complete)
                            "PERMISSION_CHANGED" -> stringResource(R.string.log_type_system_permission)
                            else -> log.eventType
                        }

                        val friendlyDetails = when (log.eventType) {
                            "RESTRICTION_ADDED" -> stringResource(R.string.log_desc_restriction_added, log.appName)
                            "RESTRICTION_REMOVED" -> stringResource(R.string.log_desc_restriction_removed, log.appName)
                            "RESTRICTION_DELETED" -> stringResource(R.string.log_desc_restriction_deleted, log.appName)
                            "QUICK_TEST_STARTED" -> stringResource(R.string.log_desc_test_started, log.appName)
                            "RESTRICTION_RESET" -> stringResource(R.string.log_desc_counter_reset, log.appName)
                            "RESET_HOLD_5S" -> stringResource(R.string.log_desc_all_cleared)
                            "FAILURE", "DAILY_FAILURE", "VIOLATION" -> stringResource(R.string.log_desc_violation, log.appName)
                            "SUCCESS", "DAILY_SUCCESS", "SUCCESS_DAY" -> stringResource(R.string.log_desc_day_success)
                            "LIMIT_CHANGED" -> stringResource(R.string.log_desc_limit_changed, log.appName)
                            "DAYS_CHANGED", "ACTIVE_DAYS_CHANGED" -> stringResource(R.string.log_desc_days_changed, log.appName)
                            "OVERLAY_SHOWN", "OVERLAY_TRIGGERED" -> stringResource(R.string.log_desc_app_locked, log.appName)
                            "SERVICE_STARTED" -> stringResource(R.string.log_desc_engine_started)
                            "SERVICE_RESTARTED" -> stringResource(R.string.log_desc_engine_restarted)
                            "SERVICE_STOPPED" -> stringResource(R.string.log_desc_engine_stopped)
                            "SUSPICIOUS_STATE_DETECTED" -> stringResource(R.string.log_desc_engine_fast)
                            "ENGINE_RESYNCED" -> stringResource(R.string.log_desc_engine_sync)
                            "A11Y_EVENT_RECEIVED" -> stringResource(R.string.log_desc_app_entry, log.appName)
                            "USAGE_STATS_FALLBACK" -> stringResource(R.string.log_desc_fallback_check, log.appName)
                            "SESSION_STARTED" -> stringResource(R.string.log_desc_session_started, log.appName)
                            "SESSION_UPDATED" -> stringResource(R.string.log_desc_session_updated, log.appName)
                            "SESSION_CLOSED" -> stringResource(R.string.log_desc_session_closed, log.appName)
                            "STALE_SESSION_CLEANED" -> stringResource(R.string.log_desc_stale_cleaned, log.appName)
                            "USAGE_PROCESSED" -> stringResource(R.string.log_desc_usage_processed, log.appName)
                            "CRITICAL_ACTION_STARTED" -> stringResource(R.string.log_desc_critical_start, log.appName)
                            "CRITICAL_ACTION_COMPLETED" -> stringResource(R.string.log_desc_critical_complete, log.appName)
                            "PERMISSION_CHANGED" -> stringResource(R.string.log_desc_system_permission)
                            else -> log.details
                        }

                        val statusIcon = when (log.eventType) {
                            "FAILURE", "RESET_HOLD_5S", "DAILY_FAILURE", "CRITICAL_ACTION_STARTED", "VIOLATION" -> "⚠️"
                            "CRITICAL_ACTION_COMPLETED", "RESTRICTION_DELETED" -> "🗑️"
                            "SUCCESS", "DAILY_SUCCESS", "SUCCESS_DAY" -> "🏆"
                            "RESTRICTION_ADDED", "QUICK_TEST_STARTED" -> "➕"
                            "SERVICE_STARTED", "SERVICE_RESTARTED", "SESSION_STARTED" -> "🚀"
                            "SERVICE_STOPPED", "SESSION_CLOSED" -> "🛑"
                            "SUSPICIOUS_STATE_DETECTED", "USAGE_STATS_FALLBACK", "SESSION_UPDATED" -> "🔍"
                            "ENGINE_RESYNCED" -> "🔄"
                            "OVERLAY_SHOWN", "OVERLAY_TRIGGERED" -> "🔒"
                            "STALE_SESSION_CLEANED" -> "🧹"
                            "USAGE_PROCESSED" -> "⏳"
                            "PERMISSION_CHANGED" -> "🔑"
                            else -> "ℹ️"
                        }

                        val statusColor = when (log.eventType) {
                            "FAILURE", "RESET_HOLD_5S", "DAILY_FAILURE", "CRITICAL_ACTION_STARTED", "VIOLATION" -> DangerRed
                            "SUCCESS", "DAILY_SUCCESS", "SUCCESS_DAY", "SERVICE_STARTED", "SERVICE_RESTARTED", "ENGINE_RESYNCED", "SESSION_STARTED" -> SuccessGreen
                            "CRITICAL_ACTION_COMPLETED", "RESTRICTION_DELETED", "SESSION_CLOSED", "STALE_SESSION_CLEANED", "PERMISSION_CHANGED" -> DangerRed.copy(alpha = 0.8f)
                            else -> MutedGray
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(statusColor.copy(alpha = 0.08f))
                                ) {
                                    Text(text = statusIcon, fontSize = 16.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = timeStr,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MutedGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = friendlyDetails,
                                        fontSize = 10.sp,
                                        color = MutedGray,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (isSettingsVisible) {
            item {
                val context = LocalContext.current
                val allOk = isOverlayEnabled && isUsageEnabled && isAccessibilityEnabled && isBatteryExempted && isNotificationsEnabled
                var isHealthExpanded by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (allOk) SuccessGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isHealthExpanded = !isHealthExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.perm_status),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureBlack
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (allOk) SuccessGreen else DangerRed)
                                )
                            }
                            Text(
                                text = if (isHealthExpanded) "▲" else "▼",
                                fontSize = 10.sp,
                                color = MutedGray
                            )
                        }
                        
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isHealthExpanded
                        ) {
                            Column {
                                              HealthRow(
                                    name = stringResource(R.string.perm_usage_access_title),
                                    description = stringResource(R.string.perm_usage_access_desc),
                                    isOk = isUsageEnabled,
                                    onClick = { viewModel.openUsageStatsSettings(context) }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                HealthRow(
                                    name = stringResource(R.string.perm_accessibility_title),
                                    description = stringResource(R.string.perm_accessibility_desc),
                                    isOk = isAccessibilityEnabled,
                                    onClick = { viewModel.openAccessibilitySettings(context) }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                HealthRow(
                                    name = stringResource(R.string.perm_overlay_title),
                                    description = stringResource(R.string.perm_overlay_desc),
                                    isOk = isOverlayEnabled,
                                    onClick = { viewModel.openOverlaySettings(context) }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                HealthRow(
                                    name = stringResource(R.string.perm_battery_title),
                                    description = stringResource(R.string.perm_battery_desc),
                                    isOk = isBatteryExempted,
                                    onClick = { viewModel.requestBatteryOptimizationIgnore(context) }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                HealthRow(
                                    name = stringResource(R.string.perm_notification_title),
                                    description = stringResource(R.string.perm_notification_desc),
                                    isOk = isNotificationsEnabled,
                                    onClick = { viewModel.openNotificationSettings(context) }
                                )
                            }
                        }
                    }
                }
            }

            // Tema Ayarları
            item {
                val context = LocalContext.current
                var isThemeExpanded by remember { mutableStateOf(false) }
                val currentMode = currentThemeMode.value
                val currentPalette = currentThemePalette.value
                val currentModeLabel = when (currentMode) {
                    AppThemeMode.SYSTEM -> stringResource(R.string.profile_theme_system_default)
                    AppThemeMode.LIGHT -> stringResource(R.string.profile_theme_light)
                    AppThemeMode.DARK -> stringResource(R.string.profile_theme_dark)
                }
                val currentPaletteLabel = when (currentPalette) {
                    AppThemePalette.BLUE -> stringResource(R.string.profile_theme_blue)
                    AppThemePalette.MONOCHROME -> stringResource(R.string.profile_theme_bw)
                    AppThemePalette.RED -> stringResource(R.string.profile_theme_red)
                    AppThemePalette.PREMIUM_DARK -> stringResource(R.string.profile_theme_premium)
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
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isThemeExpanded = !isThemeExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.profile_theme_settings),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$currentModeLabel - $currentPaletteLabel",
                                    fontSize = 11.sp,
                                    color = MutedGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isThemeExpanded) "▲" else "▼",
                                    fontSize = 10.sp,
                                    color = MutedGray
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isThemeExpanded
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = stringResource(R.string.profile_theme_view_mode),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ThemeOptionRow(
                                    label = stringResource(R.string.profile_theme_system_default),
                                    isSelected = currentMode == AppThemeMode.SYSTEM,
                                    onClick = {
                                        updateThemeMode(context, AppThemeMode.SYSTEM)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ThemeOptionRow(
                                    label = stringResource(R.string.profile_theme_light),
                                    isSelected = currentMode == AppThemeMode.LIGHT,
                                    onClick = {
                                        updateThemeMode(context, AppThemeMode.LIGHT)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ThemeOptionRow(
                                    label = stringResource(R.string.profile_theme_dark),
                                    isSelected = currentMode == AppThemeMode.DARK,
                                    onClick = {
                                        updateThemeMode(context, AppThemeMode.DARK)
                                    }
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = BorderGray, thickness = 0.8.dp)
                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = stringResource(R.string.profile_theme_color),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ThemeOptionRow(
                                    label = stringResource(R.string.profile_theme_blue),
                                    isSelected = currentPalette == AppThemePalette.BLUE,
                                    onClick = {
                                        updateThemePalette(context, AppThemePalette.BLUE)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ThemeOptionRow(
                                    label = stringResource(R.string.profile_theme_bw),
                                    isSelected = currentPalette == AppThemePalette.MONOCHROME,
                                    onClick = {
                                        updateThemePalette(context, AppThemePalette.MONOCHROME)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ThemeOptionRow(
                                    label = stringResource(R.string.profile_theme_red),
                                    isSelected = currentPalette == AppThemePalette.RED,
                                    onClick = {
                                        updateThemePalette(context, AppThemePalette.RED)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ThemeOptionRow(
                                    label = stringResource(R.string.profile_theme_premium),
                                    isSelected = currentPalette == AppThemePalette.PREMIUM_DARK,
                                    onClick = {
                                        updateThemePalette(context, AppThemePalette.PREMIUM_DARK)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Dil Ayarları
            item {
                val context = LocalContext.current
                val currentLocales = AppCompatDelegate.getApplicationLocales()
                val currentLang = if (!currentLocales.isEmpty()) currentLocales.get(0)?.language ?: "en" else java.util.Locale.getDefault().language

                val languages = listOf(
                    "en" to "English",
                    "tr" to "Türkçe",
                    "es" to "Español",
                    "fr" to "Français",
                    "de" to "Deutsch",
                    "pt" to "Português",
                    "ar" to "العربية",
                    "hi" to "हिन्दी",
                    "id" to "Bahasa Indonesia",
                    "ru" to "Русский"
                )

                val currentLangLabel = languages.firstOrNull { it.first == currentLang }?.second ?: "English"

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
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isLanguageExpanded = !isLanguageExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.profile_language_settings),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentLangLabel,
                                    fontSize = 11.sp,
                                    color = MutedGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isLanguageExpanded) "▲" else "▼",
                                    fontSize = 10.sp,
                                    color = MutedGray
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isLanguageExpanded
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(14.dp))
                                languages.forEach { (langCode, langName) ->
                                    ThemeOptionRow(
                                        label = langName,
                                        isSelected = currentLang == langCode,
                                        onClick = {
                                            val localeList = LocaleListCompat.forLanguageTags(langCode)
                                            AppCompatDelegate.setApplicationLocales(localeList)
                                            (context as? android.app.Activity)?.recreate()
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Destek ve Geri Bildirim
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(20.dp),
                    onClick = { launchEmailIntent(context) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PureBlack.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = PureBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_support),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.profile_support_desc),
                                fontSize = 10.sp,
                                color = MutedGray,
                                lineHeight = 14.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MutedGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Gizlilik Politikası
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://doc-hosting.flycricket.io/limitra-privacy-policy/1dd2dedf-ea24-4a49-91b5-fa79b0ba9337/privacy"))
                        runCatching {
                            context.startActivity(intent)
                        }.onFailure {
                            android.widget.Toast.makeText(context, context.getString(R.string.profile_browser_error), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PureBlack.copy(alpha = 0.08f))
                        ) {
                            Text(text = "📄", fontSize = 16.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_privacy),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.profile_privacy_desc),
                                fontSize = 10.sp,
                                color = MutedGray,
                                lineHeight = 14.sp
                            )
                        }

                        Text(
                            text = stringResource(R.string.profile_details),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            modifier = Modifier
                                .clickable { showDataUsageDialog = true }
                                .border(0.8.dp, SuccessGreen, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Kullanım Şartları
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://doc-hosting.flycricket.io/limitra-terms-of-use/8221a2c7-16d3-454e-9a0d-a61495fab4e6/terms"))
                        runCatching {
                            context.startActivity(intent)
                        }.onFailure {
                            android.widget.Toast.makeText(context, context.getString(R.string.profile_browser_error), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PureBlack.copy(alpha = 0.08f))
                        ) {
                            Text(text = "📜", fontSize = 16.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_terms),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.profile_terms_desc),
                                fontSize = 10.sp,
                                color = MutedGray,
                                lineHeight = 14.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MutedGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Verilerimi Temizle
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DangerRed.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(20.dp),
                    onClick = { showDeleteDataDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DangerRed.copy(alpha = 0.08f))
                        ) {
                            Text(text = "🗑️", fontSize = 16.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_clear_data),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.profile_clear_data_desc),
                                fontSize = 10.sp,
                                color = MutedGray,
                                lineHeight = 14.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MutedGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Hakkında & Sürüm Bilgisi
            item {
                val appVersion = runCatching {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName ?: "1.0.0"
                }.getOrDefault("1.0.0")

                val appVersionCode = runCatching {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        pInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        pInfo.versionCode.toString()
                    }
                }.getOrDefault("1")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(20.dp),
                    onClick = { isAboutExpanded = !isAboutExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.profile_about_title).uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack
                            )
                            Text(
                                text = if (isAboutExpanded) "▲" else "▼",
                                fontSize = 10.sp,
                                color = MutedGray
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isAboutExpanded
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = stringResource(R.string.profile_about_desc),
                                    fontSize = 12.sp,
                                    color = PureBlack,
                                    lineHeight = 18.sp
                                )

                                HorizontalDivider(color = BorderGray, thickness = 0.8.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = stringResource(R.string.profile_app_name), fontSize = 11.sp, color = MutedGray)
                                    Text(text = stringResource(R.string.app_name), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = stringResource(R.string.profile_version), fontSize = 11.sp, color = MutedGray)
                                    Text(text = appVersion, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = stringResource(R.string.profile_version_code), fontSize = 11.sp, color = MutedGray)
                                    Text(text = appVersionCode, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = stringResource(R.string.profile_android_version), fontSize = 11.sp, color = MutedGray)
                                    Text(text = android.os.Build.VERSION.RELEASE, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = stringResource(R.string.profile_developer_contact), fontSize = 11.sp, color = MutedGray)
                                    Text(text = "lumoriapdf@gmail.com", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                                }
                            }
                        }
                    }
                }
            }

            // Zaman Tüneli Butonu
            item {
                Button(
                    onClick = { isTimelineVisible = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureBlack,
                        contentColor = OnPureBlack
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_timeline),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        } else {
            // 1. Profil Hub (Görsel Avatar & XP Bar)
            item {
                val levelName = when (level) {
                    1 -> stringResource(R.string.level_rookie)
                    2 -> stringResource(R.string.level_disciplined)
                    3 -> stringResource(R.string.level_master)
                    else -> stringResource(R.string.level_rookie)
                }
                val shieldEmoji = when (level) {
                    1 -> "🛡️"
                    2 -> "🥈"
                    3 -> "👑"
                    else -> "🛡️"
                }

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
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar Dairesi
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(PureWhite.copy(alpha = 0.05f))
                                .border(1.5.dp, PureWhite.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Text(text = shieldEmoji, fontSize = 32.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.profile_level_label, level),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = MutedGray,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = levelName.uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = PureBlack,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // XP / Telafi Barı (Gerçek verilere bağlı)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = progressLabel, fontSize = 10.sp, color = MutedGray)
                                Text(
                                    text = progressText,
                                    fontSize = 10.sp,
                                    color = MutedGray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = progressColor,
                                trackColor = MatteSurface
                            )
                        }
                    }
                }
            }



            // 3. Asimetrik Bento Kutuları (Bugünün Durumu & Başarı İstatistikleri)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bugünün Durumu Bento Kartı
                        val todayStatusLabel = when (todayStatusKey) {
                            "no_restrictions" -> stringResource(R.string.today_status_no_restrictions)
                            "violated" -> stringResource(R.string.today_status_violated)
                            else -> stringResource(R.string.today_status_active)
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.profile_today_status),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedGray
                                    )
                                    Text(text = todayStatusIcon, fontSize = 14.sp)
                                }
                                Text(
                                    text = todayStatusLabel,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = todayStatusColor
                                )
                            }
                        }

                        // Ardışık Seri Bento Kartı
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.profile_streak_label),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedGray
                                    )
                                    Text(text = "🔥", fontSize = 14.sp)
                                }
                                Text(
                                    text = stringResource(R.string.profile_streak_days, consecutiveSuccessDays),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PureBlack
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Toplam Başarı Bento Kartı
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.profile_total_success),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedGray
                                    )
                                    Text(text = "🏆", fontSize = 14.sp)
                                }
                                Text(
                                    text = stringResource(R.string.profile_total_success_days, totalSuccessDays),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PureBlack
                                )
                            }
                        }

                        // Korumadaki Uygulamalar Bento Kartı
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.profile_protected_target),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedGray
                                    )
                                    Text(text = "🔒", fontSize = 14.sp)
                                }
                                Text(
                                    text = stringResource(R.string.profile_protected_apps, activeApps.size),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PureBlack
                                )
                            }
                        }
                    }
                }
            }

            // 4. Tarih Bilgileri ve Motivasyon Kartı
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.profile_stat_details),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedGray,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.profile_last_success), fontSize = 12.sp, color = MutedGray)
                            Text(text = lastSuccessDateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.profile_last_violation), fontSize = 12.sp, color = MutedGray)
                            Text(text = lastViolationDateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = BorderGray, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = motivationText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PureBlack,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = PureBlack
        )
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = PureBlack,
                unselectedColor = MutedGray
            )
        )
    }
}

private fun launchEmailIntent(context: android.content.Context) {
    val unknownStr = context.getString(R.string.no_records)
    val appVersion = runCatching {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: unknownStr
    }.getOrDefault(unknownStr)

    val androidVersion = android.os.Build.VERSION.RELEASE
    val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

    val emailBody = context.getString(R.string.profile_mail_body, appVersion, androidVersion, deviceModel)

    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
        data = android.net.Uri.parse("mailto:")
        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("lumoriapdf@gmail.com"))
        putExtra(android.content.Intent.EXTRA_SUBJECT, context.getString(R.string.profile_mail_subject))
        putExtra(android.content.Intent.EXTRA_TEXT, emailBody)
    }

    runCatching {
        context.startActivity(intent)
    }.onFailure {
        android.widget.Toast.makeText(context, context.getString(R.string.profile_mail_error), android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun HealthRow(
    name: String,
    description: String,
    isOk: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOk) { onClick() }
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = if (isOk) "🟢" else "🔴", fontSize = 10.sp)
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
            }
            if (!isOk) {
                Text(
                    text = stringResource(R.string.perm_btn_grant),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed,
                    modifier = Modifier
                        .border(0.8.dp, DangerRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.perm_state_active),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            fontSize = 10.sp,
            color = MutedGray,
            modifier = Modifier.padding(start = 18.dp),
            lineHeight = 14.sp
        )
    }
}

@Composable
fun HeaderSection(session: UserSessionEntity?) {
    // DashboardHeader artık ana header. Bu fonksiyon geriye uyumluluk için korunuyor.
}

enum class LogFilter(val titleRes: Int) {
    TODAY(R.string.timeline_today),
    SUCCESSES(R.string.profile_total_success),
    VIOLATIONS(R.string.log_type_violation),
    CHANGES(R.string.log_filter_restrictions),
    LOCKS(R.string.log_filter_locks),
    CANCELS(R.string.log_filter_bypasses),
    ALL(R.string.log_filter_all)
}

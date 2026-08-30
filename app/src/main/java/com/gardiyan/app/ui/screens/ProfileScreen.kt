package com.gardiyan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.gardiyan.app.R
import com.gardiyan.app.BuildConfig
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.gardiyan.app.data.local.entity.UserSessionEntity
import com.gardiyan.app.data.timeline.parseRestrictionLogDetails
import com.gardiyan.app.ui.theme.*
import com.gardiyan.app.viewmodel.GuardianViewModel
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.gardiyan.app.data.model.UsagePeriod
import com.gardiyan.app.ui.components.formatUsageDuration
import com.gardiyan.app.ui.components.localizedMinutes
import androidx.compose.runtime.produceState

private enum class ProfileSection {
    SUMMARY,
    TIMELINE,
    SETTINGS
}

@Composable
fun ProfileScreen(
    viewModel: GuardianViewModel,
    isOverlayEnabled: Boolean,
    isUsageEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    isBatteryExempted: Boolean,
    isNotificationsEnabled: Boolean,
    onNavigateToSavedQuotes: () -> Unit = {}
) {
    val session by viewModel.userSession.collectAsState()
    val logs by viewModel.allLogs.collectAsState()
    val restrictedApps by viewModel.restrictedApps.collectAsState()

    var filterDate by remember { mutableStateOf(TimelineDateFilter.ALL_TIME) }
    var filterType by remember { mutableStateOf(TimelineTypeFilter.ALL) }
    var selectedSection by remember { mutableStateOf(ProfileSection.SUMMARY) }

    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showDataUsageDialog by remember { mutableStateOf(false) }
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSupportInfoDialog by remember { mutableStateOf(false) }
    var showPermissionInfoDialog by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showQuoteSettingsDialog by remember { mutableStateOf(false) }

    var showLevelDetailDialog by remember { mutableStateOf(false) }
    var showStreakDetailDialog by remember { mutableStateOf(false) }
    var showSavedTimeDetailDialog by remember { mutableStateOf(false) }
    var showActiveAppsDetailDialog by remember { mutableStateOf(false) }
    var showWeeklySummaryDetailDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val allOk = isOverlayEnabled && isUsageEnabled && isAccessibilityEnabled && isBatteryExempted && isNotificationsEnabled
    var isHealthExpanded by remember { mutableStateOf(false) }
    var isClearingData by remember { mutableStateOf(false) }

    // 1. Temel Hesaplamalar
    val totalSuccessDays = remember(logs) {
        logs.count { it.eventType == "DAILY_SUCCESS" || it.eventType == "SUCCESS" || it.eventType == "SUCCESS_DAY" }
    }

    val filteredLogs = remember(logs, filterDate, filterType) {
        val now = System.currentTimeMillis()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val limitDate = when (filterDate) {
            TimelineDateFilter.TODAY -> startOfToday
            TimelineDateFilter.LAST_7_DAYS -> now - 7 * 24 * 60 * 60 * 1000L
            TimelineDateFilter.LAST_30_DAYS -> now - 30 * 24 * 60 * 60 * 1000L
            TimelineDateFilter.ALL_TIME -> 0L
        }

        val step1 = logs.filter { log ->
            // Exclude technical logs explicitly
            val isMeaningful = log.eventType in setOf(
                "RESTRICTION_ADDED", "RESTRICTION_REMOVED", "RESTRICTION_DELETED", "RESTRICTIONS_CLEARED",
                "LIMIT_CHANGED", "DAYS_CHANGED", "ACTIVE_DAYS_CHANGED", "RESTRICTION_RESET", "QUICK_TEST_STARTED",
                "FAILURE", "DAILY_FAILURE", "VIOLATION", "SUCCESS", "DAILY_SUCCESS", "SUCCESS_DAY",
                "PERMISSION_CHANGED", "RESET_HOLD_5S", "DATA_CLEARED",
                "SERVICE_STARTED", "SERVICE_STOPPED", "OVERLAY_SHOWN", "OVERLAY_TRIGGERED"
            )
            if (!isMeaningful) return@filter false

            // Date filter
            if (log.timestamp < limitDate) return@filter false

            // Type filter
            when (filterType) {
                TimelineTypeFilter.ALL -> true
                TimelineTypeFilter.RESTRICTIONS -> log.eventType in setOf(
                    "RESTRICTION_ADDED",
                    "RESTRICTION_REMOVED",
                    "RESTRICTION_DELETED",
                    "LIMIT_CHANGED",
                    "DAYS_CHANGED",
                    "ACTIVE_DAYS_CHANGED",
                    "RESTRICTION_RESET",
                    "QUICK_TEST_STARTED"
                )
                TimelineTypeFilter.LIMITS -> log.eventType in setOf(
                    "FAILURE",
                    "DAILY_FAILURE",
                    "VIOLATION",
                    "SUCCESS",
                    "DAILY_SUCCESS",
                    "SUCCESS_DAY",
                    "OVERLAY_SHOWN",
                    "OVERLAY_TRIGGERED"
                )
                TimelineTypeFilter.PERMISSIONS -> log.eventType == "PERMISSION_CHANGED"
                TimelineTypeFilter.DATA_ACTIONS -> log.eventType in setOf(
                    "RESET_HOLD_5S",
                    "DATA_CLEARED",
                    "RESTRICTIONS_CLEARED"
                )
            }
        }

        deduplicateTimelineLogs(step1)
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

    val levelNameRes = when (level) {
        1 -> R.string.level_name_1
        2 -> R.string.level_name_2
        3 -> R.string.level_name_3
        4 -> R.string.level_name_4
        5 -> R.string.level_name_5
        6 -> R.string.level_name_6
        else -> R.string.level_name_1
    }
    val levelName = stringResource(levelNameRes)

    val nextLevelNoteRes = when (level) {
        1 -> R.string.level_note_1
        2 -> R.string.level_note_2
        3 -> R.string.level_note_3
        4 -> R.string.level_note_4
        5 -> R.string.level_note_5
        else -> R.string.level_note_max
    }
    val nextLevelNote = stringResource(nextLevelNoteRes)

    val profileProgress = calculateProfileProgress(
        level = level,
        consecutiveSuccessDays = consecutiveSuccessDays,
        hasRedBadge = hasBadge,
        activeRedemptionDaysLeft = activeRedemptionDaysLeft,
        redemptionStreakGoal = redemptionStreakGoal
    )

    val progress: Float
    val progressLabel: String
    val progressText: String
    val progressColor: Color

    if (profileProgress.mode == ProfileProgressMode.REDEMPTION) {
        progress = profileProgress.progress
        progressLabel = stringResource(R.string.profile_redemption_progress)
        progressText = stringResource(R.string.profile_redemption_progress_format, profileProgress.completed, profileProgress.goal)
        progressColor = DangerRed
    } else {
        progressLabel = stringResource(R.string.profile_next_level_progress)
        progressColor = SuccessGreen
        progress = profileProgress.progress
        progressText = "${(profileProgress.progress * 100).toInt()}%"
    }

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

    val weeklySuccessDays = remember(logs) {
        val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        logs.filter { it.timestamp >= oneWeekAgo && (it.eventType == "DAILY_SUCCESS" || it.eventType == "SUCCESS" || it.eventType == "SUCCESS_DAY") }
            .map { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.get(Calendar.DAY_OF_YEAR)
            }.toSet().size.coerceAtMost(7)
    }

    val isNoDataYet = logs.isEmpty()

    if (showLevelDetailDialog) {
        AlertDialog(
            onDismissRequest = { showLevelDetailDialog = false },
            title = { Text(stringResource(R.string.profile_level_detail_title), fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Text(
                    stringResource(R.string.profile_level_detail_desc, levelName, level),
                    color = MutedGray, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLevelDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showStreakDetailDialog) {
        AlertDialog(
            onDismissRequest = { showStreakDetailDialog = false },
            title = { Text(stringResource(R.string.profile_streak_dialog_title), fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Text(
                    stringResource(R.string.profile_streak_dialog_desc, consecutiveSuccessDays),
                    color = MutedGray, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showStreakDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showSavedTimeDetailDialog) {
        AlertDialog(
            onDismissRequest = { showSavedTimeDetailDialog = false },
            title = { Text(stringResource(R.string.profile_saved_time_dialog_title), fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                val timeStr = if (totalSavedMillis > 0) formatUsageDuration(totalSavedMillis) else stringResource(R.string.profile_saved_time_no_data)
                Text(
                    stringResource(R.string.profile_saved_time_dialog_desc, timeStr),
                    color = MutedGray, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSavedTimeDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showActiveAppsDetailDialog) {
        AlertDialog(
            onDismissRequest = { showActiveAppsDetailDialog = false },
            title = { Text(stringResource(R.string.profile_active_apps_dialog_title), fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                val appListStr = if (activeApps.isNotEmpty()) {
                    activeApps.joinToString("\n") {
                        "• ${it.appName} (${context.localizedMinutes(it.dailyLimitMinutes)})"
                    }
                } else {
                    stringResource(R.string.profile_active_apps_dialog_empty)
                }
                Text(
                    stringResource(R.string.profile_active_apps_dialog_desc, appListStr),
                    color = MutedGray, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showActiveAppsDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showWeeklySummaryDetailDialog) {
        AlertDialog(
            onDismissRequest = { showWeeklySummaryDetailDialog = false },
            title = { Text(stringResource(R.string.profile_weekly_summary_dialog_title), fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Text(
                    stringResource(R.string.profile_weekly_summary_dialog_desc, weeklySuccessDays),
                    color = MutedGray, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showWeeklySummaryDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { if (!isClearingData) showDeleteDataDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.profile_clear_confirm_title),
                    fontWeight = FontWeight.Bold,
                    color = PureBlack,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.profile_clear_confirm_desc),
                        color = MutedGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    if (isClearingData) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PureBlack)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isClearingData = true
                        viewModel.clearAllUserData(
                            context = context,
                            onSuccess = {
                                isClearingData = false
                                showDeleteDataDialog = false
                                android.widget.Toast.makeText(context, context.getString(R.string.profile_clear_success), android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                isClearingData = false
                                showDeleteDataDialog = false
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.profile_clear_error,
                                        error.localizedMessage ?: context.getString(R.string.profile_unknown_error)
                                    ),
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    },
                    enabled = !isClearingData,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = OnPureBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_clean), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDataDialog = false },
                    enabled = !isClearingData,
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

    if (showThemeDialog) {
        val currentMode = currentThemeMode.value
        val currentPalette = currentThemePalette.value
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.profile_theme_dialog_title), fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(stringResource(R.string.profile_theme_dialog_mode_section), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedGray)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AppThemeMode.SYSTEM to stringResource(R.string.profile_theme_system_default),
                            AppThemeMode.LIGHT to stringResource(R.string.profile_theme_light),
                            AppThemeMode.DARK to stringResource(R.string.profile_theme_dark)
                        ).forEach { (mode, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { updateThemeMode(context, mode) }
                                    .heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentMode == mode,
                                    onClick = { updateThemeMode(context, mode) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PureBlack)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = PureBlack, fontSize = 13.sp)
                            }
                        }
                    }
                    
                    HorizontalDivider(color = BorderGray, thickness = 0.8.dp)
                    
                    Text(stringResource(R.string.profile_theme_dialog_palette_section), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedGray)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AppThemePalette.BLUE to stringResource(R.string.profile_theme_blue),
                            AppThemePalette.MONOCHROME to stringResource(R.string.profile_theme_bw),
                            AppThemePalette.RED to stringResource(R.string.profile_theme_red),
                            AppThemePalette.PREMIUM_DARK to stringResource(R.string.profile_theme_premium)
                        ).forEach { (palette, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { updateThemePalette(context, palette) }
                                    .heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentPalette == palette,
                                    onClick = { updateThemePalette(context, palette) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PureBlack)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = PureBlack, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showThemeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showLanguageDialog) {
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
            "ru" to "Русский",
            "th" to "Thai / ไทย"
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.profile_language_dialog_title), fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(languages) { (langCode, langName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val localeList = LocaleListCompat.forLanguageTags(langCode)
                                    AppCompatDelegate.setApplicationLocales(localeList)
                                    (context as? android.app.Activity)?.recreate()
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(langName, color = PureBlack, fontSize = 14.sp, fontWeight = if (currentLang == langCode) FontWeight.Bold else FontWeight.Normal)
                            RadioButton(
                                selected = currentLang == langCode,
                                onClick = {
                                    val localeList = LocaleListCompat.forLanguageTags(langCode)
                                    AppCompatDelegate.setApplicationLocales(localeList)
                                    (context as? android.app.Activity)?.recreate()
                                    showLanguageDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PureBlack)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLanguageDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MutedGray)
                ) {
                    Text(stringResource(R.string.btn_close))
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showQuoteSettingsDialog) {
        val prefs = remember { context.getSharedPreferences("gardiyan_settings", android.content.Context.MODE_PRIVATE) }
        var customQuotesList by remember { mutableStateOf(loadCustomQuotes(prefs)) }
        var showOnlyMyQuotes by remember { mutableStateOf(prefs.getBoolean("show_only_my_quotes", false)) }
        
        var tempQuoteText by remember { mutableStateOf("") }
        var tempQuoteAuthor by remember { mutableStateOf("") }
        var editingQuoteId by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showQuoteSettingsDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_quote_dialog_title),
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_quote_dialog_desc),
                        fontSize = 12.sp,
                        color = MutedGray,
                        lineHeight = 16.sp
                    )

                    // Ekleme / Duzenleme Formu
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MatteSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = tempQuoteText,
                                onValueChange = { tempQuoteText = it },
                                label = { Text(stringResource(R.string.settings_quote_text_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PureBlack,
                                    unfocusedBorderColor = BorderGray,
                                    focusedLabelColor = PureBlack
                                ),
                                singleLine = false,
                                maxLines = 3
                            )

                            OutlinedTextField(
                                value = tempQuoteAuthor,
                                onValueChange = { tempQuoteAuthor = it },
                                label = { Text(stringResource(R.string.settings_quote_author_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PureBlack,
                                    unfocusedBorderColor = BorderGray,
                                    focusedLabelColor = PureBlack
                                ),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    if (tempQuoteText.trim().isNotEmpty()) {
                                        val authorText = tempQuoteAuthor.trim().ifEmpty { context.getString(R.string.quote_author_anonymous) }
                                        if (editingQuoteId != null) {
                                            customQuotesList = customQuotesList.map {
                                                if (it.id == editingQuoteId) {
                                                    it.copy(text = tempQuoteText.trim(), author = authorText)
                                                } else it
                                            }
                                            editingQuoteId = null
                                        } else {
                                            val newItem = CustomQuoteItem(
                                                id = System.currentTimeMillis().toString(),
                                                text = tempQuoteText.trim(),
                                                author = authorText,
                                                isSelected = true
                                            )
                                            customQuotesList = customQuotesList + newItem
                                        }
                                        saveCustomQuotes(prefs, customQuotesList)
                                        tempQuoteText = ""
                                        tempQuoteAuthor = ""
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.settings_quote_empty_error),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PureBlack,
                                    contentColor = OnPureBlack
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (editingQuoteId != null) stringResource(R.string.saved_quotes_btn_update) else stringResource(R.string.saved_quotes_btn_add),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BorderGray, thickness = 0.5.dp)

                    // Kaydedilen Sozler Ozet Satiri
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showQuoteSettingsDialog = false
                                onNavigateToSavedQuotes()
                            }
                            .border(1.dp, BorderGray.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MatteSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PureBlack,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.saved_quotes_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(BorderGray.copy(alpha = 0.5f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = customQuotesList.size.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureBlack
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MutedGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderGray, thickness = 0.5.dp)

                    // Mod Secimi: "Sadece benim sözlerimi göster"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showOnlyMyQuotes = !showOnlyMyQuotes
                                prefs.edit().putBoolean("show_only_my_quotes", showOnlyMyQuotes).apply()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.profile_only_my_quotes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureBlack
                        )
                        Checkbox(
                            checked = showOnlyMyQuotes,
                            onCheckedChange = { isChecked ->
                                showOnlyMyQuotes = isChecked
                                prefs.edit().putBoolean("show_only_my_quotes", isChecked).apply()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = PureBlack)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQuoteSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showAboutDialog) {
        val appVersion = runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.1"
        }.getOrDefault("1.1")

        val appVersionCode = runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toString()
            }
        }.getOrDefault("1")

        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.profile_about_title), fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_about_desc),
                        fontSize = 13.sp,
                        color = PureBlack,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = BorderGray, thickness = 0.8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.profile_app_name), fontSize = 12.sp, color = MutedGray)
                        Text(text = stringResource(R.string.app_name), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.profile_version), fontSize = 12.sp, color = MutedGray)
                        Text(text = appVersion, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.profile_version_code), fontSize = 12.sp, color = MutedGray)
                        Text(text = appVersionCode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.profile_android_version), fontSize = 12.sp, color = MutedGray)
                        Text(text = android.os.Build.VERSION.RELEASE, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.profile_developer_contact), fontSize = 12.sp, color = MutedGray)
                        Text(text = "destek@limitra.online", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showSupportInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSupportInfoDialog = false },
            title = { Text(stringResource(R.string.profile_support_dialog_title), fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Text(
                    text = stringResource(R.string.profile_support_dialog_desc),
                    color = MutedGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSupportInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showPermissionInfoDialog != null) {
        val (title, desc) = when (showPermissionInfoDialog) {
            "usage" -> stringResource(R.string.perm_usage_access_title) to stringResource(R.string.perm_usage_access_desc)
            "accessibility" -> stringResource(R.string.perm_accessibility_title) to stringResource(R.string.perm_accessibility_desc)
            "overlay" -> stringResource(R.string.perm_overlay_title) to stringResource(R.string.perm_overlay_desc)
            "battery" -> stringResource(R.string.perm_battery_title) to stringResource(R.string.perm_battery_desc)
            "notification" -> stringResource(R.string.perm_notification_title) to stringResource(R.string.perm_notification_desc)
            else -> "" to ""
        }
        AlertDialog(
            onDismissRequest = { showPermissionInfoDialog = null },
            title = { Text(title, fontWeight = FontWeight.Bold, color = PureBlack) },
            text = { Text(desc, color = MutedGray, fontSize = 13.sp, lineHeight = 18.sp) },
            confirmButton = {
                Button(
                    onClick = { showPermissionInfoDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text(stringResource(R.string.profile_filter_dialog_title), fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.profile_filter_dialog_date_section), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedGray)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TimelineDateFilter.entries.forEach { dateOpt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { filterDate = dateOpt }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = filterDate == dateOpt,
                                    onClick = { filterDate = dateOpt },
                                    colors = RadioButtonDefaults.colors(selectedColor = PureBlack)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(dateOpt.labelResId), color = PureBlack, fontSize = 13.sp)
                            }
                        }
                    }
                    
                    HorizontalDivider(color = BorderGray, thickness = 0.8.dp)
                    
                    Text(stringResource(R.string.profile_filter_dialog_type_section), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedGray)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TimelineTypeFilter.entries.forEach { typeOpt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { filterType = typeOpt }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = filterType == typeOpt,
                                    onClick = { filterType = typeOpt },
                                    colors = RadioButtonDefaults.colors(selectedColor = PureBlack)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(typeOpt.labelResId), color = PureBlack, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFilterDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text(stringResource(R.string.profile_filter_btn_apply), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
    ) {
        ProfileSectionHeader(
            selectedSection = selectedSection,
            onSectionSelected = { selectedSection = it },
            onFilterClick = { showFilterDialog = true }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        if (selectedSection == ProfileSection.TIMELINE) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "${stringResource(filterDate.labelResId)} · ${stringResource(filterType.labelResId)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MutedGray
                    )
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
                            fontSize = 12.sp,
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
                            "RESTRICTION_ADDED" -> buildString {
                                append(stringResource(R.string.log_desc_restriction_added, log.appName))
                                val restrictionDetails = parseRestrictionLogDetails(log.details)
                                if (restrictionDetails != null) {
                                    append(" · ")
                                    append(
                                        stringResource(
                                            R.string.profile_restriction_log_limit,
                                            restrictionDetails.dailyLimitMinutes
                                        )
                                    )
                                    if (
                                        restrictionDetails.restrictionName.isNotBlank() &&
                                        restrictionDetails.restrictionName != log.appName
                                    ) {
                                        append(" · ")
                                        append(
                                            stringResource(
                                                R.string.profile_restriction_log_group,
                                                restrictionDetails.restrictionName
                                            )
                                        )
                                    }
                                } else if (log.details.isNotBlank()) {
                                    append(" · ")
                                    append(log.details)
                                }
                            }
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

                        val statusColor = when (log.eventType) {
                            "FAILURE", "RESET_HOLD_5S", "DAILY_FAILURE", "CRITICAL_ACTION_STARTED", "VIOLATION" -> DangerRed
                            "SUCCESS", "DAILY_SUCCESS", "SUCCESS_DAY", "SERVICE_STARTED", "SERVICE_RESTARTED", "ENGINE_RESYNCED", "SESSION_STARTED" -> SuccessGreen
                            "CRITICAL_ACTION_COMPLETED", "RESTRICTION_DELETED", "SESSION_CLOSED", "STALE_SESSION_CLEANED", "PERMISSION_CHANGED" -> DangerRed.copy(alpha = 0.8f)
                            else -> MutedGray
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(56.dp)
                                        .background(statusColor)
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        @Suppress("DEPRECATION")
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = timeStr,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MutedGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = friendlyDetails,
                                        fontSize = 12.sp,
                                        color = MutedGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedSection == ProfileSection.SETTINGS) {
            // 1. İzin Durumu
            item {
                SettingsGroupCard(
                    title = stringResource(R.string.settings_group_device_permissions),
                    borderColor = if (allOk) SuccessGreen.copy(alpha = 0.25f) else DangerRed.copy(alpha = 0.25f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isHealthExpanded = !isHealthExpanded }
                                .heightIn(min = 56.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.perm_status),
                                    fontSize = 14.sp,
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (allOk) stringResource(R.string.profile_permissions_all_active) else stringResource(R.string.profile_permissions_missing),
                                    fontSize = 12.sp,
                                    color = MutedGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = if (isHealthExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MutedGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isHealthExpanded
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                HealthRow(
                                    name = stringResource(R.string.perm_usage_access_title),
                                    isOk = isUsageEnabled,
                                    onInfoClick = { showPermissionInfoDialog = "usage" },
                                    onClick = { viewModel.openUsageStatsSettings(context) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                HealthRow(
                                    name = stringResource(R.string.perm_accessibility_title),
                                    isOk = isAccessibilityEnabled,
                                    onInfoClick = { showPermissionInfoDialog = "accessibility" },
                                    onClick = { viewModel.openAccessibilitySettings(context) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                HealthRow(
                                    name = stringResource(R.string.perm_overlay_title),
                                    isOk = isOverlayEnabled,
                                    onInfoClick = { showPermissionInfoDialog = "overlay" },
                                    onClick = { viewModel.openOverlaySettings(context) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                HealthRow(
                                    name = stringResource(R.string.perm_battery_title),
                                    isOk = isBatteryExempted,
                                    onInfoClick = { showPermissionInfoDialog = "battery" },
                                    onClick = { viewModel.requestBatteryOptimizationIgnore(context) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                HealthRow(
                                    name = stringResource(R.string.perm_notification_title),
                                    isOk = isNotificationsEnabled,
                                    onInfoClick = { showPermissionInfoDialog = "notification" },
                                    onClick = { viewModel.openNotificationSettings(context) }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Görünüm Ayarları
            item {
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
                    "ru" to "Русский",
                    "th" to "Thai / ไทย"
                )
                val currentLangLabel = languages.firstOrNull { it.first == currentLang }?.second ?: "English"

                SettingsGroupCard(title = stringResource(R.string.settings_group_appearance_language)) {
                    SettingsRow(
                        title = stringResource(R.string.profile_appearance),
                        value = "$currentModeLabel · $currentPaletteLabel",
                        icon = Icons.Default.Settings,
                        onClick = { showThemeDialog = true }
                    )
                    HorizontalDivider(color = BorderGray.copy(alpha = 0.7f))
                    SettingsRow(
                        title = stringResource(R.string.profile_language_settings),
                        value = currentLangLabel,
                        icon = Icons.Default.Info,
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            // 3ab. Bildirimler
            item {
                val prefs = remember { context.getSharedPreferences("gardiyan_settings", android.content.Context.MODE_PRIVATE) }
                var notificationsEnabledState by remember {
                    mutableStateOf(prefs.getBoolean("notifications_enabled", true))
                }

                val isAndroid13OrAbove = android.os.Build.VERSION.SDK_INT >= 33
                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        prefs.edit().putBoolean("notifications_enabled", true).apply()
                        notificationsEnabledState = true
                    } else {
                        prefs.edit().putBoolean("notifications_enabled", false).apply()
                        notificationsEnabledState = false
                    }
                }

                LaunchedEffect(isNotificationsEnabled) {
                    if (isAndroid13OrAbove && !isNotificationsEnabled) {
                        notificationsEnabledState = false
                        prefs.edit().putBoolean("notifications_enabled", false).apply()
                    }
                }

                val quoteSummary = deriveQuoteSummaryMode(
                    customQuotesJson = prefs.getString("custom_quotes_json", "[]") ?: "[]",
                    showOnlyMyQuotes = prefs.getBoolean("show_only_my_quotes", false),
                    legacyHasCustomQuote = prefs.getBoolean("has_custom_quote", false),
                    legacyPreference = prefs.getString("custom_quote_preference", "mix") ?: "mix"
                )
                val valueText = when (quoteSummary) {
                    QuoteSummaryMode.ONLY_MY_QUOTES -> stringResource(R.string.profile_custom_quote_always)
                    QuoteSummaryMode.MIXED -> stringResource(R.string.profile_custom_quote_mix)
                    QuoteSummaryMode.DEFAULT -> stringResource(R.string.profile_default_quotes)
                }

                SettingsGroupCard(title = stringResource(R.string.settings_group_notifications_content)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.profile_notifications_title),
                        description = stringResource(R.string.profile_notifications_desc),
                        checked = notificationsEnabledState,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (isAndroid13OrAbove && !viewModel.areNotificationsEnabled(context)) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    prefs.edit().putBoolean("notifications_enabled", true).apply()
                                    notificationsEnabledState = true
                                }
                            } else {
                                prefs.edit().putBoolean("notifications_enabled", false).apply()
                                notificationsEnabledState = false
                            }
                        }
                    )
                    HorizontalDivider(color = BorderGray.copy(alpha = 0.7f))
                    SettingsRow(
                        title = stringResource(R.string.settings_quote_title),
                        value = valueText,
                        icon = Icons.Default.Info,
                        onClick = { showQuoteSettingsDialog = true }
                    )
                }
            }

            // 4. Destek ve Geri Bildirim
            item {
                SettingsGroupCard(title = stringResource(R.string.settings_group_support_legal)) {
                    SettingsRow(
                        title = stringResource(R.string.profile_support),
                        icon = Icons.Default.Email,
                        onClick = { launchEmailIntent(context) },
                        infoIconClick = { showSupportInfoDialog = true }
                    )
                    HorizontalDivider(color = BorderGray.copy(alpha = 0.7f))
                    SettingsRow(
                        title = stringResource(R.string.profile_privacy),
                        icon = Icons.Default.Lock,
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://limitra.online/gizlilik-politikasi/"))
                            runCatching {
                                context.startActivity(intent)
                            }.onFailure {
                                android.widget.Toast.makeText(context, context.getString(R.string.profile_browser_error), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    HorizontalDivider(color = BorderGray.copy(alpha = 0.7f))
                    SettingsRow(
                        title = stringResource(R.string.profile_terms),
                        icon = Icons.Default.Info,
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://limitra.online/kullanim-sartlari/"))
                            runCatching {
                                context.startActivity(intent)
                            }.onFailure {
                                android.widget.Toast.makeText(context, context.getString(R.string.profile_browser_error), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    HorizontalDivider(color = BorderGray.copy(alpha = 0.7f))
                    SettingsRow(
                        title = stringResource(R.string.profile_about_title),
                        value = stringResource(R.string.profile_version_format, BuildConfig.VERSION_NAME),
                        icon = Icons.Default.Info,
                        onClick = { showAboutDialog = true }
                    )
                }
            }

            // 7. Verilerimi Temizle
            item {
                SettingsGroupCard(
                    title = stringResource(R.string.settings_group_data_management),
                    borderColor = DangerRed.copy(alpha = 0.5f)
                ) {
                    SettingsRow(
                        title = stringResource(R.string.profile_clear_data),
                        value = stringResource(R.string.profile_clear_data_desc),
                        icon = Icons.Default.Warning,
                        onClick = { showDeleteDataDialog = true },
                        textColor = DangerRed,
                        showArrow = false
                    )
                }
            }

        } else {
            // 1. Profil Hub (Yeni Temiz Seviye Kartı - Tıklanabilir)
            item {
                Card(
                    onClick = { showLevelDetailDialog = true },
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
                        Text(
                            text = stringResource(R.string.profile_level_format, level),
                            fontSize = 12.sp,
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

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = progressLabel, fontSize = 12.sp, color = MutedGray)
                                Text(
                                    text = progressText,
                                    fontSize = 12.sp,
                                    color = progressColor,
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (profileProgress.mode == ProfileProgressMode.REDEMPTION) {
                                    stringResource(R.string.profile_redemption_note)
                                } else {
                                    nextLevelNote
                                },
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MutedGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 2. Bento Kartları (Özet Bilgiler)
            item {
                val timeDisplayStr = if (totalSavedMillis > 0) {
                    formatUsageDuration(totalSavedMillis)
                } else {
                    stringResource(R.string.profile_saved_time_no_data)
                }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val fontScale = LocalDensity.current.fontScale
                    val stackCards = shouldStackProfileSummaryCards(maxWidth.value, fontScale)
                    val cards: @Composable (Modifier) -> Unit = { cardModifier ->
                        ProfileSummaryCard(
                            title = stringResource(R.string.profile_streak_title),
                            value = stringResource(R.string.profile_streak_days, consecutiveSuccessDays),
                            icon = Icons.Default.Check,
                            onClick = { showStreakDetailDialog = true },
                            modifier = cardModifier
                        )
                        ProfileSummaryCard(
                            title = stringResource(R.string.profile_saved_time_title),
                            value = timeDisplayStr,
                            icon = Icons.Default.Info,
                            onClick = { showSavedTimeDetailDialog = true },
                            modifier = cardModifier
                        )
                        ProfileSummaryCard(
                            title = stringResource(R.string.profile_protected_apps_title),
                            value = stringResource(R.string.profile_protected_apps, activeApps.size),
                            icon = Icons.Default.Lock,
                            onClick = { showActiveAppsDetailDialog = true },
                            modifier = cardModifier
                        )
                        ProfileSummaryCard(
                            title = stringResource(R.string.profile_weekly_summary_title),
                            value = stringResource(R.string.profile_weekly_success_days_format, weeklySuccessDays),
                            icon = Icons.Default.Settings,
                            onClick = { showWeeklySummaryDetailDialog = true },
                            modifier = cardModifier
                        )
                    }

                    if (stackCards) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            cards(Modifier.fillMaxWidth())
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ProfileSummaryCard(
                                    title = stringResource(R.string.profile_streak_title),
                                    value = stringResource(R.string.profile_streak_days, consecutiveSuccessDays),
                                    icon = Icons.Default.Check,
                                    onClick = { showStreakDetailDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                ProfileSummaryCard(
                                    title = stringResource(R.string.profile_saved_time_title),
                                    value = timeDisplayStr,
                                    icon = Icons.Default.Info,
                                    onClick = { showSavedTimeDetailDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ProfileSummaryCard(
                                    title = stringResource(R.string.profile_protected_apps_title),
                                    value = stringResource(R.string.profile_protected_apps, activeApps.size),
                                    icon = Icons.Default.Lock,
                                    onClick = { showActiveAppsDetailDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                ProfileSummaryCard(
                                    title = stringResource(R.string.profile_weekly_summary_title),
                                    value = stringResource(R.string.profile_weekly_success_days_format, weeklySuccessDays),
                                    icon = Icons.Default.Settings,
                                    onClick = { showWeeklySummaryDetailDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun ProfileSectionHeader(
    selectedSection: ProfileSection,
    onSectionSelected: (ProfileSection) -> Unit,
    onFilterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                fontSize = 20.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                color = PureBlack
            )
            if (selectedSection == ProfileSection.TIMELINE) {
                TextButton(
                    onClick = onFilterClick,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_filter_btn_text),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkCharcoal)
                .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sections = listOf(
                ProfileSection.SUMMARY to R.string.profile_section_summary,
                ProfileSection.TIMELINE to R.string.profile_section_timeline,
                ProfileSection.SETTINGS to R.string.profile_section_settings
            )
            sections.forEach { (section, labelRes) ->
                val isSelected = selectedSection == section
                Surface(
                    onClick = { onSectionSelected(section) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics { selected = isSelected },
                    color = if (isSelected) PureBlack else Color.Transparent,
                    contentColor = if (isSelected) OnPureBlack else MutedGray,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(labelRes),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
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
    val recipient = "destek@limitra.online"
    val subject = context.getString(R.string.profile_mail_subject)
    val emailBody = context.getString(R.string.profile_mail_body)
    val mailUri = android.net.Uri.parse(
        "mailto:${android.net.Uri.encode(recipient)}" +
            "?subject=${android.net.Uri.encode(subject)}" +
            "&body=${android.net.Uri.encode(emailBody)}"
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
        data = mailUri
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
    isOk: Boolean,
    onInfoClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOk) { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isOk) SuccessGreen else DangerRed)
            )
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PureBlack
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isOk) stringResource(R.string.profile_permission_state_ok) else stringResource(R.string.profile_permission_state_missing),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOk) SuccessGreen else DangerRed
            )
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.btn_info_desc),
                    tint = MutedGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 104.dp)
            .border(1.dp, BorderGray, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedGray
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PureBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Black,
                color = PureBlack
            )
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    borderColor: Color = BorderGray,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 4.dp),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MutedGray
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String? = null,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    showArrow: Boolean = true,
    infoIconClick: (() -> Unit)? = null,
    textColor: Color = PureBlack
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MutedGray,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        if (infoIconClick != null) {
            IconButton(
                onClick = infoIconClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.btn_info_desc),
                    tint = MutedGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showArrow) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MutedGray,
                        modifier = Modifier.size(20.dp)
                    )
        }
    }
}

@Composable
fun HeaderSection(session: UserSessionEntity?) {
    // DashboardHeader artık ana header. Bu fonksiyon geriye uyumluluk için korunuyor.
}

enum class TimelineDateFilter(val labelResId: Int) {
    TODAY(R.string.filter_date_today),
    LAST_7_DAYS(R.string.filter_date_7_days),
    LAST_30_DAYS(R.string.filter_date_30_days),
    ALL_TIME(R.string.filter_date_all)
}

enum class TimelineTypeFilter(val labelResId: Int) {
    ALL(R.string.filter_type_all),
    RESTRICTIONS(R.string.filter_type_restrictions),
    LIMITS(R.string.filter_type_limits),
    PERMISSIONS(R.string.filter_type_permissions),
    DATA_ACTIONS(R.string.filter_type_data_actions)
}

fun loadCustomQuotes(prefs: android.content.SharedPreferences): List<CustomQuoteItem> {
    val json = prefs.getString("custom_quotes_json", "[]") ?: "[]"
    val list = parseCustomQuotes(json).toMutableList()
    
    val oldText = prefs.getString("custom_quote_text", "") ?: ""
    val oldAuthor = prefs.getString("custom_quote_author", "") ?: ""
    val hasCustom = prefs.getBoolean("has_custom_quote", false)
    if (hasCustom && oldText.isNotEmpty() && list.isEmpty()) {
        val oldItem = CustomQuoteItem(
            id = System.currentTimeMillis().toString(),
            text = oldText,
            author = oldAuthor,
            isSelected = true
        )
        list.add(oldItem)
        try {
            val array = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("id", oldItem.id)
                    put("text", oldItem.text)
                    put("author", oldItem.author)
                    put("isSelected", oldItem.isSelected)
                })
            }
            prefs.edit().putString("custom_quotes_json", array.toString()).apply()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
    return list
}

fun saveCustomQuotes(prefs: android.content.SharedPreferences, list: List<CustomQuoteItem>) {
    try {
        val array = org.json.JSONArray()
        for (item in list) {
            val obj = org.json.JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("author", item.author)
                put("isSelected", item.isSelected)
            }
            array.put(obj)
        }
        prefs.edit().putString("custom_quotes_json", array.toString()).apply()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .heightIn(min = 64.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = PureBlack,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
                if (description != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MutedGray,
                        lineHeight = 16.sp
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureWhite,
                    checkedTrackColor = SuccessGreen,
                    uncheckedThumbColor = MutedGray,
                    uncheckedTrackColor = BorderGray
                )
            )
    }
}

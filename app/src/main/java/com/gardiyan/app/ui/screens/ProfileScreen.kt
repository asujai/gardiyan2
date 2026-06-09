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
import androidx.compose.material.icons.filled.Info
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.gardiyan.app.data.model.UsagePeriod
import com.gardiyan.app.ui.components.formatUsageDuration
import androidx.compose.runtime.produceState

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

    var filterDate by remember { mutableStateOf(TimelineDateFilter.ALL_TIME) }
    var filterType by remember { mutableStateOf(TimelineTypeFilter.ALL) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var isTimelineVisible by remember { mutableStateOf(false) }

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

        val resultList = mutableListOf<com.gardiyan.app.data.local.entity.StatusLogEntity>()
        var lastLog: com.gardiyan.app.data.local.entity.StatusLogEntity? = null
        for (log in step1) {
            if (lastLog != null && lastLog.eventType == log.eventType && lastLog.appName == log.appName) {
                continue
            }
            resultList.add(log)
            lastLog = log
        }
        resultList.take(100)
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

    val levelName = when (level) {
        1 -> "Başlangıç"
        2 -> "Kontrol Kazanıyor"
        3 -> "Odaklı"
        4 -> "Dengeli Kullanıcı"
        5 -> "Dijital Disiplin"
        6 -> "Tam Kontrol"
        else -> "Başlangıç"
    }

    val nextLevelNote = when (level) {
        1 -> "Seviye 2 (Kontrol Kazanıyor) olmak için 3 gün kesintisiz başarı gerekir."
        2 -> "Seviye 3 (Odaklı) olmak için 7 gün kesintisiz başarı gerekir."
        3 -> "Seviye 4 (Dengeli Kullanıcı) olmak için 15 gün kesintisiz başarı gerekir."
        4 -> "Seviye 5 (Dijital Disiplin) olmak için 30 gün kesintisiz başarı gerekir."
        5 -> "Seviye 6 (Tam Kontrol) olmak için 60 gün kesintisiz başarı gerekir."
        else -> "Tebrikler! En yüksek kontrol seviyesindesiniz."
    }

    val levelProgress = when (level) {
        1 -> (consecutiveSuccessDays.toFloat() / 3f).coerceIn(0f, 1f)
        2 -> (consecutiveSuccessDays.toFloat() / 7f).coerceIn(0f, 1f)
        3 -> (consecutiveSuccessDays.toFloat() / 15f).coerceIn(0f, 1f)
        4 -> (consecutiveSuccessDays.toFloat() / 30f).coerceIn(0f, 1f)
        5 -> (consecutiveSuccessDays.toFloat() / 60f).coerceIn(0f, 1f)
        else -> 1f
    }

    val progress: Float
    val progressLabel: String
    val progressText: String
    val progressColor: Color

    if (hasBadge && activeRedemptionDaysLeft > 0) {
        val completedDays = (redemptionStreakGoal - activeRedemptionDaysLeft).coerceAtLeast(0)
        progress = completedDays.toFloat() / redemptionStreakGoal.toFloat()
        progressLabel = "Telafi İlerlemesi"
        progressText = "$completedDays / $redemptionStreakGoal Gün"
        progressColor = DangerRed
    } else {
        progressLabel = "Sonraki Seviyeye İlerleme"
        progressColor = SuccessGreen
        progress = levelProgress
        progressText = "${(levelProgress * 100).toInt()}%"
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
            title = { Text("Dijital Seviye Detayı", fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Text(
                    "Mevcut Seviyeniz: $levelName (Seviye $level)\n\n" +
                    "Seviyeler ve Gereksinimler:\n" +
                    "• Seviye 1: Başlangıç\n" +
                    "• Seviye 2: Kontrol Kazanıyor (3 gün kesintisiz başarı)\n" +
                    "• Seviye 3: Odaklı (7 gün kesintisiz başarı)\n" +
                    "• Seviye 4: Dengeli Kullanıcı (15 gün kesintisiz başarı)\n" +
                    "• Seviye 5: Dijital Disiplin (30 gün kesintisiz başarı)\n" +
                    "• Seviye 6: Tam Kontrol (60 gün kesintisiz başarı)\n\n" +
                    "Kısıtlamaları ihlal etmeden her başarılı gün serinizi artırır ve sonraki seviyeye geçmenizi sağlar.",
                    color = MutedGray, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLevelDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text("Kapat", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showStreakDetailDialog) {
        AlertDialog(
            onDismissRequest = { showStreakDetailDialog = false },
            title = { Text("Başarı Serisi", fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Text(
                    "Kısıtlama kurallarını bozmadan geçirdiğiniz ardışık gün sayısı: $consecutiveSuccessDays Gün.\n\n" +
                    "Dijital dengenizi bozmadığınız sürece bu seri her gün artar. Kurallar ihlal edildiğinde veya kısıtlamalar bypass edildiğinde sıfırlanır.",
                    color = MutedGray, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showStreakDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text("Kapat", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showSavedTimeDetailDialog) {
        AlertDialog(
            onDismissRequest = { showSavedTimeDetailDialog = false },
            title = { Text("Kazanılan / Korunan Süre", fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                val timeStr = if (totalSavedMillis > 0) formatUsageDuration(totalSavedMillis) else "Henüz veri yok"
                Text(
                    "Kısıtlamalar sayesinde bugüne kadar koruduğunuz tahmini süre: $timeStr.\n\n" +
                    "Bu değer, kısıtlanan uygulamalardaki geçmiş ortalama kullanım süreniz ile günlük limitiniz arasındaki fark temel alınarak hesaplanır.",
                    color = MutedGray, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSavedTimeDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text("Kapat", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showActiveAppsDetailDialog) {
        AlertDialog(
            onDismissRequest = { showActiveAppsDetailDialog = false },
            title = { Text("Aktif Kısıtlamalar", fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                val appListStr = if (activeApps.isNotEmpty()) {
                    activeApps.joinToString("\n") { "• ${it.appName} (${it.dailyLimitMinutes} dk)" }
                } else {
                    "Aktif kısıtlanmış uygulama bulunmuyor."
                }
                Text(
                    "Şu anda aktif koruma altındaki uygulamalar:\n\n$appListStr",
                    color = MutedGray, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showActiveAppsDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text("Kapat", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showWeeklySummaryDetailDialog) {
        AlertDialog(
            onDismissRequest = { showWeeklySummaryDetailDialog = false },
            title = { Text("Haftalık Özet Detayı", fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Text(
                    "Bu hafta toplam $weeklySuccessDays gün boyunca kısıtlama kurallarına tam uyum sağladınız.\n\n" +
                    "Haftalık hedeflerinizi tamamlamak dijital disiplininizi pekiştirir ve odaklanma sürenizi artırır.",
                    color = MutedGray, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showWeeklySummaryDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text("Kapat", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

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

    if (showThemeDialog) {
        val currentMode = currentThemeMode.value
        val currentPalette = currentThemePalette.value
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Görünüm Ayarları", fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Görünüm Modu", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedGray)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AppThemeMode.SYSTEM to "Sistem Varsayılanı",
                            AppThemeMode.LIGHT to "Açık Tema",
                            AppThemeMode.DARK to "Koyu Tema"
                        ).forEach { (mode, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { updateThemeMode(context, mode) }
                                    .padding(vertical = 4.dp),
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
                    
                    Text("Renk Teması", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedGray)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AppThemePalette.BLUE to "Mavi Tema",
                            AppThemePalette.MONOCHROME to "Siyah Beyaz",
                            AppThemePalette.RED to "Kırmızı Tema",
                            AppThemePalette.PREMIUM_DARK to "Premium Koyu"
                        ).forEach { (palette, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { updateThemePalette(context, palette) }
                                    .padding(vertical = 4.dp),
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
                    Text("Tamam", fontWeight = FontWeight.Bold)
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
            "ru" to "Русский"
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Dil Seçimi", fontWeight = FontWeight.Bold, color = PureBlack) },
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
                    Text("Kapat")
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showQuoteSettingsDialog) {
        val prefs = remember { context.getSharedPreferences("gardiyan_settings", android.content.Context.MODE_PRIVATE) }
        var tempQuoteText by remember { mutableStateOf(prefs.getString("custom_quote_text", "") ?: "") }
        var tempQuoteAuthor by remember { mutableStateOf(prefs.getString("custom_quote_author", "") ?: "") }
        var tempPreference by remember { mutableStateOf(prefs.getString("custom_quote_preference", "mix") ?: "mix") }
        val hasCustom = prefs.getBoolean("has_custom_quote", false)

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
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_quote_dialog_desc),
                        fontSize = 11.sp,
                        color = MutedGray,
                        lineHeight = 16.sp
                    )

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
                        maxLines = 4
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

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_quote_pref_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { tempPreference = "always" }
                        ) {
                            RadioButton(
                                selected = tempPreference == "always",
                                onClick = { tempPreference = "always" },
                                colors = RadioButtonDefaults.colors(selectedColor = PureBlack)
                            )
                            Text(
                                text = stringResource(R.string.settings_quote_pref_always),
                                fontSize = 11.sp,
                                color = PureBlack
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { tempPreference = "mix" }
                        ) {
                            RadioButton(
                                selected = tempPreference == "mix",
                                onClick = { tempPreference = "mix" },
                                colors = RadioButtonDefaults.colors(selectedColor = PureBlack)
                            )
                            Text(
                                text = stringResource(R.string.settings_quote_pref_mix),
                                fontSize = 11.sp,
                                color = PureBlack
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasCustom) {
                        Button(
                            onClick = {
                                prefs.edit().apply {
                                    remove("custom_quote_text")
                                    remove("custom_quote_author")
                                    remove("custom_quote_preference")
                                    putBoolean("has_custom_quote", false)
                                    apply()
                                }
                                showQuoteSettingsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftDangerRed,
                                contentColor = DangerRed
                            )
                        ) {
                            Text(stringResource(R.string.btn_delete), fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (tempQuoteText.trim().isNotEmpty()) {
                                prefs.edit().apply {
                                    putString("custom_quote_text", tempQuoteText.trim())
                                    putString("custom_quote_author", tempQuoteAuthor.trim().ifEmpty { "Anonim" })
                                    putString("custom_quote_preference", tempPreference)
                                    putBoolean("has_custom_quote", true)
                                    apply()
                                }
                                showQuoteSettingsDialog = false
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
                        )
                    ) {
                        Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQuoteSettingsDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MutedGray)
                ) {
                    Text(stringResource(R.string.btn_cancel))
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
            title = { Text("Hakkında", fontWeight = FontWeight.Bold, color = PureBlack) },
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
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack)
                ) {
                    Text("Kapat", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showSupportInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSupportInfoDialog = false },
            title = { Text("Destek ve Geri Bildirim", fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Text(
                    text = "Sorun bildirmek veya yeni bir özellik önermek için bizimle iletişime geçebilirsiniz.",
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
                    Text("Kapat", fontWeight = FontWeight.Bold)
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
                    Text("Kapat", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Zaman Tünelini Filtrele", fontWeight = FontWeight.Bold, color = PureBlack) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Tarih", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedGray)
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
                                Text(dateOpt.label, color = PureBlack, fontSize = 13.sp)
                            }
                        }
                    }
                    
                    HorizontalDivider(color = BorderGray, thickness = 0.8.dp)
                    
                    Text("Olay Türü", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedGray)
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
                                Text(typeOpt.label, color = PureBlack, fontSize = 13.sp)
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
                    Text("Uygula", fontWeight = FontWeight.Bold)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTimelineVisible) {
                        TextButton(onClick = { showFilterDialog = true }) {
                            Text(text = "Filtrele", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                    }
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
        }

        if (isTimelineVisible) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "${filterDate.label} · ${filterType.label}",
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
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MutedGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = friendlyDetails,
                                        fontSize = 10.sp,
                                        color = MutedGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (isSettingsVisible) {
            // 1. İzin Durumu
            item {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (allOk) "Tüm izinler aktif" else "Eksik izin var",
                                    fontSize = 11.sp,
                                    color = MutedGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isHealthExpanded) "▲" else "▼",
                                    fontSize = 10.sp,
                                    color = MutedGray
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
                SettingsRow(
                    title = stringResource(R.string.profile_appearance),
                    value = "$currentModeLabel · $currentPaletteLabel",
                    onClick = { showThemeDialog = true }
                )
            }

            // 3. Dil Ayarları
            item {
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
                SettingsRow(
                    title = stringResource(R.string.profile_language_settings),
                    value = currentLangLabel,
                    onClick = { showLanguageDialog = true }
                )
            }

            // 3b. Koruma Ekranı Sözü
            item {
                val prefs = remember { context.getSharedPreferences("gardiyan_settings", android.content.Context.MODE_PRIVATE) }
                val hasCustom = prefs.getBoolean("has_custom_quote", false)
                val customPref = prefs.getString("custom_quote_preference", "mix") ?: "mix"
                val valueText = if (hasCustom) {
                    if (customPref == "always") "Özel Söz (Her Zaman)" else "Özel Söz (Karışık)"
                } else {
                    "Varsayılan Sözler"
                }

                SettingsRow(
                    title = stringResource(R.string.settings_quote_title),
                    value = valueText,
                    onClick = { showQuoteSettingsDialog = true }
                )
            }

            // 4. Destek ve Geri Bildirim
            item {
                SettingsRow(
                    title = stringResource(R.string.profile_support),
                    onClick = { launchEmailIntent(context) },
                    infoIconClick = { showSupportInfoDialog = true }
                )
            }

            // 5. Gizlilik Politikası
            item {
                SettingsRow(
                    title = stringResource(R.string.profile_privacy),
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://doc-hosting.flycricket.io/limitra-privacy-policy/1dd2dedf-ea24-4a49-91b5-fa79b0ba9337/privacy"))
                        runCatching {
                            context.startActivity(intent)
                        }.onFailure {
                            android.widget.Toast.makeText(context, context.getString(R.string.profile_browser_error), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // 5b. Kullanım Şartları
            item {
                SettingsRow(
                    title = stringResource(R.string.profile_terms),
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://doc-hosting.flycricket.io/limitra-terms-of-use/8221a2c7-16d3-454e-9a0d-a61495fab4e6/terms"))
                        runCatching {
                            context.startActivity(intent)
                        }.onFailure {
                            android.widget.Toast.makeText(context, context.getString(R.string.profile_browser_error), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // 6. Hakkında
            item {
                SettingsRow(
                    title = stringResource(R.string.profile_about_title),
                    value = "Limitra v1.1",
                    onClick = { showAboutDialog = true }
                )
            }

            // 7. Verilerimi Temizle
            item {
                SettingsRow(
                    title = stringResource(R.string.profile_clear_data),
                    onClick = { showDeleteDataDialog = true },
                    textColor = DangerRed,
                    showArrow = false
                )
            }

            // 8. Zaman Tüneli Butonu
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
                            text = "SEVİYE $level",
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

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Dijital Kontrol İlerlemesi", fontSize = 10.sp, color = MutedGray)
                                Text(
                                    text = "${(levelProgress * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    color = MutedGray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { levelProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SuccessGreen,
                                trackColor = MatteSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = nextLevelNote,
                                fontSize = 10.sp,
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Protection Streak Card
                        Card(
                            onClick = { showStreakDetailDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
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
                                        text = "BAŞARI SERİSİ",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedGray
                                    )
                                    Text(text = "🔥", fontSize = 14.sp)
                                }
                                Text(
                                    text = "$consecutiveSuccessDays Gün",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PureBlack
                                )
                            }
                        }

                        // Saved Time Card
                        val timeDisplayStr = if (totalSavedMillis > 0) formatUsageDuration(totalSavedMillis) else "Henüz veri yok"
                        Card(
                            onClick = { showSavedTimeDetailDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
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
                                        text = "KORUNAN SÜRE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedGray
                                    )
                                    Text(text = "⏳", fontSize = 14.sp)
                                }
                                Text(
                                    text = timeDisplayStr,
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
                        // Active Restrictions Card
                        Card(
                            onClick = { showActiveAppsDetailDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
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
                                        text = "AKTİF KORUMA",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedGray
                                    )
                                    Text(text = "🛡️", fontSize = 14.sp)
                                }
                                Text(
                                    text = "${activeApps.size} Uygulama",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PureBlack
                                )
                            }
                        }

                        // Weekly Summary Card
                        Card(
                            onClick = { showWeeklySummaryDetailDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
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
                                        text = "HAFTALIK ÖZET",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedGray
                                    )
                                    Text(text = "📅", fontSize = 14.sp)
                                }
                                Text(
                                    text = "$weeklySuccessDays/7 Gün",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PureBlack
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
                text = if (isOk) "Aktif" else "Eksik",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOk) SuccessGreen else DangerRed
            )
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Bilgi",
                    tint = MutedGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String? = null,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    infoIconClick: (() -> Unit)? = null,
    textColor: Color = PureBlack
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (value != null) {
                    Text(
                        text = value,
                        fontSize = 11.sp,
                        color = MutedGray,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (infoIconClick != null) {
                    IconButton(
                        onClick = infoIconClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Bilgi",
                            tint = MutedGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (showArrow) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MutedGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection(session: UserSessionEntity?) {
    // DashboardHeader artık ana header. Bu fonksiyon geriye uyumluluk için korunuyor.
}

enum class TimelineDateFilter(val label: String) {
    TODAY("Bugün"),
    LAST_7_DAYS("Son 7 gün"),
    LAST_30_DAYS("Son 30 gün"),
    ALL_TIME("Tüm zamanlar")
}

enum class TimelineTypeFilter(val label: String) {
    ALL("Tüm olaylar"),
    RESTRICTIONS("Kısıtlama olayları"),
    LIMITS("Limit olayları"),
    PERMISSIONS("İzin olayları"),
    DATA_ACTIONS("Veri işlemleri")
}

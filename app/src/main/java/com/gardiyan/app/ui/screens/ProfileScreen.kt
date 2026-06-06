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
                log.timestamp >= today -> "Bugün"
                log.timestamp >= yesterday -> "Dün"
                log.timestamp >= aWeekAgo -> "Bu Hafta"
                else -> "Daha Eski"
            }
        }
    }

    val activeApps = remember(restrictedApps) { restrictedApps.filter { it.isActive } }
    val todayHasViolation = remember(restrictedApps) {
        restrictedApps.any { it.isActive && it.isFailed }
    }
    val todayStatusText = when {
        activeApps.isEmpty() -> "Kısıtlama Yok"
        todayHasViolation -> "İhlal Edildi"
        else -> "Devam Ediyor"
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
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMMM yyyy HH:mm", java.util.Locale.forLanguageTag("tr")) }
    val lastSuccessDateStr = remember(lastSuccessLog) {
        lastSuccessLog?.let { dateFormat.format(java.util.Date(it.timestamp)) } ?: "Kayıt yok"
    }
    val lastViolationDateStr = remember(lastViolationLog) {
        lastViolationLog?.let { dateFormat.format(java.util.Date(it.timestamp)) } ?: "Kayıt yok"
    }

    val level = session?.level ?: 1
    val hasBadge = session?.hasRedBadge == true
    val motivationMessage = remember(level, hasBadge, todayHasViolation) {
        when {
            hasBadge -> "Her zorluk iradeni güçlendirmek için bir fırsattır. Kalkanını onarmak için korumaya sadık kal!"
            todayHasViolation -> "Bugün düştün ama pes etme. Yarın yeni bir gün, iradeni yeniden topla."
            level == 3 -> "Tebrikler Usta! Dijital iraden en üst seviyede. Diğer çaylaklara ilham oluyorsun. 👑"
            level == 2 -> "Harika gidiyorsun! Disiplinin meyvelerini vermeye başladı. Ustaya yükselmeye az kaldı. 🥈"
            else -> "İlk adımı attın! Günlük hedeflere uydukça koruman daha da güçlenecek. 🛡️"
        }
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
        progressLabel = "Kalkan Onarım İlerlemesi"
        progressText = "Onarım: $completedDays/$redemptionStreakGoal Gün"
        progressColor = DangerRed
    } else {
        progressLabel = "Rütbe İlerlemesi"
        progressColor = SuccessGreen
        when (level) {
            1 -> {
                progress = (consecutiveSuccessDays.toFloat() / 3f).coerceAtMost(1.0f)
                progressText = "Seri: $consecutiveSuccessDays/3 Gün"
            }
            2 -> {
                progress = (consecutiveSuccessDays.toFloat() / 7f).coerceAtMost(1.0f)
                progressText = "Seri: $consecutiveSuccessDays/7 Gün"
            }
            else -> {
                progress = 1.0f
                progressText = "Maksimum Rütbe"
            }
        }
    }

    val isNoDataYet = logs.isEmpty()

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
                    text = if (isSettingsVisible) "AYARLAR & SAĞLIK" else "KORUMA PROFİLİ",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    color = PureBlack,
                    letterSpacing = 0.5.sp
                )
                IconButton(
                    onClick = { isSettingsVisible = !isSettingsVisible }
                ) {
                    Icon(
                        imageVector = if (isSettingsVisible) Icons.Default.Close else Icons.Default.Settings,
                        contentDescription = "Ayarlar",
                        tint = PureBlack
                    )
                }
            }
        }

        if (isSettingsVisible) {
            item {
                val context = LocalContext.current
                val allOk = isOverlayEnabled && isUsageEnabled && isAccessibilityEnabled && isBatteryExempted && isNotificationsEnabled
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SİSTEM SAĞLIĞI",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = MutedGray,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (allOk) SuccessGreen else DangerRed)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        HealthRow(name = "Kullanım Erişimi", isOk = isUsageEnabled, onClick = { viewModel.openUsageStatsSettings(context) })
                        HealthRow(name = "Erişilebilirlik Servisi", isOk = isAccessibilityEnabled, onClick = { viewModel.openAccessibilitySettings(context) })
                        HealthRow(name = "Diğer Uygulamaların Üzerinde Çizim", isOk = isOverlayEnabled, onClick = { viewModel.openOverlaySettings(context) })
                        HealthRow(name = "Pil Optimizasyonu Muafiyeti", isOk = isBatteryExempted, onClick = { viewModel.requestBatteryOptimizationIgnore(context) })
                        HealthRow(name = "Bildirim İzni", isOk = isNotificationsEnabled, onClick = { viewModel.openNotificationSettings(context) })
                    }
                }
            }


        } else {
            // 1. Profil Hub (Görsel Avatar & XP Bar)
            item {
            val levelName = when (level) {
                1 -> "Çaylak"
                2 -> "Disiplinli"
                3 -> "Usta"
                else -> "Çaylak"
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
                        text = "LEVEL $level",
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

        // 2. Kalkan Sağlığı / Utanç Rozeti Durumu
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.2.dp,
                        color = if (hasBadge) DangerRed.copy(alpha = 0.3f) else SuccessGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (hasBadge) DangerRed.copy(alpha = 0.08f) else SuccessGreen.copy(alpha = 0.08f))
                    ) {
                        Text(text = if (hasBadge) "⚠️" else "🛡️", fontSize = 22.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasBadge) "KALKAN HASARLI" else "GÜVENLİK MAKSİMUM",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = if (hasBadge) DangerRed else SuccessGreen,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasBadge) {
                                "Denge sürecinin tamamlanması ve kalkanın tamamen onarılmasına $activeRedemptionDaysLeft gün kaldı."
                            } else {
                                "Sistem koruması aktif. Herhangi bir koruma uyarısı bulunmamaktadır."
                            },
                            fontSize = 11.sp,
                            color = MutedGray,
                            lineHeight = 16.sp
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
                                    text = "BUGÜNÜN DURUMU",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray
                                )
                                Text(text = todayStatusIcon, fontSize = 14.sp)
                            }
                            Text(
                                text = todayStatusText,
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
                                    text = "ARDIŞIK SERİ",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray
                                )
                                Text(text = "🔥", fontSize = 14.sp)
                            }
                            Text(
                                text = "$consecutiveSuccessDays Gün",
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
                                    text = "TOPLAM BAŞARI",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray
                                )
                                Text(text = "🏆", fontSize = 14.sp)
                            }
                            Text(
                                text = "$totalSuccessDays Başarı",
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
                                    text = "KORUNAN HEDEF",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray
                                )
                                Text(text = "🔒", fontSize = 14.sp)
                            }
                            Text(
                                text = "${activeApps.size} Uygulama",
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
                        text = "İSTATİSTİK DETAYLARI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedGray,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Son Başarılı Gün:", fontSize = 12.sp, color = MutedGray)
                        Text(text = lastSuccessDateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Son İhlal / Kilit:", fontSize = 12.sp, color = MutedGray)
                        Text(text = lastViolationDateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = BorderGray, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = motivationMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PureBlack,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 5. Dostane Boş Veri Bildirimleri (Gerekirse)
        if (isNoDataYet || consecutiveSuccessDays == 0 || todayStatusText == "Devam Ediyor") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MatteSurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "GARDİYAN İPUCU 💡",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedGray
                        )
                        val tipText = when {
                            isNoDataYet -> "Başarı veriniz henüz bulunmuyor. Gardiyan ile kısıtladığınız uygulamalara sadık kalarak ilk gününüzü başarıyla tamamlayın."
                            consecutiveSuccessDays == 0 -> "İlk başarılı gününüzün ardından rütbe ilerlemeniz ve seriniz burada görünmeye başlayacaktır."
                            todayStatusText == "Devam Ediyor" -> "Bugünün koruması hala aktif ve devam ediyor. Günü ihlal yapmadan bitirdiğinizde ardışık seriniz artacaktır!"
                            else -> "Korumayı sürdürerek seviyenizi artırabilir ve yeni rütbeler kazanabilirsiniz."
                        }
                        Text(
                            text = tipText,
                            fontSize = 11.sp,
                            color = MutedGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 4. Zaman Tüneli Başlığı ve Filtreler
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ZAMAN TÜNELİ",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = MutedGray,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(LogFilter.values()) { filter ->
                        val isSelected = selectedFilter == filter
                        val chipBg = if (isSelected) PureBlack else DarkCharcoal
                        val chipText = if (isSelected) Color.White else PureBlack
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
                                text = filter.displayName,
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
                            text = if (isNoDataYet) "Zaman tüneli kaydı henüz bulunmuyor." else "Bu filtre için gösterilecek kayıt bulunmuyor.",
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
                    Text(
                        text = groupName.uppercase(),
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
                        "RESTRICTION_ADDED" -> "Kısıtlama Eklendi"
                        "RESTRICTION_REMOVED" -> "Kısıtlama Kaldırıldı"
                        "RESTRICTION_DELETED" -> "Kısıtlama Silindi"
                        "QUICK_TEST_STARTED" -> "Hızlı Test Başladı"
                        "RESTRICTION_RESET" -> "Kısıtlama Sıfırlandı"
                        "RESET_HOLD_5S" -> "Tüm Kısıtlamalar Kaldırıldı"
                        "FAILURE", "DAILY_FAILURE", "VIOLATION" -> "Kısıtlama İhlali"
                        "SUCCESS", "DAILY_SUCCESS", "SUCCESS_DAY" -> "Gün Başarıyla Tamamlandı"
                        "LIMIT_CHANGED" -> "Limit Değiştirildi"
                        "DAYS_CHANGED", "ACTIVE_DAYS_CHANGED" -> "Aktif Günler Değiştirildi"
                        "OVERLAY_SHOWN", "OVERLAY_TRIGGERED" -> "Uygulama Kilitlendi"
                        "SERVICE_STARTED" -> "Koruma Başlatıldı"
                        "SERVICE_RESTARTED" -> "Koruma Yeniden Başlatıldı"
                        "SERVICE_STOPPED" -> "Koruma Durduruldu"
                        "SUSPICIOUS_STATE_DETECTED" -> "Koruma Denetimi Hızlandırıldı"
                        "ENGINE_RESYNCED" -> "Koruma Senkronize Edildi"
                        "A11Y_EVENT_RECEIVED" -> "Uygulama Girişi Algılandı"
                        "USAGE_STATS_FALLBACK" -> "Yedek Motor Denetimi"
                        "SESSION_STARTED" -> "Oturum Başladı"
                        "SESSION_UPDATED" -> "Oturum Güncellendi"
                        "SESSION_CLOSED" -> "Oturum Sonlandırıldı"
                        "STALE_SESSION_CLEANED" -> "Bayat Oturum Temizlendi"
                        "USAGE_PROCESSED" -> "Kullanım Süresi İşlendi"
                        "CRITICAL_ACTION_STARTED" -> "Kritik İşlem Başlatıldı"
                        "CRITICAL_ACTION_COMPLETED" -> "Kritik İşlem Tamamlandı"
                        "PERMISSION_CHANGED" -> "Sistem İzin Değişikliği"
                        else -> log.eventType
                    }

                    val friendlyDetails = when (log.eventType) {
                        "RESTRICTION_ADDED" -> "${log.appName} için günlük limit kısıtlaması eklendi."
                        "RESTRICTION_REMOVED" -> "${log.appName} kısıtlaması kaldırıldı."
                        "RESTRICTION_DELETED" -> "${log.appName} kısıtlaması tamamen silindi."
                        "QUICK_TEST_STARTED" -> "${log.appName} için hızlı koruma testi başlatıldı."
                        "RESTRICTION_RESET" -> "${log.appName} için günlük sayaç sıfırlandı."
                        "RESET_HOLD_5S" -> "5 saniye basılı tutularak tüm kısıtlamalar kaldırıldı."
                        "FAILURE", "DAILY_FAILURE", "VIOLATION" -> "${log.appName} kısıtlama limiti aşıldığı için kilit ekranı açıldı."
                        "SUCCESS", "DAILY_SUCCESS", "SUCCESS_DAY" -> "Bugünün hedefi başarıyla tamamlandı. Rütbeniz korundu!"
                        "LIMIT_CHANGED" -> if (log.details.isNotEmpty()) log.details else "${log.appName} günlük limiti güncellendi."
                        "DAYS_CHANGED", "ACTIVE_DAYS_CHANGED" -> if (log.details.isNotEmpty()) log.details else "${log.appName} aktif günleri güncellendi."
                        "OVERLAY_SHOWN", "OVERLAY_TRIGGERED" -> "${log.appName} kilitlendi, kilit ekranı gösterildi."
                        "SERVICE_STARTED" -> "Gardiyan koruma motoru başarıyla başlatıldı."
                        "SERVICE_RESTARTED" -> "Gardiyan koruma motoru yeniden başlatıldı."
                        "SERVICE_STOPPED" -> "Gardiyan koruma motoru durduruldu."
                        "SUSPICIOUS_STATE_DETECTED" -> "Şüpheli durum algılandı, koruma motoru denetimi hızlandırıldı."
                        "ENGINE_RESYNCED" -> "Koruma motoru durumu senkronize edildi. Normal moda dönüldü."
                        "A11Y_EVENT_RECEIVED" -> "Erişilebilirlik servisi tarafından ${log.appName} girişi algılandı."
                        "USAGE_STATS_FALLBACK" -> "Yedek denetim motoru: ${log.appName} için ön plan doğrulaması yapıldı."
                        "SESSION_STARTED" -> "${log.appName} koruma oturumu başlatıldı."
                        "SESSION_UPDATED" -> "${log.appName} koruma oturumu güncellendi."
                        "SESSION_CLOSED" -> "${log.appName} koruma oturumu sonlandırıldı."
                        "STALE_SESSION_CLEANED" -> "${log.appName} bayat oturumu temizlendi."
                        "USAGE_PROCESSED" -> "${log.appName} için kullanım süresi işlendi."
                        "CRITICAL_ACTION_STARTED" -> "${log.appName} silme işlemi basılı tutularak başlatıldı."
                        "CRITICAL_ACTION_COMPLETED" -> "${log.appName} silme işlemi onaylandı."
                        "PERMISSION_CHANGED" -> log.details
                        else -> log.details
                    }

                    val statusIcon = when (log.eventType) {
                        "FAILURE", "DAILY_FAILURE", "VIOLATION" -> "⚠️"
                        "RESET_HOLD_5S", "CRITICAL_ACTION_STARTED" -> "🔓"
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

        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HealthRow(
    name: String,
    isOk: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOk) { onClick() }
            .padding(vertical = 4.dp),
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PureBlack
            )
        }
        if (!isOk) {
            Text(
                text = "Düzelt",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = DangerRed,
                modifier = Modifier
                    .border(0.8.dp, DangerRed, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        } else {
            Text(
                text = "Aktif",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = SuccessGreen
            )
        }
    }
}

@Composable
fun HeaderSection(session: UserSessionEntity?) {
    // DashboardHeader artık ana header. Bu fonksiyon geriye uyumluluk için korunuyor.
}

enum class LogFilter(val displayName: String) {
    TODAY("Bugün"),
    SUCCESSES("Başarılar"),
    VIOLATIONS("İhlaller"),
    CHANGES("Kısıtlama Değişiklikleri"),
    LOCKS("Kilit / Ekran Olayları"),
    CANCELS("İptal / Silme Girişimleri"),
    ALL("Tümü")
}

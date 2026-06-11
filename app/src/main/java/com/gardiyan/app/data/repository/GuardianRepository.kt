package com.gardiyan.app.data.repository

import android.content.Context
import com.gardiyan.app.data.local.dao.GuardianDao
import com.gardiyan.app.data.local.entity.ActiveUsageSessionEntity
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.data.local.entity.StatusLogEntity
import com.gardiyan.app.data.local.entity.UserSessionEntity
import com.gardiyan.app.data.time.TimeProvider
import com.gardiyan.app.data.time.SystemTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GuardianRepository(
    private val context: Context,
    private val guardianDao: GuardianDao,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {

    private val resetMutex = Mutex()

    companion object {
        const val ALL_DAYS = "Pzt,Sal,Çar,Per,Cum,Cmt,Paz"
        const val MAX_DAILY_LIMIT_MINUTES = 23 * 60 + 59

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun todayKey(): String = dateFormat.format(Date())

        fun todayDayLabel(): String {
            return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Pzt"
                Calendar.TUESDAY -> "Sal"
                Calendar.WEDNESDAY -> "Çar"
                Calendar.THURSDAY -> "Per"
                Calendar.FRIDAY -> "Cum"
                Calendar.SATURDAY -> "Cmt"
                Calendar.SUNDAY -> "Paz"
                else -> ""
            }
        }
    }

    val userSession: Flow<UserSessionEntity?> = guardianDao.getUserSession()
    val allLogs: Flow<List<StatusLogEntity>> = guardianDao.getAllLogs()
    val restrictedApps: Flow<List<RestrictedAppEntity>> = guardianDao.getAllRestrictedApps()

    suspend fun getSessionSync(): UserSessionEntity? = guardianDao.getUserSessionSync()

    suspend fun insertDefaultSessionIfMissing() {
        val current = guardianDao.getUserSessionSync()
        if (current == null) {
            guardianDao.insertUserSession(
                UserSessionEntity(
                    id = 1,
                    username = "LimitraUser",
                    level = 1,
                    hasRedBadge = false,
                    isActive = false
                )
            )
        }
    }

    suspend fun saveSession(session: UserSessionEntity) {
        guardianDao.insertUserSession(session)
    }

    suspend fun insertLog(
        eventType: String,
        appName: String,
        details: String,
        customTimestamp: Long? = null
    ) {
        val technicalTypes = setOf(
            "SESSION_STARTED",
            "SESSION_UPDATED",
            "SESSION_CLOSED",
            "STALE_SESSION_CLEANED",
            "USAGE_PROCESSED",
            "ENGINE_RESYNCED",
            "SUSPICIOUS_STATE_DETECTED",
            "A11Y_EVENT_RECEIVED",
            "USAGE_STATS_FALLBACK",
            "SERVICE_RESTARTED"
        )
        if (eventType in technicalTypes) {
            return
        }
        guardianDao.insertLog(
            StatusLogEntity(
                eventType = eventType,
                appName = appName,
                details = details,
                timestamp = customTimestamp ?: System.currentTimeMillis()
            )
        )
    }

    suspend fun clearLogs() {
        guardianDao.clearLogs()
    }

    suspend fun clearActiveSessions() {
        guardianDao.clearActiveSessions()
    }

    suspend fun getActiveRestrictedAppsSync(): List<RestrictedAppEntity> {
        resetDailyCountersIfNeeded()
        return guardianDao.getActiveRestrictedAppsSync()
    }

    suspend fun getActiveRestrictedAppsForTodaySync(): List<RestrictedAppEntity> {
        val today = timeProvider.todayDayLabel()
        return getActiveRestrictedAppsSync().filter { app ->
            val days = app.activeDays.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            days.isEmpty() || days.contains(today)
        }
    }

    suspend fun getAllRestrictedAppsSync(): List<RestrictedAppEntity> {
        resetDailyCountersIfNeeded()
        return guardianDao.getAllRestrictedAppsSync()
    }

    suspend fun getRestrictedAppByPackageSync(pkg: String): RestrictedAppEntity? =
        guardianDao.getRestrictedAppByPackageSync(pkg)

    suspend fun getRestrictedAppByIdSync(id: Long): RestrictedAppEntity? =
        guardianDao.getRestrictedAppByIdSync(id)

    suspend fun upsertRestrictedApp(
        packageName: String,
        appName: String,
        dailyLimitMinutes: Int,
        activeDays: String = ALL_DAYS
    ): Long {
        val today = timeProvider.localDateString()
        val safeDailyLimitMinutes = dailyLimitMinutes.coerceIn(1, MAX_DAILY_LIMIT_MINUTES)
        val existing = guardianDao.getRestrictedAppByPackageSync(packageName)
        return if (existing != null) {
            guardianDao.updateRestrictedApp(
                existing.copy(
                    appName = appName,
                    dailyLimitMinutes = safeDailyLimitMinutes,
                    remainingMinutesToday = safeDailyLimitMinutes,
                    remainingSecondsToday = safeDailyLimitMinutes * 60,
                    isActive = true,
                    isFailed = false,
                    activeDays = activeDays,
                    lastResetDate = today,
                    nextDayLimitMinutes = 0,
                    nextDayActiveDays = "",
                    lastLimitUpdateDate = today,
                    todayMinLimitMinutes = safeDailyLimitMinutes
                )
            )
            existing.id
        } else {
            guardianDao.insertRestrictedApp(
                RestrictedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    dailyLimitMinutes = safeDailyLimitMinutes,
                    remainingMinutesToday = safeDailyLimitMinutes,
                    remainingSecondsToday = safeDailyLimitMinutes * 60,
                    isActive = true,
                    isFailed = false,
                    activeDays = activeDays,
                    lastResetDate = today,
                    lastLimitUpdateDate = today,
                    todayMinLimitMinutes = safeDailyLimitMinutes
                )
            )
        }
    }

    suspend fun insertQuickTestApp(
        packageName: String,
        appName: String,
        testSeconds: Int,
        activeDays: String = ALL_DAYS
    ): Long {
        val today = timeProvider.localDateString()
        val existing = guardianDao.getRestrictedAppByPackageSync(packageName)
        if (!com.gardiyan.app.BuildConfig.DEBUG) {
            if (existing != null) {
                return existing.id
            }
        }
        val safeTestSeconds = testSeconds.coerceIn(1, MAX_DAILY_LIMIT_MINUTES * 60)
        val dailyLimitMinutes = (safeTestSeconds + 59) / 60
        return if (existing != null) {
            guardianDao.updateRestrictedApp(
                existing.copy(
                    appName = appName,
                    dailyLimitMinutes = dailyLimitMinutes,
                    remainingMinutesToday = safeTestSeconds / 60,
                    remainingSecondsToday = safeTestSeconds,
                    isActive = true,
                    isFailed = false,
                    activeDays = activeDays,
                    lastResetDate = today,
                    nextDayLimitMinutes = 0,
                    nextDayActiveDays = ""
                )
            )
            existing.id
        } else {
            guardianDao.insertRestrictedApp(
                RestrictedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    dailyLimitMinutes = dailyLimitMinutes,
                    remainingMinutesToday = safeTestSeconds / 60,
                    remainingSecondsToday = safeTestSeconds,
                    isActive = true,
                    isFailed = false,
                    activeDays = activeDays,
                    lastResetDate = today
                )
            )
        }
    }

    suspend fun removeRestrictedApp(id: Long) {
        guardianDao.deleteRestrictedAppById(id)
    }

    suspend fun clearAllRestrictedApps() {
        guardianDao.clearAllRestrictedApps()
    }

    suspend fun markRestrictedAppFailed(id: Long) {
        guardianDao.markRestrictedAppFailed(id)
    }

    suspend fun deactivateAllRestrictedApps() {
        guardianDao.deactivateAllRestrictedApps()
    }

    suspend fun resetRestrictedApp(id: Long) {
        guardianDao.resetRestrictedApp(id, timeProvider.localDateString())
    }

    suspend fun updateRestrictedApp(app: RestrictedAppEntity) {
        guardianDao.updateRestrictedApp(app)
    }

    suspend fun resetDailyCountersIfNeeded() {
        resetMutex.withLock {
            val prefs = context.getSharedPreferences("gardiyan_secure_reset_prefs", Context.MODE_PRIVATE)
            val lastResetLocalDate = prefs.getString("lastResetLocalDate", "") ?: ""
            val lastResetWallClockMillis = prefs.getLong("lastResetWallClockMillis", 0L)
            val lastResetElapsedRealtime = prefs.getLong("lastResetElapsedRealtime", 0L)
            
            // Son bilinen güvenli duvar saati kontrolü (geriye manipülasyon tespiti için)
            val lastKnownWallClockMillis = prefs.getLong("lastKnownWallClockMillis", 0L)
            
            val today = timeProvider.localDateString()
            val nowWall = timeProvider.currentTimeMillis()
            val nowElapsed = timeProvider.elapsedRealtime()
            
            val isFirstInit = lastResetLocalDate.isEmpty() || lastResetWallClockMillis == 0L
            val isDateChanged = isFirstInit || (today != lastResetLocalDate)
            
            var isTimePassed = isFirstInit
            val minIntervalMillis = 22 * 3600 * 1000L // 22 saat sıfırlama kilidi
            
            if (!isFirstInit) {
                // 1. Cihaz açıkken saati geriye alma kontrolü
                val isTimeManipulatedBackwards = nowWall < lastResetWallClockMillis || (lastKnownWallClockMillis > 0L && nowWall < lastKnownWallClockMillis)
                
                if (isTimeManipulatedBackwards) {
                    if (com.gardiyan.app.BuildConfig.DEBUG) {
                        android.util.Log.w("GuardianRepository", "Geri zaman manipülasyonu engellendi. nowWall: $nowWall, lastResetWallClock: $lastResetWallClockMillis, lastKnownWall: $lastKnownWallClockMillis")
                    }
                    insertLog("SECURITY_WARNING", "", "Sistem saatinin geriye alındığı tespit edildi. Limitler sıfırlanmadı.")
                    return
                }
                
                val isRebooted = nowElapsed < lastResetElapsedRealtime
                
                if (!isRebooted) {
                    val elapsedDiff = nowElapsed - lastResetElapsedRealtime
                    if (elapsedDiff >= minIntervalMillis) {
                        isTimePassed = true
                    }
                    if (com.gardiyan.app.BuildConfig.DEBUG) {
                        android.util.Log.d("GuardianRepository", "Zaman geçiş kontrolü (Monotonic): elapsedDiff=${elapsedDiff}ms, min=${minIntervalMillis}ms, isTimePassed=$isTimePassed")
                    }
                } else {
                    // 2. Reboot sonrası zaman manipülasyonu kontrolü (Boot Time analizi)
                    val oldBootTime = lastResetWallClockMillis - lastResetElapsedRealtime
                    val newBootTime = nowWall - nowElapsed
                    val bootTimeDiff = newBootTime - oldBootTime
                    
                    // Boot Time farkı 15 dakikadan fazla geride ise geriye manipülasyon vardır
                    if (bootTimeDiff < -15 * 60 * 1000L) {
                        if (com.gardiyan.app.BuildConfig.DEBUG) {
                            android.util.Log.w("GuardianRepository", "Reboot sonrası geri zaman manipülasyonu engellendi. bootTimeDiff: $bootTimeDiff")
                        }
                        insertLog("SECURITY_WARNING", "", "Reboot sonrası sistem saatinin geriye alındığı tespit edildi. Limitler sıfırlanmadı.")
                        return
                    }
                    
                    val wallDiff = nowWall - lastResetWallClockMillis
                    if (wallDiff >= minIntervalMillis) {
                        isTimePassed = true
                    }
                    if (com.gardiyan.app.BuildConfig.DEBUG) {
                        android.util.Log.d("GuardianRepository", "Zaman geçiş kontrolü (Wall clock/Rebooted): wallDiff=${wallDiff}ms, min=${minIntervalMillis}ms, isTimePassed=$isTimePassed")
                    }
                }
            }
            
            if (com.gardiyan.app.BuildConfig.DEBUG) {
                android.util.Log.d("GuardianRepository", "resetDailyCountersIfNeeded: isDateChanged=$isDateChanged, isTimePassed=$isTimePassed, isFirstInit=$isFirstInit, today=$today, lastResetLocalDate=$lastResetLocalDate")
            }

            if (isDateChanged && isTimePassed) {
                val restrictedApps = guardianDao.getAllRestrictedAppsSync()
                var anyReset = false
                restrictedApps.forEach { app ->
                    if (app.lastResetDate != today) {
                        anyReset = true
                        val newLimit = if (app.nextDayLimitMinutes > 0) app.nextDayLimitMinutes else app.dailyLimitMinutes
                        val newActiveDays = app.nextDayActiveDays.ifEmpty { app.activeDays }
                        if (com.gardiyan.app.BuildConfig.DEBUG) {
                            android.util.Log.d("GuardianRepository", "Sıfırlanıyor: ${app.appName} (${app.packageName}), yeni limit: ${newLimit}dk")
                        }
                        guardianDao.updateRestrictedApp(
                            app.copy(
                                dailyLimitMinutes = newLimit,
                                remainingMinutesToday = newLimit,
                                remainingSecondsToday = newLimit * 60,
                                isFailed = false,
                                activeDays = newActiveDays,
                                lastResetDate = today,
                                nextDayLimitMinutes = 0,
                                nextDayActiveDays = "",
                                lastLimitUpdateDate = "",
                                todayMinLimitMinutes = 0
                            )
                        )
                    }
                }
                
                if (anyReset && !isFirstInit) {
                    val notifPrefs = context.getSharedPreferences("gardiyan_notifications", Context.MODE_PRIVATE)
                    notifPrefs.edit().clear().apply()
                }
                
                prefs.edit().apply {
                    putString("lastResetLocalDate", today)
                    putLong("lastResetWallClockMillis", nowWall)
                    putLong("lastResetElapsedRealtime", nowElapsed)
                    putLong("lastKnownWallClockMillis", nowWall)
                    putString("lastResetTimezoneId", timeProvider.timezoneId())
                    apply()
                }
                
                if (anyReset && !isFirstInit) {
                    insertLog("DAILY_RESET", "", "Günlük limitler sıfırlandı. Yeni gün: $today")
                }
            } else if (isDateChanged && !isTimePassed) {
                if (com.gardiyan.app.BuildConfig.DEBUG) {
                    android.util.Log.w("GuardianRepository", "Erken günlük sıfırlama engellendi! Zaman aşımı yetersiz.")
                }
                insertLog("RESET_PREVENTED", "", "Tarih değişimi algılandı ancak güvenlik süresi henüz dolmadı. Limitlerin erken sıfırlanması engellendi.")
            } else {
                // Sadece son bilinen duvar saatini güncelle
                prefs.edit().putLong("lastKnownWallClockMillis", nowWall).apply()
            }
        }
    }

    suspend fun cancelAllActiveTargets() {
        val session = getSessionSync() ?: return
        val activeApps = guardianDao.getActiveRestrictedAppsSync()
        if (activeApps.isEmpty()) return

        guardianDao.deactivateAllRestrictedApps()

        guardianDao.insertUserSession(
            session.copy(
                isActive = false,
                level = 1,
                hasRedBadge = true,
                activeRedemptionDaysLeft = 2,
                redemptionStreakGoal = 2,
                consecutiveSuccessDays = 0
            )
        )

        val appNames = activeApps.joinToString(", ") { it.appName }
        insertLog(
            eventType = "CRITICAL_ACTION_COMPLETED",
            appName = appNames,
            details = "Tüm kısıtlamalar korumalı bir işlemle kaldırıldı."
        )
    }

    suspend fun failRestrictedApp(appId: Long) {
        val app = guardianDao.getRestrictedAppByIdSync(appId) ?: return
        if (app.isFailed) return

        guardianDao.markRestrictedAppFailed(appId)

        val session = getSessionSync()
        if (session != null) {
            guardianDao.insertUserSession(
                session.copy(
                    level = 1,
                    hasRedBadge = true,
                    activeRedemptionDaysLeft = 2,
                    redemptionStreakGoal = 2,
                    consecutiveSuccessDays = 0
                )
            )
        }

        insertLog(
            eventType = "VIOLATION",
            appName = app.appName,
            details = "${app.appName} hedefinde süre doldu. Level 1'e düşürüldünüz. Kilit devam ediyor."
        )
    }

    suspend fun failActiveTarget() {
        val activeApps = guardianDao.getActiveRestrictedAppsSync()
        activeApps.forEach { app ->
            if (!app.isFailed) {
                failRestrictedApp(app.id)
            }
        }
    }

    suspend fun succeedActiveTarget(customTimestamp: Long? = null) {
        val session = getSessionSync() ?: return

        val nextStreak = session.consecutiveSuccessDays + 1
        var nextLevel = session.level
        var nextRedBadge = session.hasRedBadge
        var nextRedemptionDays = session.activeRedemptionDaysLeft
        var logDetails = "Günlük hedef başarıyla tamamlandı."

        if (nextRedBadge && nextRedemptionDays > 0) {
            nextRedemptionDays -= 1
            if (nextRedemptionDays <= 0) {
                nextRedBadge = false
                logDetails += " Başarılı telafi serisi sonucu kalkan uyarısı kaldırıldı."
            } else {
                logDetails += " Hasarlı kalkanı onarmak için $nextRedemptionDays başarılı gün kaldı."
            }
        }

        if (session.level == 1 && nextStreak >= 3) {
            nextLevel = 2
            logDetails += " 3 günlük seriyle Disiplinli rütbesine ulaştınız."
        } else if (session.level == 2 && nextStreak >= 7) {
            nextLevel = 3
            logDetails += " 7 günlük seriyle Usta rütbesine yükseldiniz."
        }

        guardianDao.insertUserSession(
            session.copy(
                isActive = session.isActive,
                level = nextLevel,
                hasRedBadge = nextRedBadge,
                activeRedemptionDaysLeft = nextRedemptionDays,
                consecutiveSuccessDays = nextStreak,
                remainingMinutesToday = session.dailyLimitMinutes
            )
        )
        insertLog(
            eventType = "SUCCESS_DAY",
            appName = session.targetAppName,
            details = logDetails,
            customTimestamp = customTimestamp
        )
    }

    suspend fun evaluateDailySuccess(dateKey: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = try { sdf.parse(dateKey) } catch (e: Exception) { null } ?: return false

        val cal = Calendar.getInstance()
        cal.time = date
        val dayLabel = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Pzt"
            Calendar.TUESDAY -> "Sal"
            Calendar.WEDNESDAY -> "Çar"
            Calendar.THURSDAY -> "Per"
            Calendar.FRIDAY -> "Cum"
            Calendar.SATURDAY -> "Cmt"
            Calendar.SUNDAY -> "Paz"
            else -> ""
        }

        val allApps = guardianDao.getAllRestrictedAppsSync()
        val activeAppsOnDay = allApps.filter { app ->
            val days = app.activeDays.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val isDayActive = days.isEmpty() || days.contains(dayLabel)

            val calEnd = Calendar.getInstance()
            calEnd.time = date
            calEnd.set(Calendar.HOUR_OF_DAY, 23)
            calEnd.set(Calendar.MINUTE, 59)
            calEnd.set(Calendar.SECOND, 59)

            app.isActive && isDayActive && app.createdAtMillis <= calEnd.timeInMillis
        }

        val calStart = Calendar.getInstance()
        calStart.time = date
        calStart.set(Calendar.HOUR_OF_DAY, 0)
        calStart.set(Calendar.MINUTE, 0)
        calStart.set(Calendar.SECOND, 0)
        calStart.set(Calendar.MILLISECOND, 0)

        val calEnd = Calendar.getInstance()
        calEnd.time = date
        calEnd.set(Calendar.HOUR_OF_DAY, 23)
        calEnd.set(Calendar.MINUTE, 59)
        calEnd.set(Calendar.SECOND, 59)
        calEnd.set(Calendar.MILLISECOND, 999)

        val startTime = calStart.timeInMillis
        val endTime = calEnd.timeInMillis

        val logs = guardianDao.getAllLogsSync()

        // Zaten SUCCESS_DAY kaydedilmişse değerlendirmeyi tekrarlama, direkt true dön
        val hasSuccessDay = logs.any { log ->
            log.timestamp in startTime..endTime && log.eventType == "SUCCESS_DAY"
        }
        if (hasSuccessDay) {
            return true
        }

        // Zaten ihlal tespit edilmişse tekrar işlem yapma
        val hasExistingViolation = logs.any { log ->
            log.timestamp in startTime..endTime && log.eventType == "VIOLATION" && log.details.contains("kısıtlama ihlali tespit edildi")
        }
        if (hasExistingViolation) {
            return false
        }

        val hasViolation = logs.any { log ->
            log.timestamp in startTime..endTime && log.eventType in setOf(
                "FAILURE",
                "VIOLATION",
                "RESET_HOLD_5S",
                "CRITICAL_ACTION_COMPLETED",
                "RESTRICTION_REMOVED",
                "RESTRICTION_DELETED",
                "RESTRICTIONS_CLEARED"
            )
        }

        if (hasViolation) {
            insertLog(
                eventType = "VIOLATION",
                appName = "",
                details = "$dateKey tarihinde kısıtlama ihlali tespit edildi. Günlük hedef başarısız.",
                customTimestamp = endTime
            )

            val session = getSessionSync()
            if (session != null) {
                guardianDao.insertUserSession(
                    session.copy(
                        level = 1,
                        hasRedBadge = true,
                        activeRedemptionDaysLeft = 2,
                        redemptionStreakGoal = 2,
                        consecutiveSuccessDays = 0
                    )
                )
            }
            return false
        } else {
            if (activeAppsOnDay.isEmpty()) {
                return false
            }

            // İzinler/servisin o gün çalıştığına dair kanıt var mı?
            val hasActiveLog = logs.any { log ->
                log.timestamp in startTime..endTime && log.eventType == "ENGINE_ACTIVE"
            }
            if (!hasActiveLog) {
                // Güvenilir kanıt yoksa günü değerlendirilemedi say ve false dön, ceza verme ama seriyi sıfırla
                val session = getSessionSync()
                if (session != null) {
                    guardianDao.insertUserSession(
                        session.copy(
                            consecutiveSuccessDays = 0
                        )
                    )
                }
                return false
            }

            succeedActiveTarget(endTime)
            return true
        }
    }

    private val lastSessionUpdateLogTime = mutableMapOf<String, Long>()

    suspend fun getActiveSession(): ActiveUsageSessionEntity? {
        return guardianDao.getActiveSessionSync()
    }

    suspend fun startSession(app: RestrictedAppEntity) {
        closeActiveSession("Yeni oturum başlatılıyor")
        val now = System.currentTimeMillis()
        val session = ActiveUsageSessionEntity(
            appId = app.id,
            packageName = app.packageName,
            appName = app.appName,
            entryAtMillis = now,
            lastSeenAtMillis = now,
            isActive = true,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        guardianDao.insertActiveSession(session)
        insertLog("SESSION_STARTED", app.appName, "Koruma oturumu veritabanında başlatıldı.")
    }

    suspend fun updateSessionLastSeen(packageName: String) {
        val session = guardianDao.getActiveSessionSync() ?: return
        if (session.packageName == packageName) {
            val now = System.currentTimeMillis()
            val deltaMs = now - session.lastSeenAtMillis
            
            if (deltaMs > 30000L) {
                // Anormal gecikme durumunda zamanlayıcıyı sıfırla, süre düşme ama izlemeyi güncelle
                if (com.gardiyan.app.BuildConfig.DEBUG) {
                    android.util.Log.d("GuardianRepository", "Anormal gecikme tespit edildi (delta: ${deltaMs}ms), süre düşülmeden lastSeen güncellendi.")
                }
                val updated = session.copy(
                    lastSeenAtMillis = now,
                    updatedAtMillis = now
                )
                guardianDao.updateActiveSession(updated)
                return
            }

            val deltaSec = (deltaMs / 1000L).toInt()
            if (deltaSec > 0) {
                val app = guardianDao.getRestrictedAppByIdSync(session.appId)
                if (app != null && app.isActive && !app.isFailed) {
                    val newRemaining = (app.remainingSecondsToday - deltaSec).coerceAtLeast(0)
                    if (com.gardiyan.app.BuildConfig.DEBUG) {
                        android.util.Log.d("GuardianRepository", "Süre düşülüyor: Paket=$packageName, Geçen saniye=$deltaSec, Kalan saniye=$newRemaining, usedToday=${app.dailyLimitMinutes * 60 - newRemaining}")
                    }
                    guardianDao.updateRestrictedApp(
                        app.copy(
                            remainingSecondsToday = newRemaining,
                            remainingMinutesToday = newRemaining / 60
                        )
                    )
                    if (newRemaining <= 0) {
                        if (com.gardiyan.app.BuildConfig.DEBUG) {
                            android.util.Log.d("GuardianRepository", "Limit doldu! failRestrictedApp tetikleniyor. Paket=$packageName")
                        }
                        failRestrictedApp(app.id)
                    }
                }

                // session'ı son görülme zamanıyla güncelle (artık milisaniyeleri koruyarak)
                val updated = session.copy(
                    lastSeenAtMillis = now - (deltaMs % 1000L),
                    updatedAtMillis = now
                )
                guardianDao.updateActiveSession(updated)
            } else {
                if (com.gardiyan.app.BuildConfig.DEBUG) {
                    android.util.Log.v("GuardianRepository", "Milisaniye birikiyor, saniye değişmedi. deltaMs=$deltaMs, paket=$packageName")
                }
            }

            val lastLogTime = lastSessionUpdateLogTime[packageName] ?: 0L
            if (now - lastLogTime > 30000L) {
                insertLog("SESSION_UPDATED", session.appName, "Koruma oturumu güncellendi (aktif kalma devam ediyor).")
                lastSessionUpdateLogTime[packageName] = now
            }
        }
    }

    suspend fun closeActiveSession(reason: String) {
        val session = guardianDao.getActiveSessionSync() ?: return
        val now = System.currentTimeMillis()
        val elapsedMs = now - session.lastSeenAtMillis
        val elapsedSec = (elapsedMs / 1000L).toInt().coerceIn(0, 5)

        val app = guardianDao.getRestrictedAppByIdSync(session.appId)
        if (app != null && app.isActive && !app.isFailed) {
            val newRemaining = (app.remainingSecondsToday - elapsedSec).coerceAtLeast(0)
            val newMinutes = newRemaining / 60
            guardianDao.updateRestrictedApp(
                app.copy(
                    remainingSecondsToday = newRemaining,
                    remainingMinutesToday = newMinutes
                )
            )
            insertLog("USAGE_PROCESSED", app.appName, "Oturumdan düşülen süre: ${elapsedSec} saniye.")
            if (newRemaining <= 0) {
                failRestrictedApp(app.id)
            }
        }

        val closed = session.copy(
            isActive = false,
            lastSeenAtMillis = now,
            updatedAtMillis = now
        )
        guardianDao.updateActiveSession(closed)
        insertLog("SESSION_CLOSED", session.appName, "Koruma oturumu kapatıldı ($reason).")
    }

    suspend fun cleanupStaleSessions() {
        val sessions = guardianDao.getActiveSessionsSync()
        val now = System.currentTimeMillis()
        for (session in sessions) {
            if (now - session.lastSeenAtMillis > 30000L) {
                val elapsedMs = now - session.lastSeenAtMillis
                val elapsedSec = (elapsedMs / 1000L).toInt().coerceIn(0, 5)

                val app = guardianDao.getRestrictedAppByIdSync(session.appId)
                if (app != null && app.isActive && !app.isFailed) {
                    val newRemaining = (app.remainingSecondsToday - elapsedSec).coerceAtLeast(0)
                    val newMinutes = newRemaining / 60
                    guardianDao.updateRestrictedApp(
                        app.copy(
                            remainingSecondsToday = newRemaining,
                            remainingMinutesToday = newMinutes
                        )
                    )
                    insertLog("USAGE_PROCESSED", app.appName, "Bayat oturumdan düşülen süre: ${elapsedSec} saniye.")
                    if (newRemaining <= 0) {
                        failRestrictedApp(app.id)
                    }
                }

                val closed = session.copy(
                    isActive = false,
                    updatedAtMillis = now
                )
                guardianDao.updateActiveSession(closed)
                insertLog("STALE_SESSION_CLEANED", session.appName, "Bayatlayan aktif oturum temizlendi.")
            }
        }
    }

    suspend fun evaluateMissedDays() {
        val sharedPref = context.getSharedPreferences("gardiyan_eval_prefs", Context.MODE_PRIVATE)
        val lastEvaluated = sharedPref.getString("last_evaluated_date", "") ?: ""
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val yesterdayCal = Calendar.getInstance()
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = sdf.format(yesterdayCal.time)
        
        if (lastEvaluated == yesterdayKey) {
            return
        }
        
        val datesToEvaluate = mutableListOf<String>()
        val lastDate = try {
            if (lastEvaluated.isNotEmpty()) sdf.parse(lastEvaluated) else null
        } catch (e: Exception) {
            null
        }
        
        if (lastDate == null) {
            datesToEvaluate.add(yesterdayKey)
        } else {
            val startCal = Calendar.getInstance()
            startCal.time = lastDate
            startCal.add(Calendar.DAY_OF_YEAR, 1)
            
            val yesterdayCompare = Calendar.getInstance()
            yesterdayCompare.time = yesterdayCal.time
            
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)
            
            yesterdayCompare.set(Calendar.HOUR_OF_DAY, 0)
            yesterdayCompare.set(Calendar.MINUTE, 0)
            yesterdayCompare.set(Calendar.SECOND, 0)
            yesterdayCompare.set(Calendar.MILLISECOND, 0)
            
            while (!startCal.after(yesterdayCompare)) {
                val key = sdf.format(startCal.time)
                datesToEvaluate.add(key)
                startCal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        for (dateKey in datesToEvaluate) {
            evaluateDailySuccess(dateKey)
        }
        
        sharedPref.edit().putString("last_evaluated_date", yesterdayKey).apply()
    }
}

package com.gardiyan.app.data.repository

import com.gardiyan.app.data.local.dao.GuardianDao
import com.gardiyan.app.data.local.entity.ActiveUsageSessionEntity
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.data.local.entity.StatusLogEntity
import com.gardiyan.app.data.local.entity.UserSessionEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GuardianRepository(private val guardianDao: GuardianDao) {

    companion object {
        const val ALL_DAYS = "Pzt,Sal,Çar,Per,Cum,Cmt,Paz"

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

    suspend fun insertLog(eventType: String, appName: String, details: String) {
        guardianDao.insertLog(
            StatusLogEntity(
                eventType = eventType,
                appName = appName,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearLogs() {
        guardianDao.clearLogs()
    }

    suspend fun getActiveRestrictedAppsSync(): List<RestrictedAppEntity> {
        resetDailyCountersIfNeeded()
        return guardianDao.getActiveRestrictedAppsSync()
    }

    suspend fun getActiveRestrictedAppsForTodaySync(): List<RestrictedAppEntity> {
        val today = todayDayLabel()
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
        val today = todayKey()
        val existing = guardianDao.getRestrictedAppByPackageSync(packageName)
        return if (existing != null) {
            if (existing.isActive) {
                return existing.id
            }
            guardianDao.updateRestrictedApp(
                existing.copy(
                    appName = appName,
                    dailyLimitMinutes = dailyLimitMinutes,
                    remainingMinutesToday = dailyLimitMinutes,
                    remainingSecondsToday = dailyLimitMinutes * 60,
                    isActive = true,
                    isFailed = false,
                    activeDays = activeDays,
                    lastResetDate = today,
                    nextDayLimitMinutes = 0,
                    nextDayActiveDays = "",
                    lastLimitUpdateDate = today,
                    todayMinLimitMinutes = dailyLimitMinutes
                )
            )
            existing.id
        } else {
            guardianDao.insertRestrictedApp(
                RestrictedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    dailyLimitMinutes = dailyLimitMinutes,
                    remainingMinutesToday = dailyLimitMinutes,
                    remainingSecondsToday = dailyLimitMinutes * 60,
                    isActive = true,
                    isFailed = false,
                    activeDays = activeDays,
                    lastResetDate = today,
                    lastLimitUpdateDate = today,
                    todayMinLimitMinutes = dailyLimitMinutes
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
        val today = todayKey()
        val existing = guardianDao.getRestrictedAppByPackageSync(packageName)
        val dailyLimitMinutes = (testSeconds + 59) / 60
        return if (existing != null) {
            if (existing.isActive) {
                return existing.id
            }
            guardianDao.updateRestrictedApp(
                existing.copy(
                    appName = appName,
                    dailyLimitMinutes = dailyLimitMinutes,
                    remainingMinutesToday = testSeconds / 60,
                    remainingSecondsToday = testSeconds,
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
                    remainingMinutesToday = testSeconds / 60,
                    remainingSecondsToday = testSeconds,
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
        guardianDao.resetRestrictedApp(id, todayKey())
    }

    suspend fun updateRestrictedApp(app: RestrictedAppEntity) {
        guardianDao.updateRestrictedApp(app)
    }

    suspend fun resetDailyCountersIfNeeded() {
        val today = todayKey()
        guardianDao.getAllRestrictedAppsSync()
            .filter { it.lastResetDate != today }
            .forEach { app ->
                val newLimit = if (app.nextDayLimitMinutes > 0) app.nextDayLimitMinutes else app.dailyLimitMinutes
                val newActiveDays = app.nextDayActiveDays.ifEmpty { app.activeDays }
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

    suspend fun succeedActiveTarget() {
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
                isActive = false,
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
            details = logDetails
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
                details = "$dateKey tarihinde kısıtlama ihlali tespit edildi. Günlük hedef başarısız."
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
            succeedActiveTarget()
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
            val updated = session.copy(
                lastSeenAtMillis = now,
                updatedAtMillis = now
            )
            guardianDao.updateActiveSession(updated)

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
        val elapsedMs = now - session.entryAtMillis
        val elapsedSec = (elapsedMs / 1000L).toInt().coerceAtLeast(0)

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
                val elapsedMs = session.lastSeenAtMillis - session.entryAtMillis
                val elapsedSec = (elapsedMs / 1000L).toInt().coerceAtLeast(0)

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
}

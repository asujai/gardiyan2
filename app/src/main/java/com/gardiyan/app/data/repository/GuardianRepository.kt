package com.gardiyan.app.data.repository

import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.os.Build
import com.gardiyan.app.data.local.dao.GuardianDao
import com.gardiyan.app.data.local.entity.ActiveUsageSessionEntity
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.data.local.entity.StatusLogEntity
import com.gardiyan.app.data.local.entity.UserSessionEntity
import com.gardiyan.app.data.model.isScheduledAt
import com.gardiyan.app.data.model.RestrictionSchedule
import com.gardiyan.app.data.time.TimeProvider
import com.gardiyan.app.data.time.SystemTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UsageStatsReconciliationResult(
    val appId: Long,
    val packageName: String,
    val appName: String,
    val adjustedSeconds: Int,
    val remainingSecondsToday: Int,
    val baselineInitialized: Boolean = false
)

private data class DailyResetUsageSnapshot(
    val usedSecondsToday: Int,
    val usageStatsBaselineMillisToday: Long,
    val lastUsageStatsObservedMillisToday: Long,
    val lastUsageStatsReconciledAtMillis: Long
)

class GuardianRepository(
    private val context: Context,
    private val guardianDao: GuardianDao,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {

    private val resetMutex = Mutex()

    companion object {
        const val ALL_DAYS = "Pzt,Sal,Çar,Per,Cum,Cmt,Paz"
        const val MAX_DAILY_LIMIT_MINUTES = 23 * 60 + 59
        const val UNKNOWN_USAGE_STATS_BASELINE = -1L
        const val USAGE_STATS_RECONCILE_TOLERANCE_SECONDS = 10
        const val USAGE_EVENTS_STATE_LOOKBACK_MS = 24 * 60 * 60 * 1000L

        // Duvar saatinin monotonik saate göre öne fırlamasına izin verilen tolerans.
        // DST/NTP düzeltmeleri (<=1 saat) yasal sayılır; bunun üzeri saat ileri alma
        // (anti-cheat) olarak değerlendirilir.
        const val CLOCK_FORWARD_JUMP_TOLERANCE_MS = 60 * 60 * 1000L

        fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        /**
         * Kısıtlama ilk kurulurken UsageStats'tan okunan başlangıç çizgisini normalize eder.
         *
         * Başlangıç çizgisi "bu uygulama bugün kısıtlama konmadan ÖNCE ne kadar kullanıldı"
         * demektir; yeni limitin eski kullanımla tüketilmemesi için tutulur.
         *
         * UsageStats günlük kovalarını gecikmeli yazar: ölçüm anında ön planda olan (veya
         * yeni kapanmış) bir uygulama için 0 dönebilir, birkaç dakika sonra aynı gün için
         * gerçek süreyi raporlar. 0'ı "hiç kullanılmamış" kabul etmek, o günün tüm eski
         * kullanımını yeni limite yazar ve kullanıcı daha başlamadan kilitlenir.
         *
         * Bu yüzden 0 da [UNKNOWN_USAGE_STATS_BASELINE] sayılır; gerçek değer ilk
         * uzlaştırmada olay tabanlı ölçümle (queryUsageEventsForegroundMillis) kurulur.
         * Gerçekten hiç kullanılmamış bir uygulamada bu yol da 0'a yakın bir çizgi
         * ürettiği için sonuç aynı kalır.
         */
        fun normalizeInitialUsageStatsBaseline(rawBaselineMillis: Long?): Long {
            if (rawBaselineMillis == null || rawBaselineMillis <= 0L) {
                return UNKNOWN_USAGE_STATS_BASELINE
            }
            return rawBaselineMillis
        }

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
        customTimestamp: Long? = null,
        packageName: String = ""
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
                packageName = packageName,
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

    suspend fun clearFriends() {
        guardianDao.clearFriends()
    }

    suspend fun getActiveRestrictedAppsSync(): List<RestrictedAppEntity> {
        resetDailyCountersIfNeeded()
        return guardianDao.getActiveRestrictedAppsSync()
    }

    suspend fun getActiveRestrictedAppsForTodaySync(): List<RestrictedAppEntity> {
        val now = timeProvider.currentTimeMillis()
        return getActiveRestrictedAppsSync().filter { it.isScheduledAt(now) }
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
        activeDays: String = ALL_DAYS,
        restrictionGroupId: String = packageName,
        restrictionName: String = appName,
        activeWindowEnabled: Boolean = false,
        activeStartMinutes: Int = 0,
        activeEndMinutes: Int = 0
    ): Long {
        val today = timeProvider.localDateString()
        val now = timeProvider.currentTimeMillis()
        val safeDailyLimitMinutes = dailyLimitMinutes.coerceIn(1, MAX_DAILY_LIMIT_MINUTES)
        val usageStatsBaseline = if (activeWindowEnabled) {
            queryScheduledUsageMillisToday(
                packageName = packageName,
                activeDays = activeDays,
                activeStartMinutes = activeStartMinutes,
                activeEndMinutes = activeEndMinutes,
                nowMillis = now
            )
        } else {
            queryTodayUsageStatsMillis(packageName)
        }.let { normalizeInitialUsageStatsBaseline(it) }
        val usageStatsObserved = usageStatsBaseline.coerceAtLeast(0L)
        val usageStatsReconciledAt = now
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
                    restrictionGroupId = restrictionGroupId.ifBlank { packageName },
                    restrictionName = restrictionName.ifBlank { appName },
                    activeWindowEnabled = activeWindowEnabled,
                    activeStartMinutes = activeStartMinutes.coerceIn(0, 1439),
                    activeEndMinutes = activeEndMinutes.coerceIn(0, 1439),
                    activeDays = activeDays,
                    lastResetDate = today,
                    nextDayLimitMinutes = 0,
                    nextDayActiveDays = "",
                    lastLimitUpdateDate = today,
                    todayMinLimitMinutes = safeDailyLimitMinutes,
                    usageStatsBaselineMillisToday = usageStatsBaseline,
                    lastUsageStatsObservedMillisToday = usageStatsObserved,
                    lastUsageStatsReconciledAtMillis = usageStatsReconciledAt
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
                    createdAtMillis = now,
                    isActive = true,
                    isFailed = false,
                    restrictionGroupId = restrictionGroupId.ifBlank { packageName },
                    restrictionName = restrictionName.ifBlank { appName },
                    activeWindowEnabled = activeWindowEnabled,
                    activeStartMinutes = activeStartMinutes.coerceIn(0, 1439),
                    activeEndMinutes = activeEndMinutes.coerceIn(0, 1439),
                    activeDays = activeDays,
                    lastResetDate = today,
                    lastLimitUpdateDate = today,
                    todayMinLimitMinutes = safeDailyLimitMinutes,
                    usageStatsBaselineMillisToday = usageStatsBaseline,
                    lastUsageStatsObservedMillisToday = usageStatsObserved,
                    lastUsageStatsReconciledAtMillis = usageStatsReconciledAt
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
        val now = timeProvider.currentTimeMillis()
        val existing = guardianDao.getRestrictedAppByPackageSync(packageName)
        if (!com.gardiyan.app.BuildConfig.DEBUG) {
            if (existing != null) {
                return existing.id
            }
        }
        val safeTestSeconds = testSeconds.coerceIn(1, MAX_DAILY_LIMIT_MINUTES * 60)
        val dailyLimitMinutes = (safeTestSeconds + 59) / 60
        val usageStatsBaseline = normalizeInitialUsageStatsBaseline(queryTodayUsageStatsMillis(packageName))
        val usageStatsObserved = usageStatsBaseline.coerceAtLeast(0L)
        val usageStatsReconciledAt = now
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
                    nextDayActiveDays = "",
                    usageStatsBaselineMillisToday = usageStatsBaseline,
                    lastUsageStatsObservedMillisToday = usageStatsObserved,
                    lastUsageStatsReconciledAtMillis = usageStatsReconciledAt
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
                    createdAtMillis = now,
                    isActive = true,
                    isFailed = false,
                    activeDays = activeDays,
                    lastResetDate = today,
                    usageStatsBaselineMillisToday = usageStatsBaseline,
                    lastUsageStatsObservedMillisToday = usageStatsObserved,
                    lastUsageStatsReconciledAtMillis = usageStatsReconciledAt
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
        val app = guardianDao.getRestrictedAppByIdSync(id) ?: return
        val now = timeProvider.currentTimeMillis()
        val usageStatsBaseline = normalizeInitialUsageStatsBaseline(queryTodayUsageStatsMillis(app.packageName))
        val usageStatsObserved = usageStatsBaseline.coerceAtLeast(0L)
        guardianDao.updateRestrictedApp(
            app.copy(
                isActive = true,
                isFailed = false,
                remainingSecondsToday = app.dailyLimitMinutes * 60,
                remainingMinutesToday = app.dailyLimitMinutes,
                lastResetDate = timeProvider.localDateString(),
                lastLimitUpdateDate = "",
                todayMinLimitMinutes = 0,
                usageStatsBaselineMillisToday = usageStatsBaseline,
                lastUsageStatsObservedMillisToday = usageStatsObserved,
                lastUsageStatsReconciledAtMillis = now
            )
        )
    }

    suspend fun updateRestrictedApp(app: RestrictedAppEntity) {
        guardianDao.updateRestrictedApp(app)
    }

    suspend fun reconcileRestrictedAppsWithUsageStats(): List<UsageStatsReconciliationResult> {
        resetDailyCountersIfNeeded()
        val now = timeProvider.currentTimeMillis()
        val activeAppsForToday = guardianDao.getActiveRestrictedAppsSync().filter { it.isScheduledAt(now) }

        return activeAppsForToday.mapNotNull { app ->
            val observedMillis = if (app.activeWindowEnabled) {
                queryScheduledUsageMillisToday(
                    packageName = app.packageName,
                    activeDays = app.activeDays,
                    activeStartMinutes = app.activeStartMinutes,
                    activeEndMinutes = app.activeEndMinutes,
                    nowMillis = now
                )
            } else {
                queryTodayUsageStatsMillis(app.packageName)
            } ?: return@mapNotNull null
            val measuredMillisSinceTrackingStart = if (app.usageStatsBaselineMillisToday < 0L) {
                val trackingStart = usageStatsTrackingStartMillis(app)
                queryUsageEventsForegroundMillis(app.packageName, trackingStart, timeProvider.currentTimeMillis())
            } else {
                null
            }
            reconcileRestrictedAppWithObservedUsage(app, observedMillis, measuredMillisSinceTrackingStart)
        }
    }

    suspend fun reconcileRestrictedAppWithObservedUsage(
        appId: Long,
        observedUsageMillisToday: Long,
        measuredUsageMillisSinceTrackingStart: Long? = null
    ): UsageStatsReconciliationResult? {
        val app = guardianDao.getRestrictedAppByIdSync(appId) ?: return null
        return reconcileRestrictedAppWithObservedUsage(
            app = app,
            observedUsageMillisToday = observedUsageMillisToday,
            measuredUsageMillisSinceTrackingStart = measuredUsageMillisSinceTrackingStart
        )
    }

    private suspend fun reconcileRestrictedAppWithObservedUsage(
        app: RestrictedAppEntity,
        observedUsageMillisToday: Long,
        measuredUsageMillisSinceTrackingStart: Long? = null
    ): UsageStatsReconciliationResult? {
        if (!app.isActive || app.isFailed) return null

        val observedMillis = observedUsageMillisToday.coerceAtLeast(0L)
        val now = timeProvider.currentTimeMillis()
        val baselineInitialized = app.usageStatsBaselineMillisToday < 0L

        val usageStatsBaseline = if (baselineInitialized) {
            measuredUsageMillisSinceTrackingStart
                ?.coerceAtLeast(0L)
                ?.let { measuredMillis -> (observedMillis - measuredMillis).coerceIn(0L, observedMillis) }
                ?: observedMillis
        } else {
            app.usageStatsBaselineMillisToday
        }

        val limitSeconds = app.dailyLimitMinutes * 60
        val usageAfterRestrictionMillis = (observedMillis - usageStatsBaseline)
            .coerceAtLeast(0L)
        val usageAfterRestrictionSeconds = elapsedWholeSeconds(usageAfterRestrictionMillis)
        val usageStatsRemaining = (limitSeconds - usageAfterRestrictionSeconds)
            .coerceIn(0, limitSeconds)
        val currentRemaining = app.remainingSecondsToday.coerceIn(0, limitSeconds)
        val missingSeconds = currentRemaining - usageStatsRemaining
        val lastObserved = maxOf(app.lastUsageStatsObservedMillisToday, observedMillis)

        if (missingSeconds <= USAGE_STATS_RECONCILE_TOLERANCE_SECONDS) {
            if (
                baselineInitialized ||
                lastObserved != app.lastUsageStatsObservedMillisToday ||
                app.lastUsageStatsReconciledAtMillis == 0L
            ) {
                guardianDao.updateRestrictedApp(
                    app.copy(
                        usageStatsBaselineMillisToday = usageStatsBaseline,
                        lastUsageStatsObservedMillisToday = lastObserved,
                        lastUsageStatsReconciledAtMillis = now
                    )
                )
            }
            return if (baselineInitialized) {
                UsageStatsReconciliationResult(
                    appId = app.id,
                    packageName = app.packageName,
                    appName = app.appName,
                    adjustedSeconds = 0,
                    remainingSecondsToday = app.remainingSecondsToday,
                    baselineInitialized = true
                )
            } else {
                null
            }
        }

        guardianDao.updateRestrictedApp(
            app.copy(
                remainingSecondsToday = usageStatsRemaining,
                remainingMinutesToday = usageStatsRemaining / 60,
                usageStatsBaselineMillisToday = usageStatsBaseline,
                lastUsageStatsObservedMillisToday = lastObserved,
                lastUsageStatsReconciledAtMillis = now
            )
        )

        return UsageStatsReconciliationResult(
            appId = app.id,
            packageName = app.packageName,
            appName = app.appName,
            adjustedSeconds = missingSeconds,
            remainingSecondsToday = usageStatsRemaining,
            baselineInitialized = baselineInitialized
        )
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
                    val wallDiff = nowWall - lastResetWallClockMillis
                    // Saati ileri almak wallDiff'i büyütür ama monotonik elapsedDiff'i
                    // büyütmez. Gerçek zaman geçişinde ikisi birlikte ilerler. Tutarlıysa
                    // (öne fırlama toleransın altındaysa) yasal gün geçişidir ve 22 saat
                    // beklenmeden sıfırlanmalıdır. Aksi halde 22 saatlik monotonik süre
                    // dolana kadar yedek olarak engellenir (sabit ama yanlış saat senaryosu).
                    val forwardJumpMillis = wallDiff - elapsedDiff
                    val clockConsistent = forwardJumpMillis <= CLOCK_FORWARD_JUMP_TOLERANCE_MS
                    if (clockConsistent || elapsedDiff >= minIntervalMillis) {
                        isTimePassed = true
                    }
                    if (com.gardiyan.app.BuildConfig.DEBUG) {
                        android.util.Log.d("GuardianRepository", "Zaman geçiş kontrolü (Monotonic): elapsedDiff=${elapsedDiff}ms, wallDiff=${wallDiff}ms, forwardJump=${forwardJumpMillis}ms, clockConsistent=$clockConsistent, isTimePassed=$isTimePassed")
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
                val activeSessions = guardianDao.getActiveSessionsSync()
                val dayStartMillis = todayStartMillis()
                val resetPackages = mutableSetOf<String>()
                var anyReset = false
                restrictedApps.forEach { app ->
                    if (app.lastResetDate != today) {
                        anyReset = true
                        resetPackages += app.packageName
                        val newLimit = if (app.nextDayLimitMinutes > 0) app.nextDayLimitMinutes else app.dailyLimitMinutes
                        val newActiveDays = app.nextDayActiveDays.ifEmpty { app.activeDays }
                        val resetUsage = buildDailyResetUsageSnapshot(
                            app = app,
                            dayStartMillis = dayStartMillis,
                            nowWallMillis = nowWall,
                            nowElapsedRealtime = nowElapsed,
                            activeSessions = activeSessions
                        )
                        val newLimitSeconds = newLimit * 60
                        val newRemainingSeconds = (newLimitSeconds - resetUsage.usedSecondsToday)
                            .coerceIn(0, newLimitSeconds)
                        if (com.gardiyan.app.BuildConfig.DEBUG) {
                            android.util.Log.d("GuardianRepository", "Sıfırlanıyor: ${app.appName} (${app.packageName}), yeni limit: ${newLimit}dk")
                        }
                        guardianDao.updateRestrictedApp(
                            app.copy(
                                dailyLimitMinutes = newLimit,
                                remainingMinutesToday = newRemainingSeconds / 60,
                                remainingSecondsToday = newRemainingSeconds,
                                isFailed = false,
                                activeDays = newActiveDays,
                                lastResetDate = today,
                                nextDayLimitMinutes = 0,
                                nextDayActiveDays = "",
                                lastLimitUpdateDate = "",
                                todayMinLimitMinutes = 0,
                                usageStatsBaselineMillisToday = resetUsage.usageStatsBaselineMillisToday,
                                lastUsageStatsObservedMillisToday = resetUsage.lastUsageStatsObservedMillisToday,
                                lastUsageStatsReconciledAtMillis = resetUsage.lastUsageStatsReconciledAtMillis
                            )
                        )
                    }
                }

                if (resetPackages.isNotEmpty()) {
                    realignActiveSessionsAfterDailyReset(
                        activeSessions = activeSessions,
                        resetPackages = resetPackages,
                        nowWallMillis = nowWall,
                        nowElapsedRealtime = nowElapsed
                    )
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
                    remove("lastResetPreventedLocalDate")
                    apply()
                }
                
                if (anyReset && !isFirstInit) {
                    insertLog("DAILY_RESET", "", "Günlük limitler sıfırlandı. Yeni gün: $today")
                }
            } else if (isDateChanged && !isTimePassed) {
                if (com.gardiyan.app.BuildConfig.DEBUG) {
                    android.util.Log.w("GuardianRepository", "Erken günlük sıfırlama engellendi! Zaman aşımı yetersiz.")
                }
                val lastPreventedDate = prefs.getString("lastResetPreventedLocalDate", "") ?: ""
                if (lastPreventedDate != today) {
                    insertLog("RESET_PREVENTED", "", "Tarih değişimi algılandı ancak güvenlik süresi henüz dolmadı. Limitlerin erken sıfırlanması engellendi.")
                }
                prefs.edit()
                    .putString("lastResetPreventedLocalDate", today)
                    .putLong("lastKnownWallClockMillis", nowWall)
                    .apply()
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

        val earnedLevel = when {
            nextStreak >= 60 -> 6
            nextStreak >= 30 -> 5
            nextStreak >= 15 -> 4
            nextStreak >= 7 -> 3
            nextStreak >= 3 -> 2
            else -> 1
        }
        if (earnedLevel > session.level) {
            nextLevel = earnedLevel
            logDetails += " $nextStreak günlük seriyle Seviye $earnedLevel seviyesine yükseldiniz."
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
            // Bir hedef o gün için aktifti ve ihlal kaydı yok. Koruma servisi o gün
            // başlatılmamış olsa bile kullanıcı limiti aşmadığından gün başarı sayılır.
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
        val now = timeProvider.currentTimeMillis()
        val nowElapsed = timeProvider.elapsedRealtime()
        val session = ActiveUsageSessionEntity(
            appId = app.id,
            packageName = app.packageName,
            appName = app.appName,
            entryAtMillis = now,
            lastSeenAtMillis = now,
            entryElapsedRealtime = nowElapsed,
            lastSeenElapsedRealtime = nowElapsed,
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
            val now = timeProvider.currentTimeMillis()
            val nowElapsed = timeProvider.elapsedRealtime()
            val deltaMs = elapsedSinceLastSeenMillis(session, now, nowElapsed)

            // Saat geriye alınırsa lastSeen'i geriye taşımayarak ücretsiz kullanım oluşmasını engelle.
            if (deltaMs <= 0L) {
                return
            }

            val deltaSec = elapsedWholeSeconds(deltaMs)
            if (deltaSec > 0) {
                deductElapsedSeconds(session.appId, deltaSec, "Aktif oturumdan düşülen süre")

                // Tam saniyeler düşülürken kalan milisaniyeyi sonraki kalp atışına taşı.
                val updated = session.copy(
                    lastSeenAtMillis = now - (deltaMs % 1000L),
                    lastSeenElapsedRealtime = nowElapsed - (deltaMs % 1000L),
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
        val now = timeProvider.currentTimeMillis()
        val nowElapsed = timeProvider.elapsedRealtime()
        val elapsedMs = elapsedSinceLastSeenMillis(session, now, nowElapsed)
        val elapsedSec = elapsedWholeSeconds(elapsedMs)
        deductElapsedSeconds(session.appId, elapsedSec, "Kapanan oturumdan düşülen süre")

        val closed = session.copy(
            isActive = false,
            lastSeenAtMillis = now,
            lastSeenElapsedRealtime = nowElapsed,
            updatedAtMillis = now
        )
        guardianDao.updateActiveSession(closed)
        insertLog("SESSION_CLOSED", session.appName, "Koruma oturumu kapatıldı ($reason).")
    }

    suspend fun cleanupStaleSessions() {
        val sessions = guardianDao.getActiveSessionsSync()
        val now = timeProvider.currentTimeMillis()
        val nowElapsed = timeProvider.elapsedRealtime()
        for (session in sessions) {
            val elapsedMs = elapsedSinceLastSeenMillis(session, now, nowElapsed)
            if (elapsedMs > 30000L) {
                val elapsedSec = elapsedWholeSeconds(elapsedMs)
                deductElapsedSeconds(session.appId, elapsedSec, "Bayat oturumdan düşülen süre")

                val closed = session.copy(
                    isActive = false,
                    lastSeenAtMillis = now,
                    lastSeenElapsedRealtime = nowElapsed,
                    updatedAtMillis = now
                )
                guardianDao.updateActiveSession(closed)
                insertLog("STALE_SESSION_CLEANED", session.appName, "Bayatlayan aktif oturum temizlendi.")
            }
        }
    }

    private fun elapsedSinceLastSeenMillis(
        session: ActiveUsageSessionEntity,
        nowWallMillis: Long,
        nowElapsedRealtime: Long
    ): Long {
        val hasMonotonicClock = session.lastSeenElapsedRealtime > 0L &&
            nowElapsedRealtime >= session.lastSeenElapsedRealtime
        return if (hasMonotonicClock) {
            nowElapsedRealtime - session.lastSeenElapsedRealtime
        } else {
            nowWallMillis - session.lastSeenAtMillis
        }
    }

    private fun buildDailyResetUsageSnapshot(
        app: RestrictedAppEntity,
        dayStartMillis: Long,
        nowWallMillis: Long,
        nowElapsedRealtime: Long,
        activeSessions: List<ActiveUsageSessionEntity>
    ): DailyResetUsageSnapshot {
        val observedUsageMillis = if (app.activeWindowEnabled) {
            queryScheduledUsageMillisToday(
                packageName = app.packageName,
                activeDays = app.activeDays,
                activeStartMinutes = app.activeStartMinutes,
                activeEndMinutes = app.activeEndMinutes,
                nowMillis = nowWallMillis
            )
        } else {
            queryTodayUsageStatsMillis(app.packageName)
        }?.coerceAtLeast(0L)

        if (app.activeWindowEnabled) {
            val scheduledUsage = observedUsageMillis ?: 0L
            return DailyResetUsageSnapshot(
                usedSecondsToday = elapsedWholeSeconds(scheduledUsage),
                usageStatsBaselineMillisToday = if (observedUsageMillis == null) UNKNOWN_USAGE_STATS_BASELINE else 0L,
                lastUsageStatsObservedMillisToday = scheduledUsage,
                lastUsageStatsReconciledAtMillis = nowWallMillis
            )
        }

        val eventUsageMillis = queryUsageEventsForegroundMillis(app.packageName, dayStartMillis, nowWallMillis)
            ?.coerceAtLeast(0L)
        val activeSessionUsageMillis = activeSessions
            .asSequence()
            .filter { it.packageName == app.packageName && it.isActive }
            .map { activeSessionUsageSinceDayStartMillis(it, dayStartMillis, nowWallMillis, nowElapsedRealtime) }
            .maxOrNull()
            ?: 0L
        val measuredUsageMillis = maxOf(eventUsageMillis ?: 0L, activeSessionUsageMillis)
        val hasMeasuredUsage = eventUsageMillis != null || activeSessionUsageMillis > 0L
        val baselineMillis = when {
            observedUsageMillis == null -> UNKNOWN_USAGE_STATS_BASELINE
            hasMeasuredUsage -> (observedUsageMillis - measuredUsageMillis)
                .coerceIn(0L, observedUsageMillis)
            else -> observedUsageMillis
        }

        return DailyResetUsageSnapshot(
            usedSecondsToday = elapsedWholeSeconds(measuredUsageMillis),
            usageStatsBaselineMillisToday = baselineMillis,
            lastUsageStatsObservedMillisToday = observedUsageMillis ?: 0L,
            lastUsageStatsReconciledAtMillis = nowWallMillis
        )
    }

    private fun activeSessionUsageSinceDayStartMillis(
        session: ActiveUsageSessionEntity,
        dayStartMillis: Long,
        nowWallMillis: Long,
        nowElapsedRealtime: Long
    ): Long {
        val sessionAgeMillis = elapsedSinceLastSeenMillis(session, nowWallMillis, nowElapsedRealtime)
        val usageEndMillis = if (sessionAgeMillis in 0L..30_000L) {
            nowWallMillis
        } else {
            session.lastSeenAtMillis
        }
        val usageStartMillis = maxOf(session.entryAtMillis, dayStartMillis)
        return (usageEndMillis - usageStartMillis).coerceAtLeast(0L)
    }

    private suspend fun realignActiveSessionsAfterDailyReset(
        activeSessions: List<ActiveUsageSessionEntity>,
        resetPackages: Set<String>,
        nowWallMillis: Long,
        nowElapsedRealtime: Long
    ) {
        activeSessions
            .filter { it.packageName in resetPackages && it.isActive }
            .forEach { session ->
                guardianDao.updateActiveSession(
                    session.copy(
                        entryAtMillis = nowWallMillis,
                        lastSeenAtMillis = nowWallMillis,
                        entryElapsedRealtime = nowElapsedRealtime,
                        lastSeenElapsedRealtime = nowElapsedRealtime,
                        updatedAtMillis = nowWallMillis
                    )
                )
            }
    }

    private fun queryScheduledUsageMillisToday(
        packageName: String,
        activeDays: String,
        activeStartMinutes: Int,
        activeEndMinutes: Int,
        nowMillis: Long
    ): Long? {
        val today = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = today.timeInMillis
        val currentDay = RestrictionSchedule.dayLabel(today)
        val previousDay = RestrictionSchedule.previousDayLabel(currentDay)
        val selectedDays = activeDays.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        fun isSelected(label: String) = selectedDays.isEmpty() || label in selectedDays

        val startMinute = activeStartMinutes.coerceIn(0, 1439)
        val endMinute = activeEndMinutes.coerceIn(0, 1439)
        val intervals = buildList<Pair<Long, Long>> {
            when {
                startMinute == endMinute -> {
                    if (isSelected(currentDay)) add(dayStart to nowMillis)
                }
                startMinute < endMinute -> {
                    if (isSelected(currentDay)) {
                        val start = dayStart + startMinute * 60_000L
                        val end = minOf(dayStart + endMinute * 60_000L, nowMillis)
                        if (end > start) add(start to end)
                    }
                }
                else -> {
                    if (isSelected(previousDay)) {
                        val earlyEnd = minOf(dayStart + endMinute * 60_000L, nowMillis)
                        if (earlyEnd > dayStart) add(dayStart to earlyEnd)
                    }
                    if (isSelected(currentDay)) {
                        val lateStart = dayStart + startMinute * 60_000L
                        if (nowMillis > lateStart) add(lateStart to nowMillis)
                    }
                }
            }
        }

        var total = 0L
        intervals.forEach { (start, end) ->
            val measured = queryUsageEventsForegroundMillis(packageName, start, end) ?: return null
            total += measured
        }
        return total
    }

    private fun queryTodayUsageStatsMillis(packageName: String): Long? {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null
            val now = timeProvider.currentTimeMillis()
            val start = todayStartMillis()
            if (now <= start) return 0L

            usageStatsManager.queryAndAggregateUsageStats(start, now)
                .values
                .firstOrNull { it.packageName == packageName }
                ?.totalTimeInForeground
                ?.coerceAtLeast(0L)
                ?: 0L
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun usageStatsTrackingStartMillis(app: RestrictedAppEntity): Long {
        val unknownBaselineStart = if (
            app.usageStatsBaselineMillisToday < 0L &&
            app.lastUsageStatsReconciledAtMillis > 0L
        ) {
            app.lastUsageStatsReconciledAtMillis
        } else {
            app.createdAtMillis
        }
        return maxOf(todayStartMillis(), unknownBaselineStart.coerceAtLeast(0L))
    }

    private fun queryUsageEventsForegroundMillis(
        packageName: String,
        startMillis: Long,
        endMillis: Long
    ): Long? {
        if (endMillis <= startMillis) return 0L

        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null
            val queryStart = (startMillis - USAGE_EVENTS_STATE_LOOKBACK_MS).coerceAtLeast(0L)
            val usageEvents = usageStatsManager.queryEvents(queryStart, endMillis)
            val event = UsageEvents.Event()
            var isPackageForeground = false
            var lastTimestamp = startMillis
            var totalMillis = 0L

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val rawTimestamp = event.timeStamp

                if (rawTimestamp >= startMillis) {
                    val timestamp = rawTimestamp.coerceAtMost(endMillis)
                    if (isPackageForeground && timestamp > lastTimestamp) {
                        totalMillis += timestamp - lastTimestamp
                    }
                    lastTimestamp = timestamp
                }

                if (event.packageName == packageName) {
                    when {
                        isUsageForegroundEvent(event.eventType) -> isPackageForeground = true
                        isUsageBackgroundEvent(event.eventType) -> isPackageForeground = false
                    }
                } else if (isUsageForegroundEvent(event.eventType)) {
                    isPackageForeground = false
                }
            }

            if (isPackageForeground && endMillis > lastTimestamp) {
                totalMillis += endMillis - lastTimestamp
            }

            totalMillis.coerceAtLeast(0L)
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun isUsageForegroundEvent(eventType: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            eventType == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
        }
    }

    private fun isUsageBackgroundEvent(eventType: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                eventType == UsageEvents.Event.ACTIVITY_STOPPED
        } else {
            @Suppress("DEPRECATION")
            eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
        }
    }

    private fun todayStartMillis(): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeProvider.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun elapsedWholeSeconds(elapsedMs: Long): Int {
        return (elapsedMs.coerceAtLeast(0L) / 1000L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private suspend fun deductElapsedSeconds(appId: Long, elapsedSec: Int, logPrefix: String) {
        if (elapsedSec <= 0) return

        val app = guardianDao.getRestrictedAppByIdSync(appId) ?: return
        if (!app.isActive || app.isFailed) return

        val chargedSeconds = elapsedSec.coerceAtMost(app.remainingSecondsToday)
        val newRemaining = app.remainingSecondsToday - chargedSeconds
        guardianDao.updateRestrictedApp(
            app.copy(
                remainingSecondsToday = newRemaining,
                remainingMinutesToday = newRemaining / 60
            )
        )
        insertLog("USAGE_PROCESSED", app.appName, "$logPrefix: $chargedSeconds saniye.")
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

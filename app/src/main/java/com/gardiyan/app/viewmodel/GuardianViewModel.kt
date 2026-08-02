package com.gardiyan.app.viewmodel

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gardiyan.app.data.local.database.GuardianDatabase
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.data.local.entity.StatusLogEntity
import com.gardiyan.app.data.local.entity.UserSessionEntity
import com.gardiyan.app.data.model.AppUsageSummary
import com.gardiyan.app.data.model.UsagePeriod
import com.gardiyan.app.data.repository.GuardianRepository
import com.gardiyan.app.service.AccessibilityHealthMonitor
import com.gardiyan.app.service.AppBlockAccessibilityService
import com.gardiyan.app.service.BlockOverlayService
import com.gardiyan.app.service.KeepAliveScheduler
import com.gardiyan.app.service.DailySuccessScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

/**
 * enabled_accessibility_services secure ayar dizesinde bizim erişilebilirlik
 * servisimizin kayıtlı olup olmadığını kontrol eder. Saf (Android bağımsız) olduğu
 * için birim testi yazılabilir. Sistem tam biçimi (pkg/pkg.Class) saklar; kısa biçim
 * (pkg/.Class) ve büyük/küçük harf farkları da tolere edilir.
 */
internal fun isAccessibilityComponentEnabled(
    enabledServicesSetting: String?,
    packageName: String,
    className: String
): Boolean {
    return AccessibilityHealthMonitor.isComponentEnabled(enabledServicesSetting, packageName, className)
}

internal fun shouldPenalizeRestrictionRemoval(app: RestrictedAppEntity): Boolean =
    app.remainingSecondsToday <= 0

internal data class RestrictionAssignment(
    val appName: String,
    val packageName: String,
    val groupId: String,
    val displayName: String
)

internal fun buildRestrictionAssignments(
    restrictionName: String,
    apps: List<Pair<String, String>>,
    namedGroupId: String
): List<RestrictionAssignment> {
    val safeName = restrictionName.trim()
    return apps.map { (appName, packageName) ->
        RestrictionAssignment(
            appName = appName,
            packageName = packageName,
            groupId = if (safeName.isEmpty()) packageName else namedGroupId,
            displayName = safeName.ifBlank { appName }
        )
    }
}

internal fun RestrictedAppEntity.withReducedDailyLimit(
    newLimitMinutes: Int,
    nextActiveDays: String
): RestrictedAppEntity {
    val oldLimitSecs = dailyLimitMinutes * 60
    val usedSecs = (oldLimitSecs - remainingSecondsToday).coerceAtLeast(0)
    val newLimitSecs = newLimitMinutes * 60
    val newRemainingSecs = (newLimitSecs - usedSecs).coerceAtLeast(0)

    return copy(
        dailyLimitMinutes = newLimitMinutes,
        nextDayLimitMinutes = newLimitMinutes,
        remainingMinutesToday = (newRemainingSecs + 59) / 60,
        remainingSecondsToday = newRemainingSecs,
        nextDayActiveDays = nextActiveDays,
        isFailed = isFailed
    )
}

class GuardianViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val db = GuardianDatabase.getDatabase(appContext)
    private val repository = GuardianRepository(appContext, db.guardianDao())

    val userSession: StateFlow<UserSessionEntity?> = repository.userSession
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allLogs: StateFlow<List<StatusLogEntity>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val restrictedApps: StateFlow<List<RestrictedAppEntity>> = repository.restrictedApps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _setupStep = MutableStateFlow(1)
    val setupStep: StateFlow<Int> = _setupStep.asStateFlow()

    private val _shameMessage = MutableStateFlow("Üzgünüm, bugün irademe yenildim ve Instagram'da sörf yaparken Limitra'a yakalandım. Bu mesaj utancımın kanıtıdır.")
    val shameMessage: StateFlow<String> = _shameMessage.asStateFlow()

    private val _isMonitoringActive = MutableStateFlow(BlockOverlayService.isServiceRunning.get())
    val isMonitoringActive: StateFlow<Boolean> = _isMonitoringActive.asStateFlow()

    init {
        viewModelScope.launch {
            repository.insertDefaultSessionIfMissing()
            repository.evaluateMissedDays()
            repository.resetDailyCountersIfNeeded()
        }
    }

    fun setStep(step: Int) {
        _setupStep.value = step
    }

    fun updateShameMessage(msg: String) {
        _shameMessage.value = msg
    }

    // ============================
    // Multi-app restricted list
    // ============================

    /**
     * Kullanıcının kurduğu yeni bir kısıtlama. Aynı paket varsa limit
     * güncellenir, yoksa yeni satır oluşturulur.
     * isActive = true HER ZAMAN — kolay kapatma yok.
     */
    fun addRestrictedApp(packageName: String, appName: String, dailyLimitMinutes: Int, activeDays: String = GuardianRepository.ALL_DAYS) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.upsertRestrictedApp(packageName, appName, dailyLimitMinutes, activeDays)
            repository.insertLog(
                eventType = "RESTRICTION_ADDED",
                appName = appName,
                details = "$appName kısıtlama listesine eklendi: günde ${dailyLimitMinutes} dakika"
            )
            // Eski `targetAppPackage` alanı için de set et (geri uyumluluk)
            markSessionHavingRestrictions()
            }
            // Servis canlıysa veya değilse restart
            ensureMonitoringRunning()
        }
    }

    /**
     * Hızlı test modu: seçilen uygulamayı belirli bir saniye sonra
     * kilitlenecek şekilde listeye ekler (örn. 10 saniye). Mevcut servisi
     * tetikler.
     */
    fun startQuickTest(context: Context, packageName: String, appName: String, testSeconds: Int = 10, activeDays: String = GuardianRepository.ALL_DAYS) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertQuickTestApp(packageName, appName, testSeconds, activeDays)
            repository.insertLog(
                eventType = "QUICK_TEST_STARTED",
                appName = appName,
                details = "$appName için hızlı test başlatıldı: ${testSeconds} saniye sonra kilit"
            )
            markSessionHavingRestrictions()
            }
            // A11y + overlay servisini başlat (henüz yoksa)
            if (!BlockOverlayService.isServiceRunning.get()) {
                startMonitoringService(appContext)
            }
        }
    }

    fun removeRestrictedApp(id: Long) {
        viewModelScope.launch {
            val app = repository.getRestrictedAppByIdSync(id)
            repository.removeRestrictedApp(id)
            if (app != null) {
                val shouldPenalize = shouldPenalizeRestrictionRemoval(app)

                // Aktif oturum bu uygulama ise kapat
                val activeSession = repository.getActiveSession()
                if (activeSession != null && activeSession.packageName == app.packageName) {
                    repository.closeActiveSession("Kısıtlama silindi")
                }
                
                // Silinen uygulama şu an kilitliyse ekranı kapat
                if (BlockOverlayService.isLockOverlayFor(app.packageName)) {
                    BlockOverlayService.hideLockOverlay()
                }

                if (shouldPenalize) {
                    // Limiti dolmuş kısıtlamayı silmek disiplin başarısızlığıdır.
                    val session = repository.getSessionSync()
                    if (session != null) {
                        repository.saveSession(
                            session.copy(
                                level = 1,
                                hasRedBadge = true,
                                activeRedemptionDaysLeft = 2,
                                redemptionStreakGoal = 2,
                                consecutiveSuccessDays = 0
                            )
                        )
                    }

                    repository.insertLog(
                        eventType = "RESTRICTION_REMOVED",
                        appName = app.appName,
                        details = "${app.appName} restriction was removed after its daily limit was exhausted."
                    )
                    repository.insertLog(
                        eventType = "RESTRICTION_DELETED",
                        appName = app.appName,
                        details = "${app.appName} restriction was removed after its daily limit was exhausted."
                    )
                }
            }

            // Başka aktif kısıtlama kaldı mı kontrol et, kalmadıysa servisi durdur
            val active = repository.getActiveRestrictedAppsSync()
            if (active.isEmpty()) {
                if (BlockOverlayService.isServiceRunning.get()) {
                    val serviceIntent = Intent(appContext, BlockOverlayService::class.java)
                    appContext.stopService(serviceIntent)
                    _isMonitoringActive.value = false
                }
                if (BlockOverlayService.isLockOverlayVisible.get()) {
                    BlockOverlayService.hideLockOverlay()
                }
                val session = repository.getSessionSync()
                if (session != null && session.isActive) {
                    repository.saveSession(session.copy(isActive = false))
                }
            }
        }
    }

    fun updateRestrictionSettings(id: Long, newLimitMinutes: Int, newActiveDays: String) {
        viewModelScope.launch {
            val app = repository.getRestrictedAppByIdSync(id) ?: return@launch
            
            // Limit artışını engelle (ViewModel koruması)
            if (newLimitMinutes > app.dailyLimitMinutes) {
                return@launch
            }

            val isLimitChanged = app.dailyLimitMinutes != newLimitMinutes
            val effectiveScheduledDays = app.nextDayActiveDays.ifEmpty { app.activeDays }
            val isActiveDaysChanged = effectiveScheduledDays != newActiveDays
            
            if (!isLimitChanged && !isActiveDaysChanged) return@launch

            var updatedApp = app

            if (isLimitChanged) {
                // Sadece limit azaltma veya eşitlik durumu buraya gelebilir
                updatedApp = updatedApp.withReducedDailyLimit(
                    newLimitMinutes = newLimitMinutes,
                    nextActiveDays = if (isActiveDaysChanged) newActiveDays else updatedApp.nextDayActiveDays
                )
                repository.insertLog(
                    eventType = "LIMIT_CHANGED",
                    appName = app.appName,
                    details = "${app.appName} günlük limiti düşürüldü: $newLimitMinutes dk."
                )
            } else if (isActiveDaysChanged) {
                updatedApp = updatedApp.copy(nextDayActiveDays = newActiveDays)
                repository.insertLog(
                    eventType = "DAYS_CHANGED",
                    appName = app.appName,
                    details = "${app.appName} aktif günleri yarından itibaren değiştirilecek: $newActiveDays"
                )
            }

            repository.updateRestrictedApp(updatedApp)
            ensureMonitoringRunning()
        }
    }

    fun logCriticalAction(eventType: String, appName: String, details: String) {
        viewModelScope.launch {
            repository.insertLog(eventType, appName, details)
        }
    }

    fun clearAllRestrictedApps() {
        viewModelScope.launch {
            repository.clearAllRestrictedApps()
            repository.insertLog(
                eventType = "RESTRICTIONS_CLEARED",
                appName = "",
                details = "Tüm kısıtlamalar silindi"
            )
        }
    }

    fun resetRestrictedApp(id: Long) {
        viewModelScope.launch {
            val app = repository.getRestrictedAppByIdSync(id) ?: return@launch
            repository.resetRestrictedApp(id)
            repository.insertLog(
                eventType = "RESTRICTION_RESET",
                appName = app.appName,
                details = "${app.appName} için günlük sayaç sıfırlandı"
            )
        }
    }

    /**
     * Birden fazla hedef uygulama varsa session.isActive=true olur.
     * Bu, A11y service'in "hiç aktif kısıtlama yok" durumunu
     * yanlış değerlendirmesini engeller.
     */
    private suspend fun markSessionHavingRestrictions() {
        val active = repository.getActiveRestrictedAppsSync()
        val session = repository.getSessionSync() ?: UserSessionEntity()
        if (active.isNotEmpty() && !session.isActive) {
            repository.saveSession(session.copy(isActive = true))
        }
    }

    private fun ensureMonitoringRunning() {
        viewModelScope.launch {
            if (!BlockOverlayService.isServiceRunning.get()) {
                startMonitoringService(appContext)
            }
        }
    }

    fun restartServiceIfNeeded(context: Context) {
        viewModelScope.launch {
            val active = repository.getActiveRestrictedAppsSync()
            if (active.isNotEmpty() && !BlockOverlayService.isServiceRunning.get()) {
                startMonitoringService(context)
            }
        }
    }

    /**
     * Servisi başlat. Kolay kapatma yok — dışarıdan false geçilemez.
     * Servis durdurmak SADECE cancelAllWithFiveSecondHold() ile mümkündür.
     */
    private fun startMonitoringService(context: Context) {
        val serviceIntent = Intent(context, BlockOverlayService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        _isMonitoringActive.value = true
    }

    /**
     * Eski API uyumluluğu için korundu ama enable=false durumunda
     * doğrudan servisi durdurmaz — sadece 5sn hold üzerinden çalışır.
     *
     * enable=true: servisi başlatır
     * enable=false: NO-OP (kolay kapatma engellenmiştir)
     */
    fun toggleMonitoringService(context: Context, enable: Boolean) {
        if (enable) {
            startMonitoringService(context)
        }
        // enable = false → NO-OP
        // Servis durdurmak YALNIZCA cancelAllWithFiveSecondHold() ile mümkündür
    }

    /**
     * 5 saniye basılı tutma sonrası çağrılır. Tüm aktif kısıtlamaları
     * kaldırır, level'ı 1'e çeker, kırmızı rozet ekler, overlay'i
     * temizler ve servisi durdurur.
     *
     * TEK YASAL DURDURMA YÖNTEMİ BU FONKSİYONDUR.
     */
    fun cancelAllWithFiveSecondHold() {
        viewModelScope.launch {
            try {
                repository.cancelAllActiveTargets()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Servisi durdur — sadece burada yapılır
            if (BlockOverlayService.isServiceRunning.get()) {
                val serviceIntent = Intent(appContext, BlockOverlayService::class.java)
                appContext.stopService(serviceIntent)
                _isMonitoringActive.value = false
            }
            // Overlay açıksa kapat
            if (BlockOverlayService.isLockOverlayVisible.get()) {
                BlockOverlayService.hideLockOverlay()
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun clearAllUserData(context: Context, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearAllRestrictedApps()
                repository.clearActiveSessions()
                repository.clearLogs()
                val defaultSession = UserSessionEntity(
                    id = 1,
                    username = "LimitraUser",
                    level = 1,
                    hasRedBadge = false,
                    isActive = false,
                    consecutiveSuccessDays = 0,
                    activeRedemptionDaysLeft = 0,
                    redemptionStreakGoal = 2
                )
                repository.saveSession(defaultSession)

                if (BlockOverlayService.isServiceRunning.get()) {
                    val serviceIntent = Intent(context, BlockOverlayService::class.java)
                    context.stopService(serviceIntent)
                    _isMonitoringActive.value = false
                }
                if (BlockOverlayService.isLockOverlayVisible.get()) {
                    BlockOverlayService.hideLockOverlay()
                }

                // SharedPreferences temizliği
                context.getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("gardiyan_eval_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("gardiyan_notifications", Context.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("gardiyan_theme_prefs", Context.MODE_PRIVATE).edit().clear().apply()

                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageStatsSettings(context: Context) {
        launchFirstAvailableSettingsIntent(context, usageAccessSettingsIntents(context.packageName))
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlaySettings(context: Context) {
        launchFirstAvailableSettingsIntent(context, overlaySettingsIntents(context.packageName))
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return getAccessibilityServiceStatus(context).isOperational
    }

    fun getAccessibilityServiceStatus(context: Context): AccessibilityHealthMonitor.Status {
        var status = AccessibilityHealthMonitor.getStatus(context)
        if (
            status.requiresReenable &&
            AppBlockAccessibilityService.requestHealthRecovery("UI status check detected stale heartbeat")
        ) {
            status = AccessibilityHealthMonitor.getStatus(context)
        }
        return status
    }

    fun addRestrictionGroup(
        restrictionName: String,
        apps: List<Pair<String, String>>,
        dailyLimitMinutes: Int,
        activeDays: String = GuardianRepository.ALL_DAYS,
        activeWindowEnabled: Boolean = false,
        activeStartMinutes: Int = 0,
        activeEndMinutes: Int = 0
    ) {
        if (apps.isEmpty()) return
        val safeName = restrictionName.trim()
        val assignments = buildRestrictionAssignments(
            restrictionName = safeName,
            apps = apps,
            namedGroupId = if (safeName.isNotEmpty()) UUID.randomUUID().toString() else ""
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                assignments.forEach { assignment ->
                    repository.upsertRestrictedApp(
                        packageName = assignment.packageName,
                        appName = assignment.appName,
                        dailyLimitMinutes = dailyLimitMinutes,
                        activeDays = activeDays,
                        restrictionGroupId = assignment.groupId,
                        restrictionName = assignment.displayName,
                        activeWindowEnabled = activeWindowEnabled,
                        activeStartMinutes = activeStartMinutes,
                        activeEndMinutes = activeEndMinutes
                    )
                }
                repository.insertLog(
                    eventType = "RESTRICTION_ADDED",
                    appName = safeName.ifBlank { apps.first().first },
                    details = "${safeName.ifBlank { apps.joinToString { it.first } }} kısıtlaması ${apps.size} uygulama için eklendi: günde $dailyLimitMinutes dakika"
                )
                markSessionHavingRestrictions()
            }
            ensureMonitoringRunning()
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val componentName = ComponentName(context, AppBlockAccessibilityService::class.java)
        launchFirstAvailableSettingsIntent(context, accessibilitySettingsIntents(componentName))
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestBatteryOptimizationIgnore(context: Context) {
        launchFirstAvailableSettingsIntent(
            context,
            batteryOptimizationSettingsIntents(context.packageName)
        )
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return nm.areNotificationsEnabled()
    }

    fun openNotificationSettings(context: Context) {
        launchFirstAvailableSettingsIntent(
            context,
            notificationSettingsIntents(context.packageName, context.applicationInfo.uid)
        )
    }

    private fun launchFirstAvailableSettingsIntent(context: Context, intents: List<Intent>) {
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Üretici bu ekranı sağlamıyorsa sıradaki daha genel ve güvenli hedefi dene.
            }
        }
    }

    fun ensureServiceAlive(context: Context) {
        KeepAliveScheduler.schedule(appContext)
        DailySuccessScheduler.schedule(appContext)
        restartServiceIfNeeded(context)
    }

    fun getInstalledApps(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        val list = mutableListOf<Pair<String, String>>()

        val popularPackages = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.google.android.youtube",
            "com.twitter.android",
            "com.facebook.katana",
            "com.reddit.frontpage",
            "com.netflix.mediaclient",
            "com.valvesoftware.android.steam.community"
        )

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        try {
            pm.queryIntentActivities(launcherIntent, 0).forEach { resolveInfo ->
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == context.packageName) return@forEach

                // Gereksiz sistem uygulamalarını filtrele (sadece ana önemli launcher sistem uygulamaları kalacak)
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val isImportantSystem = pkgName == "com.google.android.youtube" || 
                                       pkgName == "com.android.chrome" || 
                                       pkgName == "com.google.android.apps.photos" ||
                                       pkgName == "com.google.android.apps.maps"
                if (isSystem && !isImportantSystem && (
                    pkgName.startsWith("com.android.") || 
                    pkgName.startsWith("com.google.android.inputmethod") || 
                    pkgName.startsWith("com.sec.android.") || 
                    pkgName.contains("overlay") || 
                    pkgName.contains("launcher") || 
                    pkgName.contains("system")
                )) {
                    return@forEach
                }

                val label = resolveInfo.loadLabel(pm).toString()
                list.add(Pair(label, pkgName))
            }
        } catch (e: Exception) {
        }

        val popularList = list.filter { popularPackages.contains(it.second) }.sortedBy { it.first.uppercase() }
        val otherList = list.filter { !popularPackages.contains(it.second) }.sortedBy { it.first.uppercase() }

        return popularList + otherList
    }

    private fun getUsageRankingForInterval(startTime: Long, endTime: Long): List<AppUsageSummary> {
        if (!hasUsageStatsPermission(appContext)) return emptyList()
        val usageStatsManager = appContext.getSystemService(Context.USAGE_STATS_SERVICE)
            as? UsageStatsManager ?: return emptyList()
        val packageManager = appContext.packageManager

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launcherPackages = runCatching {
            packageManager.queryIntentActivities(launcherIntent, 0)
                .mapTo(mutableSetOf()) { it.activityInfo.packageName }
        }.getOrDefault(emptySet())

        return runCatching {
            usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
                .values
                .asSequence()
                .filter { it.packageName != appContext.packageName }
                .filter { it.packageName in launcherPackages }
                .filter { it.totalTimeInForeground > 0L }
                .map { stat ->
                    val appName = runCatching {
                        packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(stat.packageName, 0)
                        ).toString()
                    }.getOrDefault(stat.packageName)
                    AppUsageSummary(
                        packageName = stat.packageName,
                        appName = appName,
                        usageMillis = stat.totalTimeInForeground
                    )
                }
                .sortedByDescending { it.usageMillis }
                .toList()
        }.getOrDefault(emptyList())
    }

    fun getUsageRanking(period: UsagePeriod): List<AppUsageSummary> {
        if (!hasUsageStatsPermission(appContext)) return emptyList()

        if (period == UsagePeriod.AVERAGE) {
            val endTime = System.currentTimeMillis()
            
            // Son 7 günün ve son 30 günün başlangıç saatleri (kayan pencere / rolling window)
            // Gün sınırlarına hizalanmış son 7 ve 30 gün verisi çekilir
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = calendar.timeInMillis
            val sevenDaysAgoStart = todayStart - 7 * 24 * 60 * 60 * 1000L
            val thirtyDaysAgoStart = todayStart - 30L * 24 * 60 * 60 * 1000L

            val weeklyList = getUsageRankingForInterval(sevenDaysAgoStart, endTime)
            val monthlyList = getUsageRankingForInterval(thirtyDaysAgoStart, endTime)

            val weeklyMap = weeklyList.associateBy { it.packageName }
            val monthlyMap = monthlyList.associateBy { it.packageName }

            val allPackages = weeklyMap.keys + monthlyMap.keys
            return allPackages.map { pkg ->
                val wUsage = weeklyMap[pkg]?.usageMillis ?: 0L
                val mUsage = monthlyMap[pkg]?.usageMillis ?: 0L

                val wAvg = wUsage / 7L
                val mAvg = mUsage / 30L

                val avgUsage = when {
                    wAvg > 0L && mAvg > 0L -> (wAvg + mAvg) / 2L
                    wAvg > 0L -> wAvg
                    mAvg > 0L -> mAvg
                    else -> 0L
                }

                val appName = weeklyMap[pkg]?.appName ?: monthlyMap[pkg]?.appName ?: pkg

                AppUsageSummary(
                    packageName = pkg,
                    appName = appName,
                    usageMillis = avgUsage
                )
            }.filter { it.usageMillis > 0L }
             .sortedByDescending { it.usageMillis }
        }

        val endTime = System.currentTimeMillis()
        val startTime = getUsagePeriodStart(period)
        return getUsageRankingForInterval(startTime, endTime)
    }

    private fun getUsagePeriodStart(period: UsagePeriod): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            when (period) {
                UsagePeriod.DAILY -> Unit
                UsagePeriod.WEEKLY -> {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                }
                UsagePeriod.MONTHLY -> set(Calendar.DAY_OF_MONTH, 1)
                UsagePeriod.AVERAGE -> Unit
            }
        }.timeInMillis
    }
}

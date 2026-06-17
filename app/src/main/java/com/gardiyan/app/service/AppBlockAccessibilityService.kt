package com.gardiyan.app.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.app.KeyguardManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.gardiyan.app.MainActivity
import com.gardiyan.app.R
import com.gardiyan.app.data.local.database.GuardianDatabase
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.data.repository.GuardianRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.Locale

/**
 * Çoklu uygulama + event-driven ön plan tespiti ve zamanlayıcı.
 *
 * Mimari:
 * - PRIMARY: TYPE_WINDOW_STATE_CHANGED event'lerini dinler
 * - SECONDARY: UsageStatsManager.queryEvents() ile adaptif polling yedek katmanı
 * - Kısıtlanmış uygulamalar listesindeki (RestrictedAppEntity) bir hedefe
 *   girildiğinde entryTimeMillis kaydedilir
 * - Çıkışta elapsed = now - entryTimeMillis hesaplanır
 * - Veritabanındaki remainingSecondsToday değerinden delta düşülür
 * - Eğer remainingSecondsToday <= 0 ise, hedef uygulamaya her girişte
 *   overlay anında çizilir (10sn sonsuz döngüde)
 * - Aynı anda yalnız bir uygulamada kalınabilir; en son girilen uygulama
 *   state'i taşır
 */
class AppBlockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppBlockA11yService"

        private const val NORMAL_POLL_INTERVAL_MS = 2500L
        private const val SUSPICIOUS_POLL_INTERVAL_MS = 250L
        private const val HEALTH_GRACE_MS = 15_000L

        @Volatile
        var instance: AppBlockAccessibilityService? = null

        @Volatile
        var isRunning: Boolean = false

        @Volatile
        private var lastHeartbeatElapsedRealtime: Long = 0L

        fun isHealthy(): Boolean {
            val heartbeatAge = SystemClock.elapsedRealtime() - lastHeartbeatElapsedRealtime
            return lastHeartbeatElapsedRealtime > 0L && heartbeatAge in 0L..HEALTH_GRACE_MS
        }

        // Şu anda izlenen hedef uygulamaya GİRİŞ zamanı (epoch ms).
        // Kullanıcı kısıtlı uygulamayı açtığı an kaydedilir, çıktığı an sıfırlanır.
        @Volatile
        var entryTimeMillis: Long = 0L

        // Şu anda izlenen hedef uygulamanın paket adı.
        // null ise kullanıcı kısıtlı bir uygulamada değil.
        @Volatile
        var currentTrackedPackage: String? = null

        // Hangi restricted app'in DB satırı izleniyor (id).
        @Volatile
        var currentTrackedAppId: Long = -1L

        @Volatile
        var currentForegroundPackage: String? = null

        @Volatile
        var lastAccessibilityForegroundAt: Long = 0L

        @Volatile
        var ignoreOwnPackageEventsUntil: Long = 0L

        @Volatile
        var cachedRestrictedApps: List<RestrictedAppEntity> = emptyList()

        @Volatile
        var fastPollTicksLeft: Int = 0
    }

    private val a11yJob = SupervisorJob()
    private val a11yScope = CoroutineScope(Dispatchers.IO + a11yJob)
    private val foregroundMutex = Mutex()

    // Kısıtlı uygulamada kalındığı sürece overlay'i tetikleyecek bekleyen coroutine.
    // Her girişte iptal edilip yeniden başlatılır.
    private var tickJob: Job? = null

    // UsageStats polling coroutine
    private var usageStatsPollingJob: Job? = null
    private var heartbeatJob: Job? = null

    // RAM cache helper metotları
    private fun getCachedActiveRestrictedApps(): List<RestrictedAppEntity> {
        return cachedRestrictedApps.filter { it.isActive }
    }

    private fun getCachedActiveRestrictedAppsForToday(): List<RestrictedAppEntity> {
        val today = GuardianRepository.todayDayLabel()
        return getCachedActiveRestrictedApps().filter { app ->
            val days = app.activeDays.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            days.isEmpty() || days.contains(today)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        lastHeartbeatElapsedRealtime = SystemClock.elapsedRealtime()
        AccessibilityHealthMonitor.recordServiceStarted(applicationContext)
        startHeartbeat()
        Log.i(TAG, "Accessibility service connected")

        val db = GuardianDatabase.getDatabase(applicationContext)
        val repository = GuardianRepository(applicationContext, db.guardianDao())

        // Kısıtlı uygulamalar listesini asenkron Flow ile dinle ve RAM'de önbelleğe al
        a11yScope.launch {
            try {
                repository.restrictedApps.collect { apps ->
                    cachedRestrictedApps = apps
                    if (com.gardiyan.app.BuildConfig.DEBUG) {
                        Log.d(TAG, "Restricted apps cache updated, count: ${apps.size}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting restricted apps: ${e.message}")
            }
        }

        a11yScope.launch {
            try {
                // Öncelikle bayat oturumları temizle
                repository.cleanupStaleSessions()

                // Hala geçerli bir açık oturum varsa RAM durumlarına geri yükle
                val openSession = repository.getActiveSession()
                if (openSession != null) {
                    entryTimeMillis = openSession.entryAtMillis
                    currentTrackedPackage = openSession.packageName
                    currentTrackedAppId = openSession.appId
                    Log.i(TAG, "Restored active session from DB: ${openSession.packageName}")

                    queryForegroundEvent()?.let { event ->
                        handleForegroundChange(event.packageName)
                    }
                }

                val activeApps = repository.getActiveRestrictedAppsSync()
                val isRestart = activeApps.isNotEmpty()
                repository.insertLog(
                    eventType = if (isRestart) "SERVICE_RESTARTED" else "SERVICE_STARTED",
                    appName = "",
                    details = if (isRestart) "Limitra koruma motoru yeniden başlatıldı." else "Limitra koruma motoru başarıyla başlatıldı."
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // UsageStats polling yedek katmanını başlat
        startUsageStatsPolling()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isRunning = false
        instance = null
        tickJob?.cancel()
        tickJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        usageStatsPollingJob?.cancel()
        usageStatsPollingJob = null
        AccessibilityHealthMonitor.recordServiceStopped(applicationContext)
        Log.w(TAG, "Accessibility service unbound")

        val db = GuardianDatabase.getDatabase(applicationContext)
        val repository = GuardianRepository(applicationContext, db.guardianDao())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.closeActiveSession("Servis sonlandırıldı (Unbind)")
                repository.insertLog(
                    eventType = "SERVICE_STOPPED",
                    appName = "",
                    details = "Limitra koruma motoru durduruldu (Unbind)."
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        isRunning = false
        instance = null
        lastHeartbeatElapsedRealtime = 0L
        tickJob?.cancel()
        tickJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        usageStatsPollingJob?.cancel()
        usageStatsPollingJob = null
        AccessibilityHealthMonitor.recordServiceStopped(applicationContext)

        val db = GuardianDatabase.getDatabase(applicationContext)
        val repository = GuardianRepository(applicationContext, db.guardianDao())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.closeActiveSession("Servis sonlandırıldı (Destroy)")
                repository.insertLog(
                    eventType = "SERVICE_STOPPED",
                    appName = "",
                    details = "Limitra koruma motoru durduruldu (Destroy)."
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        a11yJob.cancel()
        super.onDestroy()
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            recordHealthyTrackingTick()
            if (event == null) return
            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val foregroundPackage = event.packageName?.toString() ?: return

            if (com.gardiyan.app.BuildConfig.DEBUG) {
                Log.d(TAG, "onAccessibilityEvent: foregroundPackage=$foregroundPackage, className=${event.className}")
            }

            if (
                foregroundPackage == packageName &&
                event.className?.toString() == MainActivity::class.java.name
            ) {
                handleLimitraForeground()
                return
            }

            // Kilit ekranı aktifken kendi paketimizden gelen pencere odak olaylarını tamamen yoksay
            if (foregroundPackage == packageName && BlockOverlayService.isLockOverlayVisible.get()) {
                Log.d(TAG, "Ignoring own package event because overlay is visible")
                return
            }

            val now = System.currentTimeMillis()
            if (foregroundPackage == packageName && now < ignoreOwnPackageEventsUntil) {
                Log.d(TAG, "Ignoring transient self foreground event from lock overlay")
                return
            }
            lastAccessibilityForegroundAt = now
            // A11y pencere değişim olayı geldiğinde geçici olarak hızlı polling'i tetikle
            fastPollTicksLeft = 12 // 3 saniye boyunca hızlı polling yap (12 * 250ms = 3000ms)

            handleForegroundChange(foregroundPackage)
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled accessibility event error", e)
        }
    }

    // ========================================================================
    // UsageStatsManager Polling — Yedek Katman (Adaptif)
    // ========================================================================

    /**
     * UsageStatsManager.queryEvents() ile son foreground uygulamayı sorgular.
     * Normal durumlarda 2500ms aralıkla çalışarak batarya tasarrufu sağlar.
     * Şüpheli durumlarda geçici olarak 250ms aralıkla çalışır.
     */
    private fun startUsageStatsPolling() {
        usageStatsPollingJob?.cancel()
        usageStatsPollingJob = a11yScope.launch {
            Log.i(TAG, "UsageStats polling started with adaptive interval")

            val db = GuardianDatabase.getDatabase(applicationContext)
            val repository = GuardianRepository(applicationContext, db.guardianDao())

            var startupTicksLeft = 60 // İlk 15 saniye boyu hızlı denetim (60 * 250ms = 15s)
            var lastEventTimeForRapidCheck = 0L
            var currentPollIntervalMs = NORMAL_POLL_INTERVAL_MS
            var lastDailyResetCheckTime = 0L
            var unclearForegroundTicks = 0

            while (isActive) {
                try {
                    recordHealthyTrackingTick()
                    val today = todayKey()
                    val evalPrefs = getSharedPreferences("gardiyan_eval_prefs", Context.MODE_PRIVATE)
                    val hasLoggedActiveToday = evalPrefs.getBoolean("logged_active_$today", false)
                    
                    // Veritabanı sorgusu yerine RAM önbelleğini kullan
                    val activeAppsForLog = getCachedActiveRestrictedApps()
                    
                    if (!hasLoggedActiveToday && activeAppsForLog.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            repository.insertLog("ENGINE_ACTIVE", "", "Limitra koruma motoru aktif olarak çalışıyor.")
                        }
                        evalPrefs.edit().putBoolean("logged_active_$today", true).apply()
                    }

                    // Günlük sıfırlama denetimini her 250ms yerine 60 saniyede bire çek
                    val now = System.currentTimeMillis()
                    if (now - lastDailyResetCheckTime > 60000L) {
                        lastDailyResetCheckTime = now
                        withContext(Dispatchers.IO) {
                            repository.resetDailyCountersIfNeeded()
                        }
                    }

                    currentTrackedPackage
                        ?.takeIf { it == currentForegroundPackage }
                        ?.let { pkg ->
                            val app = cachedRestrictedApps.firstOrNull { it.packageName == pkg }
                            if (app != null) {
                                withContext(Dispatchers.IO) {
                                    repository.updateSessionLastSeen(pkg)
                                }
                                if (app.isActive && !app.isFailed) {
                                    checkAndTriggerNotifications(app)
                                }
                            }
                        }

                    // Ekran ve Kilit durumu tespiti
                    val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                        pm?.isInteractive ?: true
                    } else {
                        @Suppress("DEPRECATION")
                        pm?.isScreenOn ?: true
                    }

                    val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    val isLocked = km?.isKeyguardLocked ?: false

                    val trackedApp = currentTrackedPackage?.let { pkg ->
                        cachedRestrictedApps.firstOrNull { it.packageName == pkg }
                    }
                    val overlayShouldBeVisible = trackedApp != null && (trackedApp.remainingSecondsToday <= 0 || trackedApp.isFailed)
                    val overlayIsVisible = BlockOverlayService.isLockOverlayVisible.get()
                    val isForegroundUnclear = currentForegroundPackage.isNullOrEmpty()

                    if (!isForegroundUnclear) {
                        unclearForegroundTicks = 0
                    }

                    // Hızlı uygulama geçişi tespiti
                    var isRapidSwitching = false
                    if (lastAccessibilityForegroundAt > 0 && lastAccessibilityForegroundAt != lastEventTimeForRapidCheck) {
                        val diff = lastAccessibilityForegroundAt - lastEventTimeForRapidCheck
                        if (diff in 1..1500L) {
                            isRapidSwitching = true
                        }
                        lastEventTimeForRapidCheck = lastAccessibilityForegroundAt
                    }

                    var suspiciousReason: String? = null

                    if (!isScreenOn || isLocked) {
                        // Ekran kapalıysa veya cihaz kilitliyse hızlı polling'i tamamen iptal et
                        fastPollTicksLeft = 0
                        unclearForegroundTicks = 0
                    } else {
                        if (startupTicksLeft > 0) {
                            suspiciousReason = "Servis başlangıcı"
                            startupTicksLeft--
                        } else if (fastPollTicksLeft > 0) {
                            suspiciousReason = "Geçici hızlı denetim aktif ($fastPollTicksLeft)"
                            fastPollTicksLeft--
                        } else if (overlayShouldBeVisible && !overlayIsVisible) {
                            suspiciousReason = "Kilit ekranı görünmüyor (beklenen: $currentTrackedPackage)"
                        } else if (isForegroundUnclear) {
                            if (unclearForegroundTicks < 8) { // En fazla 2 saniye (8 * 250ms) hızlı denetle
                                suspiciousReason = "Ön plan paket bilgisi tanımsız (geçici)"
                                unclearForegroundTicks++
                            }
                        } else if (isRapidSwitching) {
                            suspiciousReason = "Hızlı uygulama geçişi algılandı"
                        }
                    }

                    val interval = if (suspiciousReason != null) {
                        if (currentPollIntervalMs != SUSPICIOUS_POLL_INTERVAL_MS) {
                            currentPollIntervalMs = SUSPICIOUS_POLL_INTERVAL_MS
                            Log.w(TAG, "Suspicious state detected: $suspiciousReason. Switching to fast polling.")
                            withContext(Dispatchers.IO) {
                                repository.insertLog(
                                    eventType = "SUSPICIOUS_STATE_DETECTED",
                                    appName = "",
                                    details = "Şüpheli durum algılandı ($suspiciousReason). Hızlı denetim aktif."
                                )
                            }
                        }
                        SUSPICIOUS_POLL_INTERVAL_MS
                    } else {
                        if (currentPollIntervalMs != NORMAL_POLL_INTERVAL_MS) {
                            currentPollIntervalMs = NORMAL_POLL_INTERVAL_MS
                            Log.i(TAG, "Resynchronized: returning to normal polling.")
                            withContext(Dispatchers.IO) {
                                repository.insertLog(
                                    eventType = "ENGINE_RESYNCED",
                                    appName = "",
                                    details = "Koruma motoru durumu senkronize edildi. Normal denetim moduna dönüldü."
                                )
                            }
                        }
                        NORMAL_POLL_INTERVAL_MS
                    }

                    // Yedek ön plan doğrulaması
                    val foregroundEvent = queryForegroundEvent()
                    if (foregroundEvent != null) {
                        val foregroundPkg = foregroundEvent.packageName

                        if (
                            foregroundPkg == packageName &&
                            BlockOverlayService.isLockOverlayVisible.get()
                        ) {
                            handleLimitraForeground()
                        } else if (foregroundPkg != currentForegroundPackage && foregroundPkg != packageName) {
                            Log.w(TAG, "UsageStats fallback detected different package: $foregroundPkg (A11y had: $currentForegroundPackage)")
                            withContext(Dispatchers.IO) {
                                repository.insertLog(
                                    eventType = "USAGE_STATS_FALLBACK",
                                    appName = "",
                                    details = "UsageStats yedek doğrulaması: Ön plan = $foregroundPkg (A11y = $currentForegroundPackage)"
                                )
                            }
                            handleForegroundChange(foregroundPkg)
                        }
                    }

                    delay(interval)
                } catch (e: Exception) {
                    Log.e(TAG, "Adaptive polling loop error: ${e.message}")
                    delay(NORMAL_POLL_INTERVAL_MS)
                }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = a11yScope.launch {
            while (isActive) {
                lastHeartbeatElapsedRealtime = SystemClock.elapsedRealtime()
                AccessibilityHealthMonitor.recordServiceHeartbeat(applicationContext)
                delay(AccessibilityHealthMonitor.HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun recordHealthyTrackingTick() {
        lastHeartbeatElapsedRealtime = SystemClock.elapsedRealtime()
        AccessibilityHealthMonitor.recordServiceHeartbeat(applicationContext)
        AccessibilityHealthMonitor.recordTrackingHeartbeat(applicationContext)
    }

    /**
     * UsageStatsManager.queryEvents() ile son 5 saniye içindeki en son
     * ACTIVITY_RESUMED event'ini bularak aktif foreground uygulamasını döndürür.
     */
    private data class ForegroundEvent(
        val packageName: String,
        val timestampMillis: Long
    )

    private fun queryForegroundEvent(): ForegroundEvent? {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null

            val endTime = System.currentTimeMillis()
            val startTime = endTime - 5000L // Son 5 saniye

            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            var lastForegroundEvent: ForegroundEvent? = null

            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                // MOVE_TO_FOREGROUND was deprecated in API 29 in favor of ACTIVITY_RESUMED
                val isResumed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                } else {
                    @Suppress("DEPRECATION")
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                }
                
                if (isResumed) {
                    lastForegroundEvent = ForegroundEvent(event.packageName, event.timeStamp)
                }
            }

            lastForegroundEvent
        } catch (e: SecurityException) {
            Log.w(TAG, "UsageStats permission not granted: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "queryForegroundEvent error: ${e.message}")
            null
        }
    }

    // ========================================================================
    // Foreground Change Handler
    // ========================================================================

    /**
     * Ön plan uygulama değişikliği işleyicisi. Event-driven timer'ın kalbi.
     * Çoklu uygulama listesine göre çalışır.
     */
    private fun handleForegroundChange(foregroundPackage: String) {
        if (foregroundPackage == packageName && BlockOverlayService.isLockOverlayVisible.get()) {
            Log.d(TAG, "handleForegroundChange: Ignored own package event because overlay is visible")
            return
        }

        a11yScope.launch {
            foregroundMutex.withLock {
                try {
                    currentForegroundPackage = foregroundPackage

                    val db = GuardianDatabase.getDatabase(applicationContext)
                    val repository = GuardianRepository(applicationContext, db.guardianDao())
                    
                    // Veritabanı sorgusu yerine RAM önbelleğini kullan
                    val activeApps = getCachedActiveRestrictedAppsForToday()

                    if (com.gardiyan.app.BuildConfig.DEBUG) {
                        Log.d(TAG, "handleForegroundChange: foregroundPackage=$foregroundPackage, activeAppsCount=${activeApps.size}")
                    }

                    if (activeApps.isEmpty()) {
                        if (BlockOverlayService.isLockOverlayVisible.get()) {
                            BlockOverlayService.hideLockOverlay()
                        }
                        withContext(Dispatchers.IO) {
                            repository.closeActiveSession("Bugün için aktif kısıtlama kalmadı")
                        }
                        clearTrackingState()
                        return@withLock
                    }

                    val matchingApp = activeApps.firstOrNull { it.packageName == foregroundPackage }

                    if (com.gardiyan.app.BuildConfig.DEBUG) {
                        Log.d(TAG, "handleForegroundChange: matchingApp=${matchingApp?.appName ?: "null"} (packageName=$foregroundPackage)")
                    }

                    if (currentTrackedPackage != null && currentTrackedPackage != foregroundPackage) {
                        if (com.gardiyan.app.BuildConfig.DEBUG) {
                            Log.d(TAG, "handleForegroundChange: Exiting previously tracked app: $currentTrackedPackage")
                        }
                        handleExit(repository, activeApps)
                    }

                    if (matchingApp == null) {
                        if (BlockOverlayService.isLockOverlayVisible.get()) {
                            Log.d(TAG, "Hiding lock overlay because foreground package is unrelated: $foregroundPackage")
                            BlockOverlayService.hideLockOverlay()
                        }
                        return@withLock
                    }

                    // Kısıtlı uygulamaya GİRİLDİ
                    if (currentTrackedPackage != matchingApp.packageName) {
                        entryTimeMillis = System.currentTimeMillis()
                        currentTrackedPackage = matchingApp.packageName
                        currentTrackedAppId = matchingApp.id
                        
                        // Kısıtlı uygulamaya geçiş yapıldığı için geçici olarak hızlı polling'i tetikle
                        fastPollTicksLeft = 20 // 5 saniye boyunca hızlı denetle (20 * 250ms = 5000ms)
                        
                        Log.d(TAG, "Target app entered: ${matchingApp.appName} (${matchingApp.packageName}) at $entryTimeMillis")
                        withContext(Dispatchers.IO) {
                            repository.startSession(matchingApp)
                            repository.insertLog(
                                eventType = "A11Y_EVENT_RECEIVED",
                                appName = matchingApp.appName,
                                details = "Erişilebilirlik olayı alındı: ${matchingApp.appName} açıldı."
                            )
                        }
                    } else {
                        withContext(Dispatchers.IO) {
                            repository.updateSessionLastSeen(matchingApp.packageName)
                        }
                    }

                    if (matchingApp.remainingSecondsToday <= 0 || matchingApp.isFailed) {
                        if (com.gardiyan.app.BuildConfig.DEBUG) {
                            Log.w(TAG, "Limit already reached or failed for: ${matchingApp.appName}. Triggering lock overlay.")
                        }
                        tickJob?.cancel()
                        tickJob = null
                        entryTimeMillis = 0L
                        repository.failRestrictedApp(matchingApp.id)
                        if (!BlockOverlayService.isLockOverlayFor(matchingApp.packageName)) {
                            ignoreOwnPackageEventsUntil = System.currentTimeMillis() + 1500L
                            BlockOverlayService.showLockOverlay(
                                applicationContext,
                                matchingApp.appName,
                                matchingApp.packageName
                            )
                            withContext(Dispatchers.IO) {
                                repository.closeActiveSession("Kısıtlama süresi dolduğu için kilitlendi")
                                val lockReason = if (matchingApp.remainingSecondsToday <= 0) "Günlük kullanım limiti doldu" else "Kısıtlama kuralı veya ihlal gereği"
                                repository.insertLog(
                                    eventType = "OVERLAY_SHOWN",
                                    appName = matchingApp.appName,
                                    details = "${matchingApp.appName} için kilit ekranı gösterildi. Gerekçe: $lockReason."
                                )
                            }
                        }
                    } else {
                        val remaining = matchingApp.remainingSecondsToday
                        if (com.gardiyan.app.BuildConfig.DEBUG) {
                            Log.d(TAG, "Timer starting for ${matchingApp.appName}. Remaining time: ${remaining}s")
                        }
                        tickJob?.cancel()
                        tickJob = a11yScope.launch {
                            try {
                                delay(remaining * 1000L)
                                foregroundMutex.withLock {
                                    if (currentForegroundPackage != matchingApp.packageName ||
                                        currentTrackedPackage != matchingApp.packageName
                                    ) {
                                        Log.d(TAG, "Tick ignored because foreground changed to $currentForegroundPackage")
                                        return@withLock
                                    }
                                    Log.d(TAG, "Tick fired: ${matchingApp.appName} remaining=$remaining reached zero")
                                    repository.updateRestrictedApp(
                                        matchingApp.copy(
                                            remainingSecondsToday = 0,
                                            remainingMinutesToday = 0
                                        )
                                    )
                                    entryTimeMillis = 0L
                                    repository.failRestrictedApp(matchingApp.id)
                                    ignoreOwnPackageEventsUntil = System.currentTimeMillis() + 1500L
                                    BlockOverlayService.showLockOverlay(
                                        applicationContext,
                                        matchingApp.appName,
                                        matchingApp.packageName
                                    )
                                    withContext(Dispatchers.IO) {
                                        repository.closeActiveSession("Kısıtlama süresi dolduğu için kilitlendi (tick)")
                                        repository.insertLog(
                                            eventType = "OVERLAY_SHOWN",
                                            appName = matchingApp.appName,
                                            details = "${matchingApp.appName} için kilit ekranı gösterildi. Gerekçe: Günlük kullanım limiti doldu."
                                        )
                                    }
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                Log.d(TAG, "Tick cancelled (exited before time up)")
                                throw e
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in handleForegroundChange: ${e.message}", e)
                }
            }
        }
    }

    private fun handleLimitraForeground() {
        a11yScope.launch {
            foregroundMutex.withLock {
                currentForegroundPackage = packageName
                tickJob?.cancel()
                tickJob = null
                BlockOverlayService.hideLockOverlay()

                val db = GuardianDatabase.getDatabase(applicationContext)
                val repository = GuardianRepository(applicationContext, db.guardianDao())
                withContext(Dispatchers.IO) {
                    repository.closeActiveSession("Limitra açıldı")
                }
                clearTrackingState()
                Log.i(TAG, "Limitra opened; lock overlay and tracked session cleared")
            }
        }
    }

    private fun clearTrackingState() {
        currentTrackedPackage = null
        currentTrackedAppId = -1L
        entryTimeMillis = 0L
        tickJob?.cancel()
        tickJob = null
    }

    private suspend fun handleExit(
        repository: GuardianRepository,
        activeApps: List<RestrictedAppEntity>
    ) {
        val trackedPkg = currentTrackedPackage ?: return
        val trackedApp = activeApps.firstOrNull { it.packageName == trackedPkg }
            ?: run {
                if (BlockOverlayService.isLockOverlayVisible.get()) {
                    BlockOverlayService.hideLockOverlay()
                }
                withContext(Dispatchers.IO) {
                    repository.closeActiveSession("İzlenen uygulama artık bugün için aktif değil")
                }
                clearTrackingState()
                return
            }

        tickJob?.cancel()
        tickJob = null

        if (BlockOverlayService.isLockOverlayVisible.get() &&
            (trackedApp.remainingSecondsToday <= 0 || trackedApp.isFailed)
        ) {
            Log.d(TAG, "Exited locked target ${trackedApp.appName}; hiding package-scoped overlay")
            BlockOverlayService.hideLockOverlay()
            withContext(Dispatchers.IO) {
                repository.closeActiveSession("Kilitli uygulamadan çıkıldı")
            }
            clearTrackingState()
            return
        }

        withContext(Dispatchers.IO) {
            repository.closeActiveSession("Uygulamadan çıkıldı")
        }

        if (BlockOverlayService.isLockOverlayVisible.get()) {
            BlockOverlayService.hideLockOverlay()
        }

        clearTrackingState()
    }

    private fun checkAndTriggerNotifications(app: RestrictedAppEntity) {
        val entryTime = entryTimeMillis
        if (entryTime == 0L) return

        val elapsedSec = (System.currentTimeMillis() - entryTime) / 1000L
        val currentRemainingSec = (app.remainingSecondsToday - elapsedSec).coerceAtLeast(0L)

        val totalLimitSec = if (app.dailyLimitMinutes > 0) app.dailyLimitMinutes * 60 else 10
        val halfLimitSec = totalLimitSec / 2

        val prefs = getSharedPreferences("gardiyan_notifications", Context.MODE_PRIVATE)
        val today = todayKey()

        // 1. %50 kısıtlama limiti bildirimi
        val keyHalf = "notif_50_${app.packageName}_$today"
        if (currentRemainingSec <= halfLimitSec && !prefs.getBoolean(keyHalf, false)) {
            if (currentRemainingSec > 0) {
                prefs.edit().putBoolean(keyHalf, true).apply()
                sendThresholdNotification(app.appName, R.string.notif_half_limit_message)
            }
        }

        // 2. 5 dakika (300 saniye) kala bildirimi
        val key5Min = "notif_5min_${app.packageName}_$today"
        if (totalLimitSec > 300 && currentRemainingSec <= 300 && !prefs.getBoolean(key5Min, false)) {
            if (currentRemainingSec > 0) {
                prefs.edit().putBoolean(key5Min, true).apply()
                sendThresholdNotification(app.appName, R.string.notif_five_minutes_message)
            }
        }
    }

    private fun sendThresholdNotification(appName: String, messageTemplateResId: Int) {
        val prefs = getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notifications_enabled", true)) {
            return
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channelId = "limitra_events_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_service_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val message = getString(messageTemplateResId, appName)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Limitra")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notifId = appName.hashCode() + messageTemplateResId
        notificationManager.notify(notifId, notification)
    }

    private fun todayKey(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }
}

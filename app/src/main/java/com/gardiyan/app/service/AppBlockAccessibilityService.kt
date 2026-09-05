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
import com.gardiyan.app.data.model.isScheduledAt
import com.gardiyan.app.data.repository.GuardianRepository
import com.gardiyan.app.data.repository.UsageStatsReconciliationResult
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
        private const val USAGE_STATS_RECONCILE_INTERVAL_MS = 60_000L
        private const val LONG_FOREGROUND_LOOKBACK_MS = 6 * 60 * 60 * 1000L

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

        fun requestHealthRecovery(reason: String): Boolean {
            val service = instance ?: return false
            service.recoverHealth(reason)
            return true
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
        val now = System.currentTimeMillis()
        return getCachedActiveRestrictedApps().filter { it.isScheduledAt(now) }
    }

    /**
     * Ön plandaki paket bugün kısıtlı olduğu hâlde izlenmiyorsa true döner.
     * İzlemenin koptuğu ve yeniden kurulması gerektiği durumu işaret eder.
     */
    private fun isUntrackedRestrictedTarget(foregroundPackage: String): Boolean {
        if (currentTrackedPackage == foregroundPackage) return false
        return getCachedActiveRestrictedAppsForToday().any { it.packageName == foregroundPackage }
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

                    queryCurrentForegroundPackage()?.let { foregroundPackage ->
                        handleForegroundChange(foregroundPackage, allowRestrictedEntry = false)
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

            handleForegroundChange(foregroundPackage, allowRestrictedEntry = true)
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
            var lastUsageStatsReconcileTime = 0L
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

                    val fgPkg = currentForegroundPackage
                    if (fgPkg != null && fgPkg != packageName) {
                        val app = cachedRestrictedApps.firstOrNull { it.packageName == fgPkg }
                        if (app != null) {
                            val scheduledNow = app.isScheduledAt(now)
                            if (currentTrackedPackage == fgPkg) {
                                if (!scheduledNow) {
                                    // A scheduled window may end while the target stays open.
                                    // Re-run foreground handling so the session is closed and
                                    // no usage outside the configured window is charged.
                                    handleForegroundChange(fgPkg, allowRestrictedEntry = false)
                                } else {
                                    withContext(Dispatchers.IO) {
                                        repository.updateSessionLastSeen(fgPkg)
                                    }
                                    // updateSessionLastSeen kalan süreyi canlı düşer.
                                    // Süre burada biterse kullanıcı hedefte durduğu için
                                    // yeni bir ön plan olayı gelmeyebilir; sayaca ek olarak
                                    // her turda taze DB değeriyle kilit denetlenir.
                                    val latest = withContext(Dispatchers.IO) {
                                        repository.getRestrictedAppByIdSync(app.id)
                                    }
                                    if (latest != null &&
                                        latest.isActive &&
                                        (latest.remainingSecondsToday <= 0 || latest.isFailed)
                                    ) {
                                        foregroundMutex.withLock {
                                            if (currentTrackedPackage == fgPkg &&
                                                currentForegroundPackage == fgPkg
                                            ) {
                                                enforceExhaustedLock(latest, repository, "canlı denetim")
                                            }
                                        }
                                    } else if (app.isActive && !app.isFailed) {
                                        checkAndTriggerNotifications(app)
                                    }
                                }
                            } else if (scheduledNow) {
                                // Target app is open and its active scheduled window just started!
                                // Start tracking and trigger lock immediately if limit is 0 or exhausted.
                                handleForegroundChange(fgPkg, allowRestrictedEntry = true)
                            }
                        }
                    }

                    // Ekran ve Kilit durumu tespiti
                    val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isScreenOn = pm?.isInteractive ?: true

                    val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    val isLocked = km?.isKeyguardLocked ?: false

                    if (now - lastUsageStatsReconcileTime > USAGE_STATS_RECONCILE_INTERVAL_MS) {
                        lastUsageStatsReconcileTime = now
                        val reconciled = withContext(Dispatchers.IO) {
                            repository.reconcileRestrictedAppsWithUsageStats()
                        }
                        val exhaustedByUsageStats = reconciled.filter {
                            it.adjustedSeconds > 0 && it.remainingSecondsToday <= 0
                        }
                        if (exhaustedByUsageStats.isNotEmpty() && isScreenOn && !isLocked) {
                            val foregroundFromUsageStats = queryCurrentForegroundPackage(LONG_FOREGROUND_LOOKBACK_MS)
                            val effectiveForeground = foregroundFromUsageStats ?: currentForegroundPackage
                            val exhaustedForegroundApp = exhaustedByUsageStats.firstOrNull {
                                it.packageName == effectiveForeground
                            }
                            if (exhaustedForegroundApp != null && exhaustedForegroundApp.packageName == currentTrackedPackage) {
                                enforceUsageStatsLimitIfNeeded(exhaustedForegroundApp, repository)
                            }
                        }
                    }

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
                    val foregroundPkg = queryCurrentForegroundPackage()
                    if (foregroundPkg != null) {
                        if (
                            foregroundPkg == packageName &&
                            BlockOverlayService.isLockOverlayVisible.get()
                        ) {
                            // Bu dal eskiden kilidi koşulsuz kaldırıyordu. Yapışkan
                            // kilitte bu bir atlatma yolu olurdu: kilit penceresi
                            // odaklanabilir olduğu için UsageStats/aktif pencere
                            // sorgusu Limitra'yı ön planda sanabiliyor.
                            // Limitra'nın gerçekten açıldığı iki kesin yoldan anlaşılır:
                            // MainActivity.onResume ve MainActivity sınıfını taşıyan
                            // TYPE_WINDOW_STATE_CHANGED olayı. Burada yalnız kayıt tutulur.
                            Log.d(TAG, "Own package reported foreground while lock overlay visible; ignoring")
                        } else if (
                            foregroundPkg != packageName &&
                            (
                                foregroundPkg != currentForegroundPackage ||
                                    // Ön plan paketi değişmemiş olsa bile izleme kopmuş
                                    // olabilir: araya sistem arayüzü girip çıktığında
                                    // oturum kapanır ama kullanıcı hedefte kalır. Bu
                                    // durumda ön plan paketi zaten hedefe eşit olduğu
                                    // için eski koşul bir daha hiç tetiklenmiyor, süre
                                    // düşülmüyor ve sayaç kurulmuyordu.
                                    isUntrackedRestrictedTarget(foregroundPkg)
                                )
                        ) {
                            if (foregroundPkg != currentForegroundPackage) {
                                Log.w(TAG, "UsageStats fallback detected different package: $foregroundPkg (A11y had: $currentForegroundPackage)")
                                withContext(Dispatchers.IO) {
                                    repository.insertLog(
                                        eventType = "USAGE_STATS_FALLBACK",
                                        appName = "",
                                        details = "UsageStats yedek doğrulaması: Ön plan = $foregroundPkg (A11y = $currentForegroundPackage)"
                                    )
                                }
                            } else {
                                Log.w(TAG, "Tracking lost while staying in restricted target: $foregroundPkg. Recovering.")
                            }
                            handleForegroundChange(foregroundPkg, allowRestrictedEntry = false)
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

    private fun recoverHealth(reason: String) {
        Log.w(TAG, "Recovering accessibility health: $reason")
        heartbeatJob?.cancel()
        heartbeatJob = null
        usageStatsPollingJob?.cancel()
        usageStatsPollingJob = null

        recordHealthyTrackingTick()
        startHeartbeat()
        startUsageStatsPolling()

        a11yScope.launch {
            queryCurrentForegroundPackage()?.let { foregroundPackage ->
                handleForegroundChange(foregroundPackage, allowRestrictedEntry = false)
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
     * aktivite olaylarını değerlendirerek aktif foreground uygulamasını döndürür.
     */
    private data class ForegroundEvent(
        val packageName: String,
        val timestampMillis: Long
    )

    private fun queryForegroundEvent(lookbackMillis: Long = 5000L): ForegroundEvent? {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null

            val endTime = System.currentTimeMillis()
            val startTime = endTime - lookbackMillis.coerceAtLeast(1000L)

            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            val records = mutableListOf<UsageEventRecord>()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val pkg = event.packageName
                if (!pkg.isNullOrBlank()) {
                    records.add(
                        UsageEventRecord(
                            packageName = pkg,
                            eventType = event.eventType,
                            timestampMillis = event.timeStamp
                        )
                    )
                }
            }

            val foregroundPkg = UsageStatsForegroundResolver.resolveForegroundPackage(records)
                ?: return null
            val latestTimestamp = records.filter { it.packageName == foregroundPkg }.maxOfOrNull { it.timestampMillis }
                ?: endTime
            ForegroundEvent(foregroundPkg, latestTimestamp)
        } catch (e: SecurityException) {
            Log.w(TAG, "UsageStats permission not granted: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "queryForegroundEvent error: ${e.message}")
            null
        }
    }

    private fun queryCurrentForegroundPackage(lookbackMillis: Long = 5000L): String? {
        val activeWindowPackage = rootInActiveWindow
            ?.packageName
            ?.toString()
            ?.takeIf { it.isNotBlank() }

        if (
            activeWindowPackage != null &&
            !(activeWindowPackage == packageName && BlockOverlayService.isLockOverlayVisible.get())
        ) {
            return activeWindowPackage
        }

        return queryForegroundEvent(lookbackMillis)?.packageName
    }

    /**
     * Verilen paketin GERÇEKTEN şu an aktif pencerede olduğunu erişilebilirlik
     * ağacından teyit eder. UsageStats bayat veri döndürebildiği için, kilit
     * kararını a11y olayı dışında bir kaynak tetiklediğinde bu teyit kullanılır.
     */
    private fun isForegroundConfirmedByActiveWindow(packageName: String): Boolean {
        return try {
            val activeWindowPackage = rootInActiveWindow?.packageName?.toString()
            if (activeWindowPackage == packageName) return true
            val recentEvent = queryForegroundEvent(2000L)
            recentEvent?.packageName == packageName
        } catch (e: Exception) {
            Log.w(TAG, "isForegroundConfirmedByActiveWindow failed: ${e.message}")
            false
        }
    }

    // ========================================================================
    // Foreground Change Handler
    // ========================================================================

    /**
     * Ön plan uygulama değişikliği işleyicisi. Event-driven timer'ın kalbi.
     * Çoklu uygulama listesine göre çalışır.
     */
    private fun handleForegroundChange(
        foregroundPackage: String,
        allowRestrictedEntry: Boolean = false
    ) {
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
                    
                    // Engelleme kararında önbellek kullanma. Limit sıfıra indiğinde
                    // Room Flow'unun RAM önbelleğini güncellemesi kısa sürebilir; bu
                    // aralıkta hedef yeniden açılırsa eski süreyle devam edebilirdi.
                    val activeApps = withContext(Dispatchers.IO) {
                        repository.getActiveRestrictedAppsForTodaySync()
                    }

                    if (com.gardiyan.app.BuildConfig.DEBUG) {
                        Log.d(TAG, "handleForegroundChange: foregroundPackage=$foregroundPackage, allowRestrictedEntry=$allowRestrictedEntry, activeAppsCount=${activeApps.size}")
                    }

                    if (activeApps.isEmpty()) {
                        if (BlockOverlayService.isLockOverlayVisible.get()) {
                            BlockOverlayService.forceHideLockOverlay(
                                "Bugun icin aktif kisitlama kalmadi"
                            )
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
                        if (currentTrackedPackage == foregroundPackage) {
                            handleExit(repository, activeApps)
                        }
                        if (BlockOverlayService.isLockOverlayVisible.get()) {
                            // Yapışkan kilit aktifse bu istek yok sayılır: kullanıcı ana
                            // ekrana ya da başka bir uygulamaya geçse bile kilit kalır.
                            Log.d(TAG, "Soft hide request because foreground package is unrelated: $foregroundPackage")
                            BlockOverlayService.requestHideLockOverlay(
                                "Ilgisiz on plan paketi: $foregroundPackage"
                            )
                        }
                        return@withLock
                    }

                    val isNewEntry = currentTrackedPackage != matchingApp.packageName
                    val isExhausted = matchingApp.remainingSecondsToday <= 0 || matchingApp.isFailed
                    if (isNewEntry && !allowRestrictedEntry) {
                        // Erişilebilirlik pencere olayı her zaman gelmez: araya sistem
                        // arayüzü, klavye ya da başlatıcı girdiğinde izleme kopar ve
                        // kullanıcı hedefte kalmaya devam eder. Bu durumda giriş
                        // yalnızca a11y olayına bırakılırsa oturum hiç kurulamaz;
                        // süre düşülmez ve sayaç kurulmadığı için kilit hiç gelmez.
                        //
                        // Bu yüzden polling de girişi kurabilir, ancak yalnızca hedefin
                        // GERÇEKTEN aktif pencerede olduğu canlı erişilebilirlik
                        // ağacından teyit edilirse. UsageStats'ın bayat verisi tek
                        // başına yeterli değildir, dolayısıyla sahte giriş oluşmaz.
                        val confirmedByActiveWindow =
                            isForegroundConfirmedByActiveWindow(matchingApp.packageName)
                        if (!confirmedByActiveWindow) {
                            Log.d(
                                TAG,
                                "handleForegroundChange: Restricted entry for ${matchingApp.packageName} blocked (not confirmed by active window)"
                            )
                            return@withLock
                        }
                        Log.w(
                            TAG,
                            "handleForegroundChange: Recovering tracking for ${matchingApp.packageName} (confirmed by active window, exhausted=$isExhausted)"
                        )
                    }

                    // Kısıtlı uygulamaya GİRİLDİ
                    if (isNewEntry) {
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

                    if (isExhausted) {
                        if (com.gardiyan.app.BuildConfig.DEBUG) {
                            Log.w(TAG, "Limit already reached or failed for: ${matchingApp.appName}. Triggering lock overlay.")
                        }
                        tickJob?.cancel()
                        tickJob = null
                        entryTimeMillis = 0L
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
                    } else if (
                        CountdownPolicy.shouldRestartCountdown(
                            isNewEntry = isNewEntry,
                            isCountdownRunning = tickJob?.isActive == true
                        )
                    ) {
                        // Aynı uygulama içindeki gezinme de (video değişimi, tam ekran,
                        // dialog, sekme) TYPE_WINDOW_STATE_CHANGED üretir. Eskiden bu
                        // olayların her biri sayacı iptal edip kalan sürenin TAMAMIYLA
                        // yeniden başlatıyordu; uzun oturumlarda sayaç asla dolmuyor,
                        // kilit ancak Limitra açılıp kapandıktan sonra geliyordu.
                        // Çalışan bir sayaca artık dokunulmaz.
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
                                    val latestApp = repository.getRestrictedAppByIdSync(matchingApp.id)
                                        ?: return@withLock
                                    enforceExhaustedLock(latestApp, repository, "sayaç")
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

    /**
     * Süresi dolmuş hedefi kilitler. Çağıran taraf [foregroundMutex] kilidini
     * tutmalıdır. [tickJob] bilerek iptal edilmez: bu fonksiyon sayacın kendi
     * gövdesinden de çağrıldığı için iptal, çağrının geri kalanını düşürürdü.
     */
    private suspend fun enforceExhaustedLock(
        app: RestrictedAppEntity,
        repository: GuardianRepository,
        reason: String
    ) {
        if (!app.isScheduledAt(System.currentTimeMillis())) {
            withContext(Dispatchers.IO) {
                repository.closeActiveSession("Aktif saat aralığı sona erdi")
            }
            clearTrackingState()
            return
        }

        val exhaustedApp = app.copy(remainingSecondsToday = 0, remainingMinutesToday = 0)
        if (app.remainingSecondsToday != 0 || app.remainingMinutesToday != 0) {
            withContext(Dispatchers.IO) {
                repository.updateRestrictedApp(exhaustedApp)
            }
        }
        // Flow güncellemesi gelene kadar tüm hızlı denetimler de sıfır süreyi görsün.
        cachedRestrictedApps = cachedRestrictedApps.map { cached ->
            if (cached.id == exhaustedApp.id) exhaustedApp else cached
        }
        entryTimeMillis = 0L

        if (BlockOverlayService.isLockOverlayFor(app.packageName)) return

        ignoreOwnPackageEventsUntil = System.currentTimeMillis() + 1500L
        BlockOverlayService.showLockOverlay(
            applicationContext,
            app.appName,
            app.packageName
        )
        withContext(Dispatchers.IO) {
            repository.closeActiveSession("Kısıtlama süresi dolduğu için kilitlendi ($reason)")
            repository.insertLog(
                eventType = "OVERLAY_SHOWN",
                appName = app.appName,
                details = "${app.appName} için kilit ekranı gösterildi. Gerekçe: Günlük kullanım limiti doldu ($reason)."
            )
        }
    }

    private suspend fun enforceUsageStatsLimitIfNeeded(
        result: UsageStatsReconciliationResult,
        repository: GuardianRepository
    ) {
        if (result.remainingSecondsToday > 0) return
        if (currentTrackedPackage != result.packageName) return
        entryTimeMillis = 0L
        tickJob?.cancel()
        tickJob = null

        if (!BlockOverlayService.isLockOverlayFor(result.packageName)) {
            ignoreOwnPackageEventsUntil = System.currentTimeMillis() + 1500L
            BlockOverlayService.showLockOverlay(
                applicationContext,
                result.appName,
                result.packageName
            )
            withContext(Dispatchers.IO) {
                repository.closeActiveSession("UsageStats uzlaştırması limiti doldurdu")
                repository.insertLog(
                    eventType = "OVERLAY_SHOWN",
                    appName = result.appName,
                    details = "${result.appName} için kilit ekranı gösterildi. Gerekçe: UsageStats uzlaştırması ile günlük kullanım limiti doldu."
                )
            }
        }
    }

    private fun handleLimitraForeground() {
        a11yScope.launch {
            foregroundMutex.withLock {
                currentForegroundPackage = packageName
                tickJob?.cancel()
                tickJob = null
                // Limitra, kilit ekranından çıkışın ve kısıtlama yönetiminin
                // meşru yoludur; yapışkan kilit burada koşulsuz kaldırılır.
                BlockOverlayService.forceHideLockOverlay("Limitra on plana geldi")

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
                    BlockOverlayService.forceHideLockOverlay(
                        "Izlenen uygulama artik bugun icin aktif degil"
                    )
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
            // BUG DÜZELTMESİ: Eskiden kilitli hedeften çıkış kilidi kaldırıyordu.
            // Kullanıcı ana ekrana çekme hareketini yarıda bırakıp uygulamaya
            // dönünce kilit geri gelmiyor ve kısıtlama atlatılabiliyordu.
            // Artık yapışkan kilit yalnızca butonla kalkar.
            Log.d(TAG, "Exited locked target ${trackedApp.appName}; soft hide request")
            BlockOverlayService.requestHideLockOverlay(
                "Kilitli hedeften cikildi: ${trackedApp.packageName}"
            )
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
            BlockOverlayService.requestHideLockOverlay("Uygulamadan cikildi: $trackedPkg")
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

data class ForegroundEvaluationResult(
    val nextTrackedPackage: String?,
    val shouldStartSession: Boolean,
    val shouldUpdateSession: Boolean,
    val shouldCloseSession: Boolean,
    val shouldShowLockOverlay: Boolean,
    val shouldHideLockOverlay: Boolean,
    val isRestrictedEntryAllowed: Boolean
)

/**
 * Geri sayımın ne zaman yeniden kurulacağını belirler.
 *
 * Kısıtlı uygulamanın kendi içindeki gezinme de ön plan olayı ürettiği için,
 * her olayda sayacı kalan sürenin tamamıyla yeniden başlatmak sayacın hiç
 * dolmamasına yol açıyordu. Sayaç yalnızca gerçekten yeni bir girişte ya da
 * çalışan sayaç kalmadığında kurulur.
 */
object CountdownPolicy {
    fun shouldRestartCountdown(isNewEntry: Boolean, isCountdownRunning: Boolean): Boolean {
        return isNewEntry || !isCountdownRunning
    }
}

object ForegroundPolicyEvaluator {
    fun evaluate(
        currentTrackedPackage: String?,
        candidatePackage: String,
        isCandidateRestrictedToday: Boolean,
        isCandidateLimitExhaustedOrFailed: Boolean,
        allowRestrictedEntry: Boolean,
        isForegroundConfirmedByActiveWindow: Boolean = false
    ): ForegroundEvaluationResult {
        if (!isCandidateRestrictedToday) {
            val hadTracked = currentTrackedPackage != null
            return ForegroundEvaluationResult(
                nextTrackedPackage = null,
                shouldStartSession = false,
                shouldUpdateSession = false,
                shouldCloseSession = hadTracked,
                shouldShowLockOverlay = false,
                shouldHideLockOverlay = true,
                isRestrictedEntryAllowed = false
            )
        }

        val isSameAsTracked = currentTrackedPackage == candidatePackage
        // Canlı pencere teyidi varsa giriş, süre dolmuş olsun ya da olmasın kurulur.
        //
        // Eskiden yalnız süresi dolmuş hedef için teyide izin veriliyordu. Bunun yan
        // etkisi ağırdı: araya sistem arayüzü veya klavye girip izleme koptuğunda,
        // süresi HENÜZ dolmamış hedef yeniden izlemeye alınamıyordu. Oturum
        // kurulmadığı için süre düşülmüyor, sayaç kurulmadığı için kilit hiç
        // gelmiyordu; kısıtlama ancak Limitra açılıp kapandıktan sonra devreye
        // giriyordu. Teyit canlı erişilebilirlik ağacından geldiği için bayat
        // UsageStats verisiyle sahte giriş yine mümkün değildir.
        if (!isSameAsTracked && !allowRestrictedEntry && !isForegroundConfirmedByActiveWindow) {
            val hadTracked = currentTrackedPackage != null
            return ForegroundEvaluationResult(
                nextTrackedPackage = null,
                shouldStartSession = false,
                shouldUpdateSession = false,
                shouldCloseSession = hadTracked,
                shouldShowLockOverlay = false,
                shouldHideLockOverlay = false,
                isRestrictedEntryAllowed = false
            )
        }

        val isNewEntry = !isSameAsTracked
        return ForegroundEvaluationResult(
            nextTrackedPackage = candidatePackage,
            shouldStartSession = isNewEntry,
            shouldUpdateSession = !isNewEntry,
            shouldCloseSession = false,
            shouldShowLockOverlay = isCandidateLimitExhaustedOrFailed,
            shouldHideLockOverlay = false,
            isRestrictedEntryAllowed = true
        )
    }
}


package com.gardiyan.app.service

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.gardiyan.app.MainActivity
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

        @Volatile
        var instance: AppBlockAccessibilityService? = null

        @Volatile
        var isRunning: Boolean = false

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
    }

    private val a11yJob = SupervisorJob()
    private val a11yScope = CoroutineScope(Dispatchers.IO + a11yJob)
    private val foregroundMutex = Mutex()

    // Kısıtlı uygulamada kalındığı sürece overlay'i tetikleyecek bekleyen coroutine.
    // Her girişte iptal edilip yeniden başlatılır.
    private var tickJob: Job? = null

    // UsageStats polling coroutine
    private var usageStatsPollingJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        Log.i(TAG, "Accessibility service connected")

        val db = GuardianDatabase.getDatabase(applicationContext)
        val repository = GuardianRepository(db.guardianDao())
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
                    details = if (isRestart) "Gardiyan koruma motoru yeniden başlatıldı." else "Gardiyan koruma motoru başarıyla başlatıldı."
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
        usageStatsPollingJob?.cancel()
        usageStatsPollingJob = null
        Log.w(TAG, "Accessibility service unbound")

        val db = GuardianDatabase.getDatabase(applicationContext)
        val repository = GuardianRepository(db.guardianDao())
        runBlocking {
            try {
                withContext(Dispatchers.IO) {
                    repository.closeActiveSession("Servis sonlandırıldı (Unbind)")
                    repository.insertLog(
                        eventType = "SERVICE_STOPPED",
                        appName = "",
                        details = "Gardiyan koruma motoru durduruldu (Unbind)."
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        isRunning = false
        instance = null
        tickJob?.cancel()
        tickJob = null
        usageStatsPollingJob?.cancel()
        usageStatsPollingJob = null

        val db = GuardianDatabase.getDatabase(applicationContext)
        val repository = GuardianRepository(db.guardianDao())
        runBlocking {
            try {
                withContext(Dispatchers.IO) {
                    repository.closeActiveSession("Servis sonlandırıldı (Destroy)")
                    repository.insertLog(
                        eventType = "SERVICE_STOPPED",
                        appName = "",
                        details = "Gardiyan koruma motoru durduruldu (Destroy)."
                    )
                }
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
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return

        if (
            foregroundPackage == packageName &&
            event.className?.toString() == MainActivity::class.java.name
        ) {
            handleGardiyanForeground()
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

        handleForegroundChange(foregroundPackage)
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
            val repository = GuardianRepository(db.guardianDao())

            var startupTicksLeft = 60 // İlk 15 saniye boyu hızlı denetim (60 * 250ms = 15s)
            var lastEventTimeForRapidCheck = 0L
            var currentPollIntervalMs = NORMAL_POLL_INTERVAL_MS

            while (isActive) {
                try {
                    currentTrackedPackage
                        ?.takeIf { it == currentForegroundPackage }
                        ?.let { pkg ->
                        withContext(Dispatchers.IO) {
                            repository.updateSessionLastSeen(pkg)
                        }
                    }

                    val now = System.currentTimeMillis()
                    val a11yAge = now - lastAccessibilityForegroundAt
                    val overlayShouldBeVisible = currentTrackedPackage != null
                    val overlayIsVisible = BlockOverlayService.isLockOverlayVisible.get()
                    val isForegroundUnclear = currentForegroundPackage.isNullOrEmpty()

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

                    if (startupTicksLeft > 0) {
                        suspiciousReason = "Servis başlangıcı"
                        startupTicksLeft--
                    } else if (a11yAge > 8000L) {
                        suspiciousReason = "A11y olayı gecikti (${a11yAge / 1000}sn)"
                    } else if (overlayShouldBeVisible && !overlayIsVisible) {
                        suspiciousReason = "Kilit ekranı görünmüyor (beklenen: $currentTrackedPackage)"
                    } else if (isForegroundUnclear) {
                        suspiciousReason = "Ön plan paket bilgisi tanımsız"
                    } else if (isRapidSwitching) {
                        suspiciousReason = "Hızlı uygulama geçişi algılandı"
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
                            handleGardiyanForeground()
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

    /**
     * UsageStatsManager.queryEvents() ile son 5 saniye içindeki en son
     * MOVE_TO_FOREGROUND event'ini bularak aktif foreground uygulamasını döndürür.
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
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
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
                    val repository = GuardianRepository(db.guardianDao())
                    val activeApps = withContext(Dispatchers.IO) {
                        repository.getActiveRestrictedAppsForTodaySync()
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

                    if (currentTrackedPackage != null && currentTrackedPackage != foregroundPackage) {
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

    private fun handleGardiyanForeground() {
        a11yScope.launch {
            foregroundMutex.withLock {
                currentForegroundPackage = packageName
                tickJob?.cancel()
                tickJob = null
                BlockOverlayService.hideLockOverlay()

                val db = GuardianDatabase.getDatabase(applicationContext)
                val repository = GuardianRepository(db.guardianDao())
                withContext(Dispatchers.IO) {
                    repository.closeActiveSession("Gardiyan açıldı")
                }
                clearTrackingState()
                Log.i(TAG, "Gardiyan opened; lock overlay and tracked session cleared")
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
}

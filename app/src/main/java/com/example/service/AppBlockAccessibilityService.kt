package com.example.service

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.data.local.database.GuardianDatabase
import com.example.data.local.entity.RestrictedAppEntity
import com.example.data.repository.GuardianRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Çoklu uygulama + event-driven ön plan tespiti ve zamanlayıcı.
 *
 * Mimari:
 * - PRIMARY: TYPE_WINDOW_STATE_CHANGED event'lerini dinler
 * - SECONDARY: UsageStatsManager.queryEvents() ile 2sn polling yedek katmanı
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

        /** UsageStats polling aralığı (ms) */
        private const val USAGE_STATS_POLL_INTERVAL_MS = 300L

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
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        isRunning = false
        instance = null
        tickJob?.cancel()
        tickJob = null
        usageStatsPollingJob?.cancel()
        usageStatsPollingJob = null
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
        currentForegroundPackage = foregroundPackage
        lastAccessibilityForegroundAt = now

        handleForegroundChange(foregroundPackage)
    }

    // ========================================================================
    // UsageStatsManager Polling — Yedek Katman
    // ========================================================================

    /**
     * Her 2 saniyede bir UsageStatsManager.queryEvents() ile son foreground
     * uygulamayı sorgular. AccessibilityService event'lerinin atlandığı
     * durumlarda (PiP, popup, Activity geçişleri) yedek olarak devreye girer.
     */
    private fun startUsageStatsPolling() {
        usageStatsPollingJob?.cancel()
        usageStatsPollingJob = a11yScope.launch {
            Log.i(TAG, "UsageStats polling started (interval: ${USAGE_STATS_POLL_INTERVAL_MS}ms)")
            while (isActive) {
                try {
                    val foregroundEvent = queryForegroundEvent()
                    if (foregroundEvent != null && foregroundEvent.timestampMillis >= lastAccessibilityForegroundAt) {
                        val foregroundPkg = foregroundEvent.packageName

                        // Kilit ekranı aktifken kendi paketimiz için poller üzerinden tetikleme yapma
                        if (foregroundPkg == packageName && BlockOverlayService.isLockOverlayVisible.get()) {
                            delay(USAGE_STATS_POLL_INTERVAL_MS)
                            continue
                        }

                        // Sadece AccessibilityService'in kaçırdığı değişiklikleri yakala
                        val currentForeground = currentForegroundPackage
                        if (foregroundPkg != currentForeground) {
                            Log.d(TAG, "UsageStats polling detected foreground change: $foregroundPkg (foreground: $currentForeground)")
                            handleForegroundChange(foregroundPkg)
                        }
                    } else if (foregroundEvent != null) {
                        Log.d(TAG, "UsageStats polling ignored stale foreground event: ${foregroundEvent.packageName}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "UsageStats polling error: ${e.message}")
                }
                delay(USAGE_STATS_POLL_INTERVAL_MS)
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
     *
     * Hem AccessibilityService event'lerinden hem UsageStats polling'den çağrılır.
     * Aynı paket için tekrar çağrılırsa no-op.
     */
    private fun handleForegroundChange(foregroundPackage: String) {
        // Kilit ekranı aktifken kendi paketimiz için işlem yapmayı engelle
        if (foregroundPackage == packageName && BlockOverlayService.isLockOverlayVisible.get()) {
            Log.d(TAG, "handleForegroundChange: Ignored own package event because overlay is visible")
            return
        }

        a11yScope.launch {
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
                    clearTrackingState()
                    // Aktif kısıtlama yok, hiçbir şey yapma
                    return@launch
                }

                val matchingApp = activeApps.firstOrNull { it.packageName == foregroundPackage }

                if (currentTrackedPackage != null && currentTrackedPackage != foregroundPackage) {
                    handleExit(repository, activeApps, System.currentTimeMillis())
                }

                if (matchingApp == null) {
                    if (BlockOverlayService.isLockOverlayVisible.get()) {
                        Log.d(TAG, "Hiding lock overlay because foreground package is unrelated: $foregroundPackage")
                        BlockOverlayService.hideLockOverlay()
                    }
                    return@launch
                }

                // Matching package; handle package-scoped timer/block below.
                    // CASE 1: Kısıtlı uygulamaya GİRİLDİ
                        // Önceki izlenen uygulamadan çıkışı işle (varsa ve farklıysa)
                        if (currentTrackedPackage != null && currentTrackedPackage != matchingApp.packageName) {
                            handleExit(repository, activeApps, System.currentTimeMillis())
                        }

                        // Yeni giriş veya aynı uygulamaya devam
                        if (currentTrackedPackage != matchingApp.packageName) {
                            entryTimeMillis = System.currentTimeMillis()
                            currentTrackedPackage = matchingApp.packageName
                            currentTrackedAppId = matchingApp.id
                            Log.d(TAG, "Target app entered: ${matchingApp.appName} (${matchingApp.packageName}) at $entryTimeMillis")
                        }

                        // remaining <= 0 ise overlay'i hemen göster; aksi halde tick coroutine başlat
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
                            }
                        } else {
                            val remaining = matchingApp.remainingSecondsToday
                            tickJob?.cancel()
                            tickJob = a11yScope.launch {
                                try {
                                    delay(remaining * 1000L)
                                    if (currentForegroundPackage != matchingApp.packageName) {
                                        Log.d(TAG, "Tick ignored because foreground changed to $currentForegroundPackage")
                                        return@launch
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
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    Log.d(TAG, "Tick cancelled (exited before time up)")
                                    throw e
                                }
                            }
                        }

                    // CASE 2: Kısıtlı olmayan bir uygulamaya geçildi
            } catch (e: Exception) {
                Log.e(TAG, "Error in handleForegroundChange: ${e.message}", e)
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

    /**
     * İzlenen uygulamadan çıkışı işle: elapsed hesapla, remainingSeconds'i
     * güncelle, state'i sıfırla.
     *
     * NOT: isActive ASLA false yapılmaz. Uygulama aktif kalır.
     * Overlay sadece hideLockOverlay() ile kapatılır (5sn hold veya
     * kullanıcı uygulamadan çıkınca).
     */
    private suspend fun handleExit(
        repository: GuardianRepository,
        activeApps: List<RestrictedAppEntity>,
        exitTime: Long
    ) {
        val trackedPkg = currentTrackedPackage ?: return
        val trackedApp = activeApps.firstOrNull { it.packageName == trackedPkg }
            ?: run {
                // Tracked paket artık aktif listesinde yok, state'i temizle
                clearTrackingState()
                if (BlockOverlayService.isLockOverlayVisible.get()) {
                    BlockOverlayService.hideLockOverlay()
                }
                return
            }

        if (BlockOverlayService.isLockOverlayVisible.get() &&
            (trackedApp.remainingSecondsToday <= 0 || trackedApp.isFailed)
        ) {
            Log.d(TAG, "Exited locked target ${trackedApp.appName}; hiding package-scoped overlay")
            BlockOverlayService.hideLockOverlay()
            clearTrackingState()
            return
        }

        val elapsedMs = exitTime - entryTimeMillis
        val elapsedSec = (elapsedMs / 1000L).toInt().coerceAtLeast(1)
        val newRemaining = (trackedApp.remainingSecondsToday - elapsedSec).coerceAtLeast(0)
        val newMinutes = newRemaining / 60

        // Sadece süreyi güncelle — isActive DEĞİŞTİRİLMEZ
        withContext(Dispatchers.IO) {
            repository.updateRestrictedApp(
                trackedApp.copy(
                    remainingSecondsToday = newRemaining,
                    remainingMinutesToday = newMinutes
                )
            )
            if (newRemaining <= 0) {
                repository.failRestrictedApp(trackedApp.id)
            }
        }

        Log.d(TAG, "Exited ${trackedApp.appName}. Elapsed: ${elapsedSec}s, Remaining: ${newRemaining}s")

        tickJob?.cancel()
        tickJob = null

        // Overlay açıksa kapat (kullanıcı kısıtlı uygulamadan çıktı)
        if (BlockOverlayService.isLockOverlayVisible.get()) {
            BlockOverlayService.hideLockOverlay()
        }

        clearTrackingState()
    }
}

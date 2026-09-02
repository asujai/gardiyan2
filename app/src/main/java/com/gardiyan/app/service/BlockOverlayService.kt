package com.gardiyan.app.service

import android.accessibilityservice.AccessibilityService
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
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
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.lang.ref.WeakReference
import android.annotation.SuppressLint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat

/**
 * Foreground Service — 10 saniye sonsuz döngüde kilit ekranı.
 *
 * Sorumlulukları:
 * 1. Foreground notification ile sürekli çalışır vaziyette kalmak (Android 14+)
 * 2. onTaskRemoved → AlarmManager ile yeniden başlatma
 * 3. AppBlockAccessibilityService'ten gelen "showLockOverlay" komutuyla
 *    WindowManager.addView() kullanarak hedef uygulamanın ÜZERİNE lock overlay çizmek
 * 4. 10 saniyelik geri sayım → 0'a ulaştığında ANINDA 10'a reset → sonsuz döngü
 * 5. Kilit ekranı YAPIŞKANDIR: ön plan değişimi (ana ekrana çekme, uygulama
 *    değiştirme) kilidi kaldırmaz. Tek çıkış yolları: kilit ekranındaki
 *    "Ana sayfaya dön" butonu ve Limitra MainActivity'den 5sn basılı tutma.
 *
 * KRİTİK: Sayaç 0'a ulaştığında:
 * - isActive = false KULLANILMAZ
 * - Overlay kapanmaz
 * - Sayaç anında 10'a resetlenir
 * - Döngü sonsuza kadar devam eder
 *
 * İptal akışı:
 * - ViewModel.cancelAllWithFiveSecondHold() çağrılır
 * - forceHideLockOverlay() çağrılarak overlay kapatılır
 */
class BlockOverlayService : Service() {

    companion object {
        private const val TAG = "BlockOverlayService"
        private const val ALARM_RESTART_REQUEST_CODE = 300
        private const val FALLBACK_PROTECTION_POLL_MS = 2_000L
        private const val FALLBACK_RECONCILE_INTERVAL_MS = 15_000L
        private const val FALLBACK_FOREGROUND_LOOKBACK_MS = 10_000L

        // Ana ekrana geçiş ile kilidin kaldırılması arasındaki gecikme.
        // Kilitli uygulamanın bir an bile görünmesini engeller.
        private const val RETURN_HOME_DISMISS_DELAY_MS = 250L

        // Yapışkan kilit açıkken view'in pencereden düşüp düşmediğini denetleme aralığı.
        private const val STICKY_OVERLAY_WATCHDOG_INTERVAL_MS = 1_000L
        const val CHANNEL_ID = "gardiyan_service_channel"
        const val NOTIF_ID = 101

        // 10 saniye sonsuz döngü
        const val LOCK_CYCLE_SECONDS = 10

        // Emoji harici dosya yolu (URI). null ise varsayılan emoji kullanılır.
        // Bu değer, MainActivity'den veya ayarlardan set edilebilir.
        @Volatile
        var customEmojiUri: String? = null

        // Emoji yazısı (TextView fallback). Default 🚫
        @Volatile
        var defaultEmojiText: String = "🚫"

        @JvmStatic
        val isServiceRunning = AtomicBoolean(false)

        @JvmStatic
        val isLockOverlayVisible = AtomicBoolean(false)

        @JvmStatic
        val isFallbackProtectionActive = AtomicBoolean(false)

        /**
         * Kilit ekranı yapışkan mı? true iken kilit YALNIZCA kullanıcının
         * "Ana sayfaya dön" butonuna basmasıyla (veya forceHideLockOverlay ile)
         * kalkar; ön plan değişimi kilidi kaldırmaz.
         */
        @JvmStatic
        val requiresManualDismiss = AtomicBoolean(false)

        @Volatile
        private var serviceInstance: WeakReference<BlockOverlayService>? = null

        @Volatile
        private var appContextRef: WeakReference<Context>? = null

        @Volatile
        private var pendingOverlayTarget: Pair<String, String>? = null

        @Volatile
        private var visibleOverlayPackage: String? = null

        /**
         * AppBlockAccessibilityService tarafından çağrılır: lock overlay'i
         * pencere yöneticisine ekle. Eğer zaten görünürse no-op.
         * Service çalışmıyorsa pending olarak saklar, service onCreate'te uygular.
         */
        @JvmStatic
        fun showLockOverlay(context: Context, targetAppName: String, targetAppPackage: String) {
            appContextRef = WeakReference(context.applicationContext)
            showLockOverlay(targetAppName, targetAppPackage)
        }

        @JvmStatic
        fun showLockOverlay(targetAppName: String, targetAppPackage: String) {
            Log.i(TAG, "showLockOverlay requested for $targetAppName")
            // Kilit gösterildiği andan itibaren yapışkandır: ön plan değişimi kaldıramaz.
            requiresManualDismiss.set(true)
            val instance = serviceInstance?.get()
            if (instance != null) {
                instance.addLockOverlayView(targetAppName, targetAppPackage)
            } else {
                pendingOverlayTarget = targetAppName to targetAppPackage
                Log.w(TAG, "Service instance null, queuing pending overlay and restarting")
                val ctx = appContextRef?.get() ?: return
                try {
                    start(ctx)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service: ${e.message}")
                }
            }
        }

        /**
         * Ön plan değişiminden kaynaklanan "kilidi kaldır" isteği.
         *
         * Kilit ekranı yapışkan moddayken (requiresManualDismiss) bu istek YOK SAYILIR:
         * kullanıcı alttan yukarı çekip ana ekrana veya başka bir uygulamaya geçse bile
         * kilit ekranı ekranda kalmaya devam eder. Kilit yalnızca kilit ekranındaki
         * "Ana sayfaya dön" butonu ya da diğer meşru yollar (bkz. forceHideLockOverlay)
         * ile kalkar.
         */
        @JvmStatic
        fun requestHideLockOverlay(reason: String) {
            if (OverlayDismissPolicy.shouldIgnoreSoftHide(requiresManualDismiss.get())) {
                Log.d(TAG, "requestHideLockOverlay ignored (sticky lock active): $reason")
                return
            }
            Log.i(TAG, "requestHideLockOverlay accepted: $reason")
            pendingOverlayTarget = null
            serviceInstance?.get()?.removeLockOverlayView()
        }

        /**
         * Kilidi koşulsuz kaldırır ve yapışkan modu sıfırlar.
         *
         * Yalnızca meşru çıkış yolları için:
         * 1. Kilit ekranındaki "Ana sayfaya dön" butonu
         * 2. Limitra MainActivity'nin öne gelmesi (5sn basılı tutma yolu)
         * 3. O gün için aktif kısıtlama kalmaması
         * 4. Kısıtlamanın silinmesi / kullanıcı verilerinin temizlenmesi
         * 5. Servisin yok edilmesi
         */
        @JvmStatic
        fun forceHideLockOverlay(reason: String) {
            Log.i(TAG, "forceHideLockOverlay: $reason")
            requiresManualDismiss.set(false)
            pendingOverlayTarget = null
            serviceInstance?.get()?.removeLockOverlayView()
        }

        @JvmStatic
        fun isLockOverlayFor(packageName: String): Boolean {
            return isLockOverlayVisible.get() && visibleOverlayPackage == packageName
        }

        /**
         * Harici URI'den emoji yüklemek için. Property assignment kullanın:
         * `BlockOverlayService.customEmojiUri = uri` (companion var setter).
         */
        fun start(context: Context) {
            appContextRef = WeakReference(context.applicationContext)
            val serviceIntent = Intent(context, BlockOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var lockOverlayView: View? = null
    private var cycleJob: Job? = null
    private var trackingHealthJob: Job? = null
    private var stickyOverlayWatchdogJob: Job? = null
    private val returnHomeInProgress = AtomicBoolean(false)
    private var lastStickyTarget: Pair<String, String>? = null
    private var fallbackTrackedPackage: String? = null
    private var fallbackTrackedAppId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning.set(true)
        serviceInstance = WeakReference(this)
        appContextRef = WeakReference(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForegroundCompat()
        startTrackingHealthWatchdog()
        startStickyOverlayWatchdog()
        Log.i(TAG, "Service created (10s infinite loop lock)")

        pendingOverlayTarget?.let { (name, pkg) ->
            pendingOverlayTarget = null
            mainHandler.post {
                addLockOverlayViewInternal(name, pkg)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "Task removed, scheduling restart via AlarmManager")
        scheduleRestart()
    }

    override fun onDestroy() {
        Log.w(TAG, "Service destroyed")
        isServiceRunning.set(false)
        isFallbackProtectionActive.set(false)
        requiresManualDismiss.set(false)
        AccessibilityHealthMonitor.recordFallbackProtectionState(applicationContext, false)
        serviceInstance = null
        appContextRef = null
        visibleOverlayPackage = null
        fallbackTrackedPackage = null
        fallbackTrackedAppId = -1L
        cycleJob?.cancel()
        cycleJob = null
        trackingHealthJob?.cancel()
        trackingHealthJob = null
        stickyOverlayWatchdogJob?.cancel()
        stickyOverlayWatchdogJob = null
        lastStickyTarget = null
        removeLockOverlayView()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun scheduleRestart() {
        try {
            val restartServiceIntent = Intent(applicationContext, BlockOverlayService::class.java).apply {
                setPackage(packageName)
            }
            val pendingIntent = PendingIntent.getService(
                this, ALARM_RESTART_REQUEST_CODE, restartServiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmService = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Play Store politika riski ve SCHEDULE_EXACT_ALARM izni gereksinimini kaldırmak için
            // setAndAllowWhileIdle veya normal set kullanımı tercih edilir.
            alarmService.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000L,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule service restart: ${e.message}")
        }
    }

    private fun startForegroundCompat() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_active))
            .setContentText(getString(R.string.overlay_desc))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification,
                foregroundServiceType
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.notification_channel_service_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /**
     * Yapışkan kilit açıkken kilit view'i pencere yöneticisinden düşerse
     * (konfigürasyon değişimi, WindowManager hatası vb.) yeniden ekler.
     */
    private fun startStickyOverlayWatchdog() {
        stickyOverlayWatchdogJob?.cancel()
        stickyOverlayWatchdogJob = serviceScope.launch {
            while (isActive) {
                try {
                    if (requiresManualDismiss.get() && !isLockOverlayVisible.get()) {
                        val target = lastStickyTarget
                        if (target != null) {
                            Log.w(TAG, "Sticky lock lost its window, re-adding for ${target.second}")
                            mainHandler.post {
                                addLockOverlayViewInternal(target.first, target.second)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Sticky overlay watchdog failed: ${e.message}")
                }
                delay(STICKY_OVERLAY_WATCHDOG_INTERVAL_MS)
            }
        }
    }

    private fun startTrackingHealthWatchdog() {
        trackingHealthJob?.cancel()
        trackingHealthJob = serviceScope.launch {
            val db = GuardianDatabase.getDatabase(applicationContext)
            val repository = GuardianRepository(applicationContext, db.guardianDao())
            var hasWarnedUntilHealthy = false
            var lastReconcileAt = 0L

            while (isActive) {
                var nextDelayMs = AccessibilityHealthMonitor.HEARTBEAT_STALE_MS
                try {
                    val activeAppsForToday = withContext(Dispatchers.IO) {
                        repository.getActiveRestrictedAppsForTodaySync()
                    }
                    val hasActiveRestrictions = activeAppsForToday.isNotEmpty()

                    if (hasActiveRestrictions) {
                        var status = AccessibilityHealthMonitor.getStatus(applicationContext)
                        if (
                            status.requiresReenable &&
                            AppBlockAccessibilityService.requestHealthRecovery("Foreground watchdog detected stale heartbeat")
                        ) {
                            delay(1_000L)
                            status = AccessibilityHealthMonitor.getStatus(applicationContext)
                        }

                        val shouldRunFallback = !status.isOperational
                        if (shouldRunFallback) {
                            nextDelayMs = FALLBACK_PROTECTION_POLL_MS
                            isFallbackProtectionActive.set(true)
                            AccessibilityHealthMonitor.recordFallbackProtectionState(applicationContext, true)

                            val now = System.currentTimeMillis()
                            if (now - lastReconcileAt >= FALLBACK_RECONCILE_INTERVAL_MS) {
                                lastReconcileAt = now
                                withContext(Dispatchers.IO) {
                                    repository.cleanupStaleSessions()
                                    repository.reconcileRestrictedAppsWithUsageStats()
                                }
                            }

                            val foregroundPackage = queryUsageStatsForegroundPackage()
                            runFallbackProtectionTick(
                                repository = repository,
                                activeAppsForToday = activeAppsForToday,
                                foregroundPackage = foregroundPackage
                            )

                            if (status.requiresReenable && !hasWarnedUntilHealthy) {
                                Log.w(TAG, "Accessibility tracking heartbeat is stale; re-enable required")
                                withContext(Dispatchers.IO) {
                                    repository.insertLog(
                                        eventType = "ACCESSIBILITY_HEALTH_WARNING",
                                        appName = "",
                                        details = getString(R.string.accessibility_health_log_reenable)
                                    )
                                }
                                AccessibilityHealthMonitor.maybeNotifyReenableRequired(applicationContext)
                                hasWarnedUntilHealthy = true
                            }
                        } else {
                            isFallbackProtectionActive.set(false)
                            AccessibilityHealthMonitor.recordFallbackProtectionState(applicationContext, false)
                            closeFallbackTrackedSession(repository, "Erişilebilirlik motoru tekrar sağlıklı")
                            withContext(Dispatchers.IO) {
                                repository.cleanupStaleSessions()
                                repository.reconcileRestrictedAppsWithUsageStats()
                            }
                            hasWarnedUntilHealthy = false
                        }
                    } else {
                        isFallbackProtectionActive.set(false)
                        AccessibilityHealthMonitor.recordFallbackProtectionState(applicationContext, false)
                        closeFallbackTrackedSession(repository, "Aktif kısıtlama yok")
                        hasWarnedUntilHealthy = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Tracking health watchdog failed: ${e.message}", e)
                }

                delay(nextDelayMs)
            }
        }
    }

    private suspend fun runFallbackProtectionTick(
        repository: GuardianRepository,
        activeAppsForToday: List<RestrictedAppEntity>,
        foregroundPackage: String?
    ) {
        if (!isDeviceInteractiveAndUnlocked() || foregroundPackage.isNullOrBlank()) {
            closeFallbackTrackedSession(repository, "Yedek motor on plani dogrulayamadi")
            if (isLockOverlayVisible.get()) {
                requestHideLockOverlay("Yedek motor on plani dogrulayamadi")
            }
            return
        }

        val matchingApp = activeAppsForToday.firstOrNull { it.packageName == foregroundPackage }
        if (matchingApp == null) {
            closeFallbackTrackedSession(repository, "Yedek motor hedef uygulamadan cikis algiladi")
            if (
                isLockOverlayVisible.get() &&
                visibleOverlayPackage != null &&
                visibleOverlayPackage != foregroundPackage
            ) {
                requestHideLockOverlay("Yedek motor hedef uygulamadan cikis algiladi")
            }
            return
        }

        val app = withContext(Dispatchers.IO) {
            repository.getRestrictedAppByIdSync(matchingApp.id)
        } ?: matchingApp

        if (app.remainingSecondsToday <= 0 || app.isFailed) {
            fallbackTrackedPackage = app.packageName
            fallbackTrackedAppId = app.id
            withContext(Dispatchers.IO) {
                repository.closeActiveSession("Yedek motor limit doldugu icin kilitledi")
            }
            showLockOverlay(applicationContext, app.appName, app.packageName)
            return
        }

        if (fallbackTrackedPackage != app.packageName) {
            closeFallbackTrackedSession(repository, "Yedek motor yeni hedefe gecti")
            fallbackTrackedPackage = app.packageName
            fallbackTrackedAppId = app.id
            withContext(Dispatchers.IO) {
                repository.startSession(app)
            }
        } else {
            withContext(Dispatchers.IO) {
                repository.updateSessionLastSeen(app.packageName)
            }
        }

        val updatedApp = withContext(Dispatchers.IO) {
            repository.getRestrictedAppByIdSync(app.id)
        } ?: app

        if (updatedApp.remainingSecondsToday <= 0 || updatedApp.isFailed) {
            withContext(Dispatchers.IO) {
                repository.closeActiveSession("Yedek motor kullanim hakkini tuketti")
            }
            showLockOverlay(applicationContext, updatedApp.appName, updatedApp.packageName)
        } else if (isLockOverlayFor(updatedApp.packageName)) {
            // Kilitli uygulamanın hakkı yeniden doğdu (günlük sıfırlama veya limit
            // düzenlemesi): yapışkan kilidi burada koşulsuz kaldırmak gerekir.
            forceHideLockOverlay("Yedek motor: ${updatedApp.appName} icin kullanim hakki yeniden dogdu")
        }
    }

    private suspend fun closeFallbackTrackedSession(
        repository: GuardianRepository,
        reason: String
    ) {
        if (fallbackTrackedPackage == null) return
        withContext(Dispatchers.IO) {
            repository.closeActiveSession(reason)
        }
        fallbackTrackedPackage = null
        fallbackTrackedAppId = -1L
    }

    private fun isDeviceInteractiveAndUnlocked(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isScreenOn = powerManager?.isInteractive ?: true
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        return isScreenOn && !isLocked
    }

    private fun queryUsageStatsForegroundPackage(): String? {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null
            val endTime = System.currentTimeMillis()
            val startTime = endTime - FALLBACK_FOREGROUND_LOOKBACK_MS
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

            UsageStatsForegroundResolver.resolveForegroundPackage(records)
        } catch (e: SecurityException) {
            Log.w(TAG, "UsageStats permission missing for fallback protection: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Fallback foreground query failed: ${e.message}", e)
            null
        }
    }

    /**
     * Hedef uygulamanın ÜZERİNE WindowManager.addView ile gerçek bir View çiz.
     * Bu yöntem Android 11+ background activity start kısıtlamasından etkilenmez.
     */
    private fun addLockOverlayView(targetAppName: String, targetAppPackage: String) {
        if (isLockOverlayVisible.get() && lockOverlayView != null) {
            if (visibleOverlayPackage != targetAppPackage) {
                mainHandler.post {
                    removeLockOverlayViewInternal()
                    addLockOverlayViewInternal(targetAppName, targetAppPackage)
                }
                return
            }
            forceOverlayToFrontInternal()
            return
        }
        if (lockOverlayView != null) return

        mainHandler.post {
            addLockOverlayViewInternal(targetAppName, targetAppPackage)
        }
    }

    private fun addLockOverlayViewInternal(targetAppName: String, targetAppPackage: String) {
        if (isLockOverlayVisible.get() || lockOverlayView != null) {
            if (visibleOverlayPackage == targetAppPackage) {
                forceOverlayToFrontInternal()
                return
            }
            removeLockOverlayViewInternal()
        }

        val wm = windowManager ?: run {
            Log.e(TAG, "WindowManager null, cannot add overlay")
            return
        }

        try {
            @SuppressLint("InflateParams")
            val overlayView = LayoutInflater.from(this).inflate(R.layout.lock_overlay, null)
            val targetText = overlayView.findViewById<TextView>(R.id.targetAppText)
            val emojiText = overlayView.findViewById<TextView>(R.id.emojiText)
            val emojiImage = overlayView.findViewById<ImageView>(R.id.emojiImage)

            targetText.text = targetAppName.ifEmpty { getString(R.string.overlay_restricted_app) }

            // Emoji / Uygulama İkonu: Önce harici URI dene, yoksa kısıtlı uygulamanın kendi ikonunu yükle
            val uri = customEmojiUri
            if (!uri.isNullOrEmpty()) {
                tryLoadEmojiFromUri(uri, emojiImage, emojiText)
            } else {
                try {
                    val appIconDrawable = packageManager.getApplicationIcon(targetAppPackage)
                    emojiImage.setImageDrawable(appIconDrawable)
                    emojiImage.visibility = View.VISIBLE
                    emojiText.visibility = View.GONE
                } catch (e: Exception) {
                    emojiText.text = defaultEmojiText
                    emojiText.visibility = View.VISIBLE
                    emojiImage.visibility = View.GONE
                }
            }

            // --- Söz Seçme ve Ayarlama Logic'i ---
            val quoteText = overlayView.findViewById<TextView>(R.id.quoteText)
            val quoteAuthorText = overlayView.findViewById<TextView>(R.id.quoteAuthorText)

            // Yazı Tipi (Font) Ayarlamaları
            try {
                quoteText?.typeface = Typeface.SERIF
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set serif typeface for quoteText: ${e.message}")
            }

            try {
                val typefaceAuthor = ResourcesCompat.getFont(this, R.font.finytaels)
                if (typefaceAuthor != null) {
                    quoteAuthorText?.typeface = typefaceAuthor
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load custom author font: ${e.message}")
            }

            val prefs = getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE)
            val quotesJson = prefs.getString("custom_quotes_json", "[]") ?: "[]"
            val showOnlyMyQuotes = prefs.getBoolean("show_only_my_quotes", false)

            class CustomQuoteLocal(val text: String, val author: String, val isSelected: Boolean)
            val customQuotes = mutableListOf<CustomQuoteLocal>()
            try {
                val array = org.json.JSONArray(quotesJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val text = obj.optString("text", "")
                    val author = obj.optString("author", "")
                    val isSelected = obj.optBoolean("isSelected", true)
                    if (text.isNotEmpty()) {
                        customQuotes.add(CustomQuoteLocal(text, author, isSelected))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse custom quotes JSON: ${e.message}")
            }

            var selectedCustomQuotes = customQuotes.filter { it.isSelected }
            if (selectedCustomQuotes.isEmpty() && customQuotes.isNotEmpty()) {
                selectedCustomQuotes = customQuotes
            }

            val calendar = java.util.Calendar.getInstance()
            val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
            val year = calendar.get(java.util.Calendar.YEAR)
            val seed = year * 365 + dayOfYear

            var finalQuoteText = ""
            var finalQuoteAuthor = ""

            if (showOnlyMyQuotes && selectedCustomQuotes.isNotEmpty()) {
                val index = seed % selectedCustomQuotes.size
                val q = selectedCustomQuotes[index]
                finalQuoteText = q.text
                finalQuoteAuthor = q.author
            } else {
                if (selectedCustomQuotes.isNotEmpty()) {
                    val totalQuotes = DefaultQuotes.list.size + selectedCustomQuotes.size
                    val selectedIndex = seed % totalQuotes
                    if (selectedIndex < DefaultQuotes.list.size) {
                        val pair = DefaultQuotes.list.getOrNull(selectedIndex)
                        finalQuoteText = if (pair != null && pair.first != 0) getString(pair.first) else ""
                        finalQuoteAuthor = if (pair != null && pair.second != 0) getString(pair.second) else ""
                    } else {
                        val customIndex = selectedIndex - DefaultQuotes.list.size
                        val q = selectedCustomQuotes[customIndex]
                        finalQuoteText = q.text
                        finalQuoteAuthor = q.author
                    }
                } else {
                    val index = seed % DefaultQuotes.list.size
                    val pair = DefaultQuotes.list.getOrNull(index)
                    finalQuoteText = if (pair != null && pair.first != 0) getString(pair.first) else ""
                    finalQuoteAuthor = if (pair != null && pair.second != 0) getString(pair.second) else ""
                }
            }

            quoteText?.text = finalQuoteText
            quoteAuthorText?.text = if (finalQuoteAuthor.isNotEmpty()) "- $finalQuoteAuthor" else ""

            // --- Programatik Tema Renklendirme ---
            // Kilit ekranı, sistem gece moduna değil uygulamanın kendi tema ayarına uymalıdır.
            val isDark = resolveAppIsDarkTheme()

            val rootLayout = overlayView.findViewById<View>(R.id.rootLayout)
            rootLayout?.setBackgroundColor(android.graphics.Color.parseColor(if (isDark) "#0B0F19" else "#F1F5F9"))

            val cardView = overlayView.findViewById<View>(R.id.cardLayout)
            if (cardView != null) {
                val scale = resources.displayMetrics.density
                val cardDrawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 24f * scale
                    setColor(android.graphics.Color.parseColor(if (isDark) "#151D30" else "#FFFFFF"))
                    setStroke((1f * scale).toInt(), android.graphics.Color.parseColor(if (isDark) "#24324D" else "#CBD5E1"))
                }
                cardView.background = cardDrawable
            }

            val appTitleText = overlayView.findViewById<TextView>(R.id.appTitleText)
            val limitOverText = overlayView.findViewById<TextView>(R.id.limitOverText)
            val returnToHomeText = overlayView.findViewById<TextView>(R.id.returnToHomeText)

            appTitleText?.setTextColor(android.graphics.Color.parseColor(if (isDark) "#2EC4B6" else "#0D9488"))
            targetText?.setTextColor(android.graphics.Color.parseColor(if (isDark) "#FFFFFF" else "#0F172A"))
            limitOverText?.setTextColor(android.graphics.Color.parseColor(if (isDark) "#EF4444" else "#DC2626"))
            quoteText?.setTextColor(android.graphics.Color.parseColor(if (isDark) "#E2E8F0" else "#334155"))
            quoteAuthorText?.setTextColor(android.graphics.Color.parseColor(if (isDark) "#94A3B8" else "#475569"))
            returnToHomeText?.setTextColor(android.graphics.Color.parseColor(if (isDark) "#94A3B8" else "#475569"))

            setupReturnHomeButton(overlayView, isDark)
            setupOverlayInputGuards(overlayView)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            wm.addView(overlayView, params)
            lockOverlayView = overlayView
            visibleOverlayPackage = targetAppPackage
            isLockOverlayVisible.set(true)
            lastStickyTarget = targetAppName to targetAppPackage

            Log.i(TAG, "Lock overlay added for $targetAppName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add lock overlay: ${e.message}", e)
        }
    }

    /**
     * Kilit ekranının TEK meşru çıkış yolu olan "Ana sayfaya dön" butonunu hazırlar.
     */
    private fun setupReturnHomeButton(overlayView: View, isDark: Boolean) {
        val button = overlayView.findViewById<TextView>(R.id.returnHomeButton) ?: return
        val scale = resources.displayMetrics.density

        button.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 14f * scale
            setColor(android.graphics.Color.parseColor(if (isDark) "#2EC4B6" else "#0D9488"))
        }
        button.setTextColor(android.graphics.Color.parseColor(if (isDark) "#04211E" else "#FFFFFF"))

        button.setOnClickListener {
            if (returnHomeInProgress.getAndSet(true)) return@setOnClickListener
            val dismissedPackage = visibleOverlayPackage
            Log.i(TAG, "Return-home button pressed for $dismissedPackage")
            val wentHome = navigateToHomeScreen()

            // Önce ana ekrana git, kilidi kısa bir gecikmeyle kaldır. Aksi hâlde
            // kilitli uygulama bir kare boyunca görünür kalırdı.
            mainHandler.postDelayed({
                forceHideLockOverlay("Kullanici 'Ana sayfaya don' butonuna basti")
                returnHomeInProgress.set(false)
            }, RETURN_HOME_DISMISS_DELAY_MS)

            serviceScope.launch {
                try {
                    val db = GuardianDatabase.getDatabase(applicationContext)
                    val repository = GuardianRepository(applicationContext, db.guardianDao())
                    repository.insertLog(
                        eventType = "OVERLAY_DISMISSED_BY_USER",
                        appName = dismissedPackage ?: "",
                        details = "Kilit ekrani 'Ana sayfaya don' butonuyla kaldirildi. " +
                            "Ana ekrana yonlendirme: ${if (wentHome) "basarili" else "basarisiz"}."
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to log overlay dismissal: ${e.message}")
                }
            }
        }
    }

    /**
     * Kullanıcıyı ana ekrana gönderir.
     *
     * Öncelik erişilebilirlik servisindedir (arka plan activity başlatma
     * kısıtlarından etkilenmez). Servis bağlı değilse HOME intent'ine düşer;
     * SYSTEM_ALERT_WINDOW izni olduğu için bu yol da çalışır.
     */
    private fun navigateToHomeScreen(): Boolean {
        try {
            val a11y = AppBlockAccessibilityService.instance
            if (a11y != null && a11y.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)) {
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "GLOBAL_ACTION_HOME failed: ${e.message}")
        }

        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(homeIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Home intent fallback failed: ${e.message}")
            false
        }
    }

    /**
     * Kilit ekranının altındaki uygulamaya hiçbir girdinin sızmamasını sağlar
     * ve BACK tuşunu tüketir. HOME/RECENTS tuşları platform gereği tüketilemez;
     * o durumda yapışkan kilit devrede kalarak koruma sağlar.
     */
    private fun setupOverlayInputGuards(overlayView: View) {
        val root = overlayView.findViewById<View>(R.id.rootLayout) ?: overlayView
        root.isClickable = true
        root.isFocusable = true
        root.isFocusableInTouchMode = true
        @Suppress("ClickableViewAccessibility")
        root.setOnTouchListener { _, _ -> true }
        root.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                Log.d(TAG, "BACK key consumed by lock overlay")
                true
            } else {
                false
            }
        }
        root.requestFocus()
    }

    /**
     * Harici URI'den ImageView'a resim yüklemeyi dene. Başarısız olursa
     * TextView emoji fallback'ine düş.
     */
    private fun tryLoadEmojiFromUri(uri: String, emojiImage: ImageView, emojiText: TextView) {
        serviceScope.launch {
            try {
                val parsed = Uri.parse(uri)
                val bitmap = withContext(Dispatchers.IO) {
                    val input: InputStream? = contentResolver.openInputStream(parsed)
                    input?.use { BitmapFactory.decodeStream(it) }
                }
                if (bitmap != null) {
                    mainHandler.post {
                        emojiImage.setImageBitmap(bitmap)
                        emojiImage.visibility = View.VISIBLE
                        emojiText.visibility = View.GONE
                    }
                } else {
                    Log.w(TAG, "Failed to decode emoji URI bitmap, fallback to text")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load emoji from URI: ${e.message}")
            }
        }
    }

    private fun forceOverlayToFrontInternal() {
        mainHandler.post {
            val view = lockOverlayView
            val wm = windowManager
            if (view != null && wm != null && isLockOverlayVisible.get()) {
                try {
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        overlayWindowType(),
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.START
                    }
                    wm.updateViewLayout(view, params)
                    Log.d(TAG, "forceOverlayToFrontInternal: overlay layout updated to top most")
                } catch (e: Exception) {
                    Log.w(TAG, "forceOverlayToFrontInternal failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Overlay'i pencere yöneticisinden kaldır.
     */
    private fun removeLockOverlayView() {
        if (lockOverlayView == null && !isLockOverlayVisible.get()) return
        mainHandler.post {
            removeLockOverlayViewInternal()
        }
    }

    private fun removeLockOverlayViewInternal() {
        cycleJob?.cancel()
        cycleJob = null

        // Yapışkan mod kapatıldıysa watchdog'un kilidi geri getirmesi istenmez.
        if (!requiresManualDismiss.get()) {
            lastStickyTarget = null
        }

        val view = lockOverlayView
        val wm = windowManager
        if (view == null || wm == null) {
            lockOverlayView = null
            visibleOverlayPackage = null
            isLockOverlayVisible.set(false)
            return
        }

        if (!isLockOverlayVisible.get()) {
            lockOverlayView = null
            visibleOverlayPackage = null
            return
        }

        try {
            wm.removeView(view)
            Log.i(TAG, "Lock overlay removed")
        } catch (e: Exception) {
            Log.w(TAG, "removeView failed (already detached?): ${e.message}")
        } finally {
            lockOverlayView = null
            visibleOverlayPackage = null
            isLockOverlayVisible.set(false)
        }
    }



    /**
     * Kilit ekranının açık/koyu renklenmesini, uygulamanın kendi tema ayarına göre belirler.
     * Theme.kt içindeki MyApplicationTheme ile aynı kuralları izler:
     * - PREMIUM_DARK paleti her iki modda da koyu kalır.
     * - LIGHT/DARK seçimi doğrudan uygulanır, SYSTEM ise cihaz gece moduna bakar.
     */
    private fun resolveAppIsDarkTheme(): Boolean {
        val prefs = getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE)
        val mode = prefs.getString("theme_mode", "LIGHT") ?: "LIGHT"
        val palette = prefs.getString("theme_palette", "BLUE") ?: "BLUE"
        if (palette == "PREMIUM_DARK") return true
        return when (mode) {
            "LIGHT" -> false
            "DARK" -> true
            else -> (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    /**
     * UI thread'ine güvenli geçiş (service bir background thread'den çağrılabilir).
     */
    private fun runOnUiThreadSafe(action: () -> Unit) {
        try {
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post(action)
        } catch (e: Exception) {
            action()
        }
    }
}

object DefaultQuotes {
    val list = listOf(
        R.string.quote_text_1 to R.string.quote_author_1,
        R.string.quote_text_2 to R.string.quote_author_2,
        R.string.quote_text_3 to R.string.quote_author_3,
        R.string.quote_text_4 to R.string.quote_author_4,
        R.string.quote_text_5 to R.string.quote_author_5,
        R.string.quote_text_6 to R.string.quote_author_6,
        R.string.quote_text_7 to R.string.quote_author_7,
        R.string.quote_text_8 to R.string.quote_author_8,
        R.string.quote_text_9 to R.string.quote_author_9,
        R.string.quote_text_10 to R.string.quote_author_10,
        R.string.quote_text_11 to R.string.quote_author_11,
        R.string.quote_text_12 to R.string.quote_author_12,
        R.string.quote_text_13 to R.string.quote_author_13,
        R.string.quote_text_14 to R.string.quote_author_14,
        R.string.quote_text_15 to R.string.quote_author_15,
        R.string.quote_text_16 to R.string.quote_author_16,
        R.string.quote_text_17 to R.string.quote_author_17,
        R.string.quote_text_18 to R.string.quote_author_18,
        R.string.quote_text_19 to R.string.quote_author_19,
        R.string.quote_text_20 to R.string.quote_author_20,
        R.string.quote_text_21 to R.string.quote_author_21,
        R.string.quote_text_22 to R.string.quote_author_22,
        R.string.quote_text_23 to R.string.quote_author_23,
        R.string.quote_text_24 to R.string.quote_author_24,
        R.string.quote_text_25 to R.string.quote_author_25,
        R.string.quote_text_26 to R.string.quote_author_26,
        R.string.quote_text_27 to R.string.quote_author_27,
        R.string.quote_text_28 to R.string.quote_author_28,
        R.string.quote_text_29 to R.string.quote_author_29,
        R.string.quote_text_30 to R.string.quote_author_30,
        R.string.quote_text_31 to R.string.quote_author_31,
        R.string.quote_text_32 to R.string.quote_author_32,
        R.string.quote_text_33 to R.string.quote_author_33,
        R.string.quote_text_34 to R.string.quote_author_34,
        R.string.quote_text_35 to R.string.quote_author_35,
        R.string.quote_text_36 to R.string.quote_author_36,
        R.string.quote_text_37 to R.string.quote_author_37,
        R.string.quote_text_38 to R.string.quote_author_38,
        R.string.quote_text_39 to R.string.quote_author_39,
        R.string.quote_text_40 to R.string.quote_author_40,
        R.string.quote_text_41 to R.string.quote_author_41,
        R.string.quote_text_42 to R.string.quote_author_42,
        R.string.quote_text_43 to R.string.quote_author_43,
        R.string.quote_text_44 to R.string.quote_author_44,
        R.string.quote_text_45 to R.string.quote_author_45,
        R.string.quote_text_46 to R.string.quote_author_46,
        R.string.quote_text_47 to R.string.quote_author_47,
        R.string.quote_text_48 to R.string.quote_author_48,
        R.string.quote_text_49 to R.string.quote_author_49,
        R.string.quote_text_50 to R.string.quote_author_50,
        R.string.quote_text_51 to R.string.quote_author_51,
        R.string.quote_text_52 to R.string.quote_author_52,
        R.string.quote_text_53 to R.string.quote_author_53,
        R.string.quote_text_54 to R.string.quote_author_54
    )
}

data class UsageEventRecord(
    val packageName: String,
    val eventType: Int,
    val timestampMillis: Long
)

object UsageStatsForegroundResolver {
    const val TYPE_ACTIVITY_RESUMED = 1
    const val TYPE_ACTIVITY_PAUSED = 2
    const val TYPE_ACTIVITY_STOPPED = 23
    const val TYPE_MOVE_TO_FOREGROUND = 1
    const val TYPE_MOVE_TO_BACKGROUND = 2

    fun isResumeEvent(eventType: Int): Boolean {
        return eventType == TYPE_ACTIVITY_RESUMED || eventType == TYPE_MOVE_TO_FOREGROUND
    }

    fun isPauseOrStopEvent(eventType: Int): Boolean {
        return eventType == TYPE_ACTIVITY_PAUSED ||
               eventType == TYPE_ACTIVITY_STOPPED ||
               eventType == TYPE_MOVE_TO_BACKGROUND
    }

    fun resolveForegroundPackage(events: List<UsageEventRecord>): String? {
        data class PackageLifecycleState(val isForeground: Boolean, val timestamp: Long)
        val packageStates = mutableMapOf<String, PackageLifecycleState>()

        for (event in events) {
            val pkg = event.packageName
            if (pkg.isBlank()) continue

            if (isResumeEvent(event.eventType)) {
                packageStates[pkg] = PackageLifecycleState(isForeground = true, timestamp = event.timestampMillis)
            } else if (isPauseOrStopEvent(event.eventType)) {
                packageStates[pkg] = PackageLifecycleState(isForeground = false, timestamp = event.timestampMillis)
            }
        }

        return packageStates.entries
            .filter { it.value.isForeground }
            .maxByOrNull { it.value.timestamp }
            ?.key
    }
}


/**
 * Kilit ekranının kaldırılma kurallarını taşıyan saf (Android'siz) politika nesnesi.
 *
 * Kural: kilit ekranı gösterildiği anda "yapışkan" olur. Ön plan değişiminden
 * doğan yumuşak kaldırma istekleri (kullanıcının alttan yukarı çekip ana ekrana
 * veya başka bir uygulamaya geçmesi dahil) yok sayılır. Kilit yalnızca meşru
 * çıkış yollarıyla kalkar; bkz. [BlockOverlayService.forceHideLockOverlay].
 */
object OverlayDismissPolicy {

    /** Ön plan kaynaklı yumuşak kaldırma isteği yok sayılmalı mı? */
    fun shouldIgnoreSoftHide(requiresManualDismiss: Boolean): Boolean = requiresManualDismiss

    /** Verilen istek kilidi gerçekten kaldırır mı? */
    fun willDismiss(requiresManualDismiss: Boolean, isForced: Boolean): Boolean {
        if (isForced) return true
        return !shouldIgnoreSoftHide(requiresManualDismiss)
    }
}

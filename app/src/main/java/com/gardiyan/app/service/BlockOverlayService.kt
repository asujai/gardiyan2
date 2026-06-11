package com.gardiyan.app.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
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
import com.gardiyan.app.data.repository.GuardianRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
 * 5. Kilit ekranından çıkış YOK; sadece Limitra MainActivity'den 5sn basılı
 *    tutarak iptal edilebilir
 *
 * KRİTİK: Sayaç 0'a ulaştığında:
 * - isActive = false KULLANILMAZ
 * - Overlay kapanmaz
 * - Sayaç anında 10'a resetlenir
 * - Döngü sonsuza kadar devam eder
 *
 * İptal akışı:
 * - ViewModel.cancelAllWithFiveSecondHold() çağrılır
 * - hideLockOverlay() çağrılarak overlay kapatılır
 */
class BlockOverlayService : Service() {

    companion object {
        private const val TAG = "BlockOverlayService"
        private const val ALARM_RESTART_REQUEST_CODE = 300
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
         * A11y service hedef uygulamadan çıkışı tespit ettiğinde çağırır.
         * Veya ViewModel 5sn basılı tutma sonrası iptal için çağırır.
         */
        @JvmStatic
        fun hideLockOverlay() {
            Log.i(TAG, "hideLockOverlay requested")
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning.set(true)
        serviceInstance = WeakReference(this)
        appContextRef = WeakReference(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForegroundCompat()
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
        serviceInstance = null
        appContextRef = null
        visibleOverlayPackage = null
        cycleJob?.cancel()
        cycleJob = null
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmService.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 1000L,
                    pendingIntent
                )
            } else {
                alarmService.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 1000L,
                    pendingIntent
                )
            }
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
            val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

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

            Log.i(TAG, "Lock overlay added for $targetAppName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add lock overlay: ${e.message}", e)
        }
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

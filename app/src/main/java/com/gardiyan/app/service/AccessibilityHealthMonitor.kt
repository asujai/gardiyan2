package com.gardiyan.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.gardiyan.app.MainActivity
import com.gardiyan.app.R

object AccessibilityHealthMonitor {
    const val HEARTBEAT_INTERVAL_MS = 5_000L
    const val HEARTBEAT_STALE_MS = 30_000L

    private const val PREFS_NAME = "gardiyan_accessibility_health"
    private const val KEY_SERVICE_BOUND = "service_bound"
    private const val KEY_SERVICE_HEARTBEAT_WALL = "service_heartbeat_wall"
    private const val KEY_SERVICE_HEARTBEAT_ELAPSED = "service_heartbeat_elapsed"
    private const val KEY_TRACKING_HEARTBEAT_WALL = "tracking_heartbeat_wall"
    private const val KEY_TRACKING_HEARTBEAT_ELAPSED = "tracking_heartbeat_elapsed"
    private const val KEY_FALLBACK_PROTECTION_ACTIVE = "fallback_protection_active"
    private const val KEY_FALLBACK_PROTECTION_HEARTBEAT_WALL = "fallback_protection_heartbeat_wall"
    private const val KEY_FALLBACK_PROTECTION_HEARTBEAT_ELAPSED = "fallback_protection_heartbeat_elapsed"
    private const val KEY_LAST_STARTED_WALL = "last_started_wall"
    private const val KEY_LAST_STOPPED_WALL = "last_stopped_wall"
    private const val KEY_LAST_WARNING_WALL = "last_warning_wall"
    private const val HEALTH_ALERT_CHANNEL_ID = "limitra_health_alerts_channel"
    private const val WARNING_THROTTLE_MS = 5 * 60 * 1000L
    private const val WARNING_NOTIFICATION_ID = 103

    data class Status(
        val isPermissionGranted: Boolean,
        val isServiceHeartbeatFresh: Boolean,
        val isTrackingHeartbeatFresh: Boolean,
        val isFallbackProtectionActive: Boolean,
        val isFallbackProtectionFresh: Boolean,
        val serviceHeartbeatAgeMillis: Long?,
        val trackingHeartbeatAgeMillis: Long?,
        val fallbackProtectionHeartbeatAgeMillis: Long?,
        val isServiceMarkedBound: Boolean
    ) {
        val isOperational: Boolean
            get() = isPermissionGranted && isServiceHeartbeatFresh && isTrackingHeartbeatFresh

        val requiresReenable: Boolean
            get() = isPermissionGranted && !isOperational

        val hasFailSafeProtection: Boolean
            get() = isFallbackProtectionActive && isFallbackProtectionFresh
    }

    fun recordServiceStarted(context: Context) {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        context.healthPrefs().edit()
            .putBoolean(KEY_SERVICE_BOUND, true)
            .putLong(KEY_LAST_STARTED_WALL, nowWall)
            .putLong(KEY_SERVICE_HEARTBEAT_WALL, nowWall)
            .putLong(KEY_SERVICE_HEARTBEAT_ELAPSED, nowElapsed)
            .putLong(KEY_TRACKING_HEARTBEAT_WALL, nowWall)
            .putLong(KEY_TRACKING_HEARTBEAT_ELAPSED, nowElapsed)
            .apply()
    }

    fun recordServiceHeartbeat(context: Context) {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        context.healthPrefs().edit()
            .putBoolean(KEY_SERVICE_BOUND, true)
            .putLong(KEY_SERVICE_HEARTBEAT_WALL, nowWall)
            .putLong(KEY_SERVICE_HEARTBEAT_ELAPSED, nowElapsed)
            .apply()
    }

    fun recordTrackingHeartbeat(context: Context) {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        context.healthPrefs().edit()
            .putBoolean(KEY_SERVICE_BOUND, true)
            .putLong(KEY_TRACKING_HEARTBEAT_WALL, nowWall)
            .putLong(KEY_TRACKING_HEARTBEAT_ELAPSED, nowElapsed)
            .apply()
    }

    fun recordFallbackProtectionState(context: Context, isActive: Boolean) {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        context.healthPrefs().edit()
            .putBoolean(KEY_FALLBACK_PROTECTION_ACTIVE, isActive)
            .putLong(KEY_FALLBACK_PROTECTION_HEARTBEAT_WALL, nowWall)
            .putLong(KEY_FALLBACK_PROTECTION_HEARTBEAT_ELAPSED, nowElapsed)
            .apply()
    }

    fun recordServiceStopped(context: Context) {
        context.healthPrefs().edit()
            .putBoolean(KEY_SERVICE_BOUND, false)
            .putBoolean(KEY_FALLBACK_PROTECTION_ACTIVE, false)
            .putLong(KEY_LAST_STOPPED_WALL, System.currentTimeMillis())
            .apply()
    }

    fun getStatus(context: Context): Status {
        val prefs = context.healthPrefs()
        val permissionGranted = isAccessibilityPermissionGranted(context)
        val serviceBound = prefs.getBoolean(KEY_SERVICE_BOUND, false) || AppBlockAccessibilityService.isRunning

        val serviceAge = heartbeatAgeMillis(
            nowWallMillis = System.currentTimeMillis(),
            nowElapsedRealtime = SystemClock.elapsedRealtime(),
            lastWallMillis = prefs.getLong(KEY_SERVICE_HEARTBEAT_WALL, 0L),
            lastElapsedRealtime = prefs.getLong(KEY_SERVICE_HEARTBEAT_ELAPSED, 0L)
        )
        val trackingAge = heartbeatAgeMillis(
            nowWallMillis = System.currentTimeMillis(),
            nowElapsedRealtime = SystemClock.elapsedRealtime(),
            lastWallMillis = prefs.getLong(KEY_TRACKING_HEARTBEAT_WALL, 0L),
            lastElapsedRealtime = prefs.getLong(KEY_TRACKING_HEARTBEAT_ELAPSED, 0L)
        )
        val fallbackProtectionActive =
            prefs.getBoolean(KEY_FALLBACK_PROTECTION_ACTIVE, false) ||
                BlockOverlayService.isFallbackProtectionActive.get()
        val fallbackProtectionAge = heartbeatAgeMillis(
            nowWallMillis = System.currentTimeMillis(),
            nowElapsedRealtime = SystemClock.elapsedRealtime(),
            lastWallMillis = prefs.getLong(KEY_FALLBACK_PROTECTION_HEARTBEAT_WALL, 0L),
            lastElapsedRealtime = prefs.getLong(KEY_FALLBACK_PROTECTION_HEARTBEAT_ELAPSED, 0L)
        )

        val inMemoryHealthy = AppBlockAccessibilityService.isHealthy()
        val serviceFresh = serviceBound && (inMemoryHealthy || isFreshHeartbeatAge(serviceAge, HEARTBEAT_STALE_MS))
        val trackingFresh = isFreshHeartbeatAge(trackingAge, HEARTBEAT_STALE_MS)
        val fallbackProtectionFresh = fallbackProtectionActive &&
            isFreshHeartbeatAge(fallbackProtectionAge, HEARTBEAT_STALE_MS)

        return Status(
            isPermissionGranted = permissionGranted,
            isServiceHeartbeatFresh = serviceFresh,
            isTrackingHeartbeatFresh = trackingFresh,
            isFallbackProtectionActive = fallbackProtectionActive,
            isFallbackProtectionFresh = fallbackProtectionFresh,
            serviceHeartbeatAgeMillis = serviceAge,
            trackingHeartbeatAgeMillis = trackingAge,
            fallbackProtectionHeartbeatAgeMillis = fallbackProtectionAge,
            isServiceMarkedBound = serviceBound
        )
    }

    fun isAccessibilityPermissionGranted(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return isComponentEnabled(
            enabledServices,
            context.packageName,
            AppBlockAccessibilityService::class.java.name
        )
    }

    fun maybeNotifyReenableRequired(context: Context) {
        val prefs = context.healthPrefs()
        val now = System.currentTimeMillis()
        val lastWarning = prefs.getLong(KEY_LAST_WARNING_WALL, 0L)
        if (now - lastWarning < WARNING_THROTTLE_MS) return
        if (!context.getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE)
                .getBoolean("notifications_enabled", true)
        ) {
            return
        }

        prefs.edit().putLong(KEY_LAST_WARNING_WALL, now).apply()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = ensureHealthAlertChannel(context, nm)

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val settingsPendingIntent = PendingIntent.getActivity(
            context,
            1,
            accessibilitySettingsIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.accessibility_health_notification_title))
            .setContentText(context.getString(R.string.accessibility_health_notification_desc))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.accessibility_health_notification_desc))
            )
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentIntent(appPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_manage,
                context.getString(R.string.perm_state_reenable),
                settingsPendingIntent
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(WARNING_NOTIFICATION_ID, notification)
    }

    private fun ensureHealthAlertChannel(
        context: Context,
        notificationManager: NotificationManager
    ): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                HEALTH_ALERT_CHANNEL_ID,
                context.getString(R.string.accessibility_health_notification_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.accessibility_health_notification_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }
        return HEALTH_ALERT_CHANNEL_ID
    }

    private fun accessibilitySettingsIntent(context: Context): Intent {
        val componentName = ComponentName(
            context,
            AppBlockAccessibilityService::class.java
        ).flattenToString()
        val args = Bundle().apply {
            putString(":settings:fragment_args_key", componentName)
        }
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(":settings:fragment_args_key", componentName)
            putExtra(":settings:show_fragment_args", args)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    internal fun isComponentEnabled(
        enabledServicesSetting: String?,
        packageName: String,
        className: String
    ): Boolean {
        if (enabledServicesSetting.isNullOrBlank()) return false
        val expectedFull = "$packageName/$className"
        val shortClass = if (className.startsWith("$packageName.")) {
            className.substring(packageName.length)
        } else {
            ".$className"
        }
        val expectedShort = "$packageName/$shortClass"
        return enabledServicesSetting.split(':')
            .map { it.trim() }
            .any { it.equals(expectedFull, ignoreCase = true) || it.equals(expectedShort, ignoreCase = true) }
    }

    internal fun heartbeatAgeMillis(
        nowWallMillis: Long,
        nowElapsedRealtime: Long,
        lastWallMillis: Long,
        lastElapsedRealtime: Long
    ): Long? {
        if (lastWallMillis <= 0L || lastElapsedRealtime <= 0L) return null
        return if (nowElapsedRealtime >= lastElapsedRealtime) {
            nowElapsedRealtime - lastElapsedRealtime
        } else {
            nowWallMillis - lastWallMillis
        }
    }

    internal fun isFreshHeartbeatAge(ageMillis: Long?, staleAfterMillis: Long): Boolean {
        return ageMillis != null && ageMillis in 0L..staleAfterMillis
    }

    private fun Context.healthPrefs() =
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

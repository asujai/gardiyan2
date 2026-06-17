package com.gardiyan.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
    private const val KEY_LAST_STARTED_WALL = "last_started_wall"
    private const val KEY_LAST_STOPPED_WALL = "last_stopped_wall"
    private const val KEY_LAST_WARNING_WALL = "last_warning_wall"
    private const val WARNING_THROTTLE_MS = 5 * 60 * 1000L
    private const val WARNING_NOTIFICATION_ID = 103

    data class Status(
        val isPermissionGranted: Boolean,
        val isServiceHeartbeatFresh: Boolean,
        val isTrackingHeartbeatFresh: Boolean,
        val serviceHeartbeatAgeMillis: Long?,
        val trackingHeartbeatAgeMillis: Long?,
        val isServiceMarkedBound: Boolean
    ) {
        val isOperational: Boolean
            get() = isPermissionGranted && isServiceHeartbeatFresh && isTrackingHeartbeatFresh

        val requiresReenable: Boolean
            get() = isPermissionGranted && !isOperational
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

    fun recordServiceStopped(context: Context) {
        context.healthPrefs().edit()
            .putBoolean(KEY_SERVICE_BOUND, false)
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

        val inMemoryHealthy = AppBlockAccessibilityService.isHealthy()
        val serviceFresh = serviceBound && (inMemoryHealthy || isFreshHeartbeatAge(serviceAge, HEARTBEAT_STALE_MS))
        val trackingFresh = isFreshHeartbeatAge(trackingAge, HEARTBEAT_STALE_MS)

        return Status(
            isPermissionGranted = permissionGranted,
            isServiceHeartbeatFresh = serviceFresh,
            isTrackingHeartbeatFresh = trackingFresh,
            serviceHeartbeatAgeMillis = serviceAge,
            trackingHeartbeatAgeMillis = trackingAge,
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BlockOverlayService.CHANNEL_ID,
                context.getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_service_desc)
            }
            nm.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BlockOverlayService.CHANNEL_ID)
            .setContentTitle(context.getString(R.string.accessibility_health_notification_title))
            .setContentText(context.getString(R.string.accessibility_health_notification_desc))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.accessibility_health_notification_desc))
            )
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(WARNING_NOTIFICATION_ID, notification)
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

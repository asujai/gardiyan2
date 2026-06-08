package com.gardiyan.app.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gardiyan.app.R
import com.gardiyan.app.data.local.database.GuardianDatabase
import com.gardiyan.app.data.repository.GuardianRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Foreground service ölse bile servisin yeniden başlatılmasını garanti eden
 * WorkManager yedek işçisi. WorkManager, OEM batarya optimizasyonlarına
 * (Xiaomi MIUI, Samsung, Huawei vb.) karşı Foreground Service'den daha
 * dayanıklıdır.
 */
class ServiceKeepAliveWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = GuardianDatabase.getDatabase(applicationContext)
            val repository = GuardianRepository(db.guardianDao())

            val hasActiveRestrictions = withContext(Dispatchers.IO) {
                repository.getActiveRestrictedAppsSync().isNotEmpty()
            }
            if (!hasActiveRestrictions) {
                return Result.success()
            }

            // Pil optimizasyon muafiyeti kontrolü
            val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val isIgnoringBattery = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                pm?.isIgnoringBatteryOptimizations(applicationContext.packageName) == true
            } else {
                true
            }

            if (!isIgnoringBattery) {
                withContext(Dispatchers.IO) {
                    repository.insertLog(
                        eventType = "BATTERY_OPTIMIZATION_WARN",
                        appName = "",
                        details = applicationContext.getString(R.string.keep_alive_notification_long)
                    )
                }
                sendBatteryWarningNotification(applicationContext)
            }

            if (!BlockOverlayService.isServiceRunning.get()) {
                BlockOverlayService.start(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendBatteryWarningNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                BlockOverlayService.CHANNEL_ID,
                context.getString(R.string.notification_channel_service),
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_service_desc)
            }
            nm.createNotificationChannel(channel)
        }

        val notificationIntent = android.content.Intent(context, com.gardiyan.app.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, notificationIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(context, BlockOverlayService.CHANNEL_ID)
            .setContentTitle(context.getString(R.string.keep_alive_notification_title))
            .setContentText(context.getString(R.string.keep_alive_notification_desc))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)

        nm.notify(102, builder.build())
    }
}

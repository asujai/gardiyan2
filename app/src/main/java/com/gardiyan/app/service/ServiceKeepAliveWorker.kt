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

            // restricted_apps tablosundaki aktif kayıt varlığını kontrol et
            val hasActiveRestrictions = withContext(Dispatchers.IO) {
                repository.getActiveRestrictedAppsSync().isNotEmpty()
            }

            if (hasActiveRestrictions) {
                if (!BlockOverlayService.isServiceRunning.get()) {
                    BlockOverlayService.start(applicationContext)
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendBatteryWarningNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
        
        val notificationIntent = android.content.Intent(context, com.gardiyan.app.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, notificationIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(context, "gardiyan_service_channel")
            .setContentTitle(context.getString(R.string.keep_alive_notification_title))
            .setContentText(context.getString(R.string.keep_alive_notification_desc))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)

        nm.notify(102, builder.build())
    }
}

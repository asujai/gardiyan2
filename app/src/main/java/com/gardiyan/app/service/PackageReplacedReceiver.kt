package com.gardiyan.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gardiyan.app.data.local.database.GuardianDatabase
import com.gardiyan.app.data.repository.GuardianRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Uygulama güncellendiğinde (yeni APK yüklendiğinde) arka planda koruma durumunu
 * güvenli şekilde yeniden başlatır. Kullanıcıyı istemeden ana ekrana getirmez.
 */
class PackageReplacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appContext = context.applicationContext
                    KeepAliveScheduler.schedule(appContext)
                    DailySuccessScheduler.schedule(appContext)

                    val db = GuardianDatabase.getDatabase(appContext)
                    val repository = GuardianRepository(appContext, db.guardianDao())
                    val hasActiveRestrictions = repository.getActiveRestrictedAppsSync().isNotEmpty()

                    if (hasActiveRestrictions) {
                        val serviceIntent = Intent(appContext, BlockOverlayService::class.java)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            appContext.startForegroundService(serviceIntent)
                        } else {
                            appContext.startService(serviceIntent)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

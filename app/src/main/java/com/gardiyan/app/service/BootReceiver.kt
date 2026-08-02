package com.gardiyan.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gardiyan.app.data.local.database.GuardianDatabase
import com.gardiyan.app.data.repository.GuardianRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            val db = GuardianDatabase.getDatabase(appContext)
            val repository = GuardianRepository(appContext, db.guardianDao())

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    KeepAliveScheduler.schedule(appContext)
                    DailySuccessScheduler.schedule(appContext)
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

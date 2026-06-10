package com.gardiyan.app.service

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gardiyan.app.data.local.database.GuardianDatabase
import com.gardiyan.app.data.repository.GuardianRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailySuccessWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = GuardianDatabase.getDatabase(applicationContext)
            val repository = GuardianRepository(applicationContext, db.guardianDao())

            val sharedPref = applicationContext.getSharedPreferences("gardiyan_eval_prefs", Context.MODE_PRIVATE)
            val lastEvaluated = sharedPref.getString("last_evaluated_date", "")

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val yesterdayCal = Calendar.getInstance()
            yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayKey = sdf.format(yesterdayCal.time)

            if (lastEvaluated == yesterdayKey) {
                // Her halükarda günlük sıfırlamayı da tetikle
                withContext(Dispatchers.IO) {
                    repository.resetDailyCountersIfNeeded()
                }
                return Result.success()
            }

            val datesToEvaluate = mutableListOf<String>()
            val lastDate = try { 
                if (!lastEvaluated.isNullOrEmpty()) sdf.parse(lastEvaluated) else null 
            } catch (e: Exception) { 
                null 
            }

            if (lastDate == null) {
                datesToEvaluate.add(yesterdayKey)
            } else {
                val startCal = Calendar.getInstance()
                startCal.time = lastDate
                startCal.add(Calendar.DAY_OF_YEAR, 1)

                val yesterdayCompare = Calendar.getInstance()
                yesterdayCompare.time = yesterdayCal.time
                
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                yesterdayCompare.set(Calendar.HOUR_OF_DAY, 0)
                yesterdayCompare.set(Calendar.MINUTE, 0)
                yesterdayCompare.set(Calendar.SECOND, 0)
                yesterdayCompare.set(Calendar.MILLISECOND, 0)

                while (!startCal.after(yesterdayCompare)) {
                    val key = sdf.format(startCal.time)
                    datesToEvaluate.add(key)
                    startCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            withContext(Dispatchers.IO) {
                for (dateKey in datesToEvaluate) {
                    repository.evaluateDailySuccess(dateKey)
                }
                repository.resetDailyCountersIfNeeded()
            }

            sharedPref.edit().putString("last_evaluated_date", yesterdayKey).apply()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

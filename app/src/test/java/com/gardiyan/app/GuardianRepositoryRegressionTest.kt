package com.gardiyan.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gardiyan.app.data.local.database.GuardianDatabase
import com.gardiyan.app.data.repository.GuardianRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GuardianRepositoryRegressionTest {

    private lateinit var database: GuardianDatabase
    private lateinit var repository: GuardianRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GuardianDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GuardianRepository(database.guardianDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `readding an active restriction updates its limit and active days`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 30)
        val locked = repository.getRestrictedAppByIdSync(id)!!.copy(
            remainingMinutesToday = 0,
            remainingSecondsToday = 0,
            isFailed = true
        )
        repository.updateRestrictedApp(locked)

        repository.upsertRestrictedApp("test.package", "Test", 60)

        val result = repository.getRestrictedAppByIdSync(id)!!
        assertEquals(60, result.dailyLimitMinutes)
        assertEquals(60 * 60, result.remainingSecondsToday)
        assertFalse(result.isFailed)
    }

    @Test
    fun `pending active days are applied during the next daily reset`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 30)
        val app = repository.getRestrictedAppByIdSync(id)!!
        repository.updateRestrictedApp(
            app.copy(
                lastResetDate = "2000-01-01",
                nextDayActiveDays = "Pzt,Sal"
            )
        )

        repository.resetDailyCountersIfNeeded()

        val result = repository.getRestrictedAppByIdSync(id)!!
        assertEquals("Pzt,Sal", result.activeDays)
        assertEquals("", result.nextDayActiveDays)
    }

    @Test
    fun `violation log prevents daily success`() = runBlocking {
        repository.insertDefaultSessionIfMissing()
        repository.upsertRestrictedApp("test.package", "Test", 30)
        repository.insertLog("VIOLATION", "Test", "Limit doldu")

        val success = repository.evaluateDailySuccess(GuardianRepository.todayKey())

        assertFalse(success)
        assertEquals(0, database.guardianDao().getAllLogsSync().count { it.eventType == "SUCCESS_DAY" })
    }

    @Test
    fun `removing the last restriction is still evaluated as a violation`() = runBlocking {
        repository.insertDefaultSessionIfMissing()
        val id = repository.upsertRestrictedApp("test.package", "Test", 30)
        repository.removeRestrictedApp(id)
        repository.insertLog("RESTRICTION_REMOVED", "Test", "Kısıtlama kaldırıldı")

        val success = repository.evaluateDailySuccess(GuardianRepository.todayKey())

        assertFalse(success)
        assertTrue(repository.getSessionSync()!!.hasRedBadge)
    }

    @Test
    fun `successful daily evaluation creates one success log`() = runBlocking {
        repository.insertDefaultSessionIfMissing()
        repository.upsertRestrictedApp("test.package", "Test", 30)
        repository.insertLog("ENGINE_ACTIVE", "", "Limitra active")

        val success = repository.evaluateDailySuccess(GuardianRepository.todayKey())

        assertTrue(success)
        assertEquals(1, database.guardianDao().getAllLogsSync().count { it.eventType == "SUCCESS_DAY" })
    }

    @Test
    fun `evaluation fails if no active engine log is present`() = runBlocking {
        repository.insertDefaultSessionIfMissing()
        repository.upsertRestrictedApp("test.package", "Test", 30)
        
        val success = repository.evaluateDailySuccess(GuardianRepository.todayKey())
        
        assertFalse(success)
        assertEquals(0, database.guardianDao().getAllLogsSync().count { it.eventType == "SUCCESS_DAY" })
    }

    @Test
    fun `evaluation success writes log with correct end of day timestamp`() = runBlocking {
        repository.insertDefaultSessionIfMissing()
        repository.upsertRestrictedApp("test.package", "Test", 30)
        repository.insertLog("ENGINE_ACTIVE", "", "Limitra active")
        
        val todayStr = GuardianRepository.todayKey()
        val success = repository.evaluateDailySuccess(todayStr)
        
        assertTrue(success)
        
        val successLog = database.guardianDao().getAllLogsSync().first { it.eventType == "SUCCESS_DAY" }
        
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val date = sdf.parse(todayStr)!!
        val cal = java.util.Calendar.getInstance()
        cal.time = date
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        
        assertEquals(cal.timeInMillis, successLog.timestamp)
    }

    @Test
    fun `repeated evaluation does not change success state`() = runBlocking {
        repository.insertDefaultSessionIfMissing()
        repository.upsertRestrictedApp("test.package", "Test", 30)
        repository.insertLog("ENGINE_ACTIVE", "", "Limitra active")
        
        val successFirst = repository.evaluateDailySuccess(GuardianRepository.todayKey())
        assertTrue(successFirst)
        
        val successSecond = repository.evaluateDailySuccess(GuardianRepository.todayKey())
        assertTrue(successSecond)
        
        assertEquals(1, database.guardianDao().getAllLogsSync().count { it.eventType == "SUCCESS_DAY" })
    }

    @Test
    fun `upsertRestrictedApp sets lastLimitUpdateDate and todayMinLimitMinutes`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 45)
        val result = repository.getRestrictedAppByIdSync(id)!!
        
        assertEquals(GuardianRepository.todayKey(), result.lastLimitUpdateDate)
        assertEquals(45, result.todayMinLimitMinutes)
    }

    @Test
    fun `resetDailyCountersIfNeeded clears limit update tracking`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 45)
        val app = repository.getRestrictedAppByIdSync(id)!!
        
        repository.updateRestrictedApp(
            app.copy(
                lastResetDate = "2000-01-01"
            )
        )
        
        repository.resetDailyCountersIfNeeded()
        
        val result = repository.getRestrictedAppByIdSync(id)!!
        assertEquals("", result.lastLimitUpdateDate)
        assertEquals(0, result.todayMinLimitMinutes)
    }

    @Test
    fun `invalid daily limits are clamped to a safe range`() = runBlocking {
        val minimumId = repository.upsertRestrictedApp("minimum.package", "Minimum", 0)
        val maximumId = repository.upsertRestrictedApp("maximum.package", "Maximum", Int.MAX_VALUE)

        val minimum = repository.getRestrictedAppByIdSync(minimumId)!!
        val maximum = repository.getRestrictedAppByIdSync(maximumId)!!

        assertEquals(1, minimum.dailyLimitMinutes)
        assertEquals(60, minimum.remainingSecondsToday)
        assertEquals(GuardianRepository.MAX_DAILY_LIMIT_MINUTES, maximum.dailyLimitMinutes)
        assertEquals(GuardianRepository.MAX_DAILY_LIMIT_MINUTES * 60, maximum.remainingSecondsToday)
    }

    @Test
    fun `invalid quick test duration cannot create an exhausted restriction`() = runBlocking {
        val id = repository.insertQuickTestApp("quick.package", "Quick", 0)

        val result = repository.getRestrictedAppByIdSync(id)!!

        assertEquals(1, result.remainingSecondsToday)
        assertFalse(result.isFailed)
    }

    @Test
    fun `upsertRestrictedApp always updates existing active restrictions`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 30, "Pzt")
        
        repository.upsertRestrictedApp("test.package", "Test", 45, "Sal")
        
        val result = repository.getRestrictedAppByIdSync(id)!!
        assertEquals(45, result.dailyLimitMinutes)
        assertEquals(45 * 60, result.remainingSecondsToday)
        assertEquals("Sal", result.activeDays)
    }

    @Test
    fun `insertQuickTestApp always updates existing active restrictions for quick test`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 30)
        
        repository.insertQuickTestApp("test.package", "Test", 15)
        
        val result = repository.getRestrictedAppByIdSync(id)!!
        assertEquals(15, result.remainingSecondsToday)
        assertFalse(result.isFailed)
    }

    @Test
    fun `evaluateDailySuccess does not disable user session on success`() = runBlocking {
        repository.insertDefaultSessionIfMissing()
        val sessionBefore = repository.getSessionSync()!!
        repository.saveSession(sessionBefore.copy(isActive = true))
        
        repository.upsertRestrictedApp("test.package", "Test", 30)
        repository.insertLog("ENGINE_ACTIVE", "", "Limitra active")
        
        val success = repository.evaluateDailySuccess(GuardianRepository.todayKey())
        assertTrue(success)
        
        val sessionAfter = repository.getSessionSync()!!
        assertTrue(sessionAfter.isActive)
    }

    @Test
    fun `evaluateDailySuccess does not award success to multiple past days without engine active log`() = runBlocking {
        repository.insertDefaultSessionIfMissing()
        repository.upsertRestrictedApp("test.package", "Test", 30)
        
        val successDayMinus3 = repository.evaluateDailySuccess("2026-06-07")
        val successDayMinus2 = repository.evaluateDailySuccess("2026-06-08")
        val successYesterday = repository.evaluateDailySuccess("2026-06-09")
        
        assertFalse(successDayMinus3)
        assertFalse(successDayMinus2)
        assertFalse(successYesterday)
        assertEquals(0, database.guardianDao().getAllLogsSync().count { it.eventType == "SUCCESS_DAY" })
    }

    @Test
    fun `incremental session duration updates remaining seconds`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 30)
        val app = repository.getRestrictedAppByIdSync(id)!!
        
        repository.startSession(app)
        val session = repository.getActiveSession()!!
        
        val dbSession = database.guardianDao().getActiveSessionSync()!!
        database.guardianDao().updateActiveSession(
            dbSession.copy(
                lastSeenAtMillis = System.currentTimeMillis() - 5000L
            )
        )
        
        repository.updateSessionLastSeen("test.package")
        
        val result = repository.getRestrictedAppByIdSync(id)!!
        val remaining = result.remainingSecondsToday
        assertTrue(remaining in 1793..1797)
    }

    @Test
    fun `closeActiveSession only subtracts duration since last heartbeat`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 30)
        val app = repository.getRestrictedAppByIdSync(id)!!
        
        repository.startSession(app)
        
        val dbSession = database.guardianDao().getActiveSessionSync()!!
        database.guardianDao().updateActiveSession(
            dbSession.copy(
                entryAtMillis = System.currentTimeMillis() - 10000L,
                lastSeenAtMillis = System.currentTimeMillis() - 3000L
            )
        )
        
        repository.closeActiveSession("Test Close")
        
        val result = repository.getRestrictedAppByIdSync(id)!!
        val remaining = result.remainingSecondsToday
        assertTrue(remaining in 1795..1798)
    }
}

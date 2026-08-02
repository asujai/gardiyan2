package com.gardiyan.app

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gardiyan.app.data.local.database.GuardianDatabase
import com.gardiyan.app.data.repository.GuardianRepository
import com.gardiyan.app.data.time.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowUsageStatsManager
import org.robolectric.shadows.ShadowUsageStatsManager.UsageStatsBuilder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GuardianRepositoryRegressionTest {

    private lateinit var database: GuardianDatabase
    private lateinit var repository: GuardianRepository

    private class FakeTimeProvider(
        var wallMillis: Long = 1_800_000_000_000L,
        var elapsedMillis: Long = 1_000_000L
    ) : TimeProvider {
        override fun currentTimeMillis(): Long = wallMillis
        override fun elapsedRealtime(): Long = elapsedMillis
        override fun localDateString(): String = "2026-06-10"
        override fun todayDayLabel(): String = "Pzt"
        override fun timezoneId(): String = "Europe/Istanbul"
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ShadowUsageStatsManager.reset()
        database = Room.inMemoryDatabaseBuilder(context, GuardianDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GuardianRepository(context, database.guardianDao())
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
    fun `evaluation succeeds without an engine log when restriction existed and no violation occurred`() = runBlocking {
        repository.insertDefaultSessionIfMissing()
        repository.upsertRestrictedApp("test.package", "Test", 30)
        
        val success = repository.evaluateDailySuccess(GuardianRepository.todayKey())
        
        assertTrue(success)
        assertEquals(1, database.guardianDao().getAllLogsSync().count { it.eventType == "SUCCESS_DAY" })
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
    fun `successful streak advances through all six levels`() = runBlocking {
        repository.insertDefaultSessionIfMissing()

        repeat(60) {
            repository.succeedActiveTarget()
        }

        val session = repository.getSessionSync()!!
        assertEquals(6, session.level)
        assertEquals(60, session.consecutiveSuccessDays)
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
                lastSeenAtMillis = System.currentTimeMillis() - 5000L,
                lastSeenElapsedRealtime = 0L
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
                lastSeenAtMillis = System.currentTimeMillis() - 3000L,
                lastSeenElapsedRealtime = 0L
            )
        )
        
        repository.closeActiveSession("Test Close")
        
        val result = repository.getRestrictedAppByIdSync(id)!!
        val remaining = result.remainingSecondsToday
        assertTrue(remaining in 1795..1798)
    }

    @Test
    fun `closeActiveSession charges all remaining time on long delays`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 30)
        val app = repository.getRestrictedAppByIdSync(id)!!
        
        repository.startSession(app)
        
        val dbSession = database.guardianDao().getActiveSessionSync()!!
        database.guardianDao().updateActiveSession(
            dbSession.copy(
                entryAtMillis = System.currentTimeMillis() - 12 * 3600 * 1000L,
                lastSeenAtMillis = System.currentTimeMillis() - 12 * 3600 * 1000L,
                lastSeenElapsedRealtime = 0L
            )
        )
        
        repository.closeActiveSession("Test Close Long Delay")
        
        val result = repository.getRestrictedAppByIdSync(id)!!
        val remaining = result.remainingSecondsToday
        assertEquals(0, remaining)
        assertFalse(result.isFailed)
    }

    @Test
    fun `cleanupStaleSessions charges all remaining time on long delays`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 30)
        val app = repository.getRestrictedAppByIdSync(id)!!
        
        repository.startSession(app)
        
        val dbSession = database.guardianDao().getActiveSessionSync()!!
        database.guardianDao().updateActiveSession(
            dbSession.copy(
                entryAtMillis = System.currentTimeMillis() - 12 * 3600 * 1000L,
                lastSeenAtMillis = System.currentTimeMillis() - 12 * 3600 * 1000L,
                lastSeenElapsedRealtime = 0L
            )
        )
        
        repository.cleanupStaleSessions()
        
        val result = repository.getRestrictedAppByIdSync(id)!!
        val remaining = result.remainingSecondsToday
        assertEquals(0, remaining)
        assertFalse(result.isFailed)
    }

    @Test
    fun `delayed heartbeat still charges elapsed usage`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 30)
        val app = repository.getRestrictedAppByIdSync(id)!!

        repository.startSession(app)

        val dbSession = database.guardianDao().getActiveSessionSync()!!
        database.guardianDao().updateActiveSession(
            dbSession.copy(
                lastSeenAtMillis = System.currentTimeMillis() - 45_000L,
                lastSeenElapsedRealtime = 0L
            )
        )

        repository.updateSessionLastSeen("test.package")

        val result = repository.getRestrictedAppByIdSync(id)!!
        assertTrue(result.remainingSecondsToday in 1753..1757)
    }

    // --- Yeni Zaman Açığı ve Günlük Reset Testleri ---

    @Test
    fun `heartbeat charges elapsed realtime when wall clock moves backwards`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeTime = FakeTimeProvider()
        val timedRepository = GuardianRepository(context, database.guardianDao(), fakeTime)
        val id = timedRepository.upsertRestrictedApp("test.package", "Test", 30)
        val app = timedRepository.getRestrictedAppByIdSync(id)!!

        timedRepository.startSession(app)
        fakeTime.wallMillis -= 5_000L
        fakeTime.elapsedMillis += 45_000L

        timedRepository.updateSessionLastSeen("test.package")

        val result = timedRepository.getRestrictedAppByIdSync(id)!!
        assertTrue(result.remainingSecondsToday in 1753..1757)
    }

    @Test
    fun `usage stats reconciliation initializes unknown baseline without charging old usage`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 120)
        val app = repository.getRestrictedAppByIdSync(id)!!
        repository.updateRestrictedApp(
            app.copy(
                usageStatsBaselineMillisToday = GuardianRepository.UNKNOWN_USAGE_STATS_BASELINE,
                lastUsageStatsObservedMillisToday = 0L
            )
        )

        val result = repository.reconcileRestrictedAppWithObservedUsage(
            appId = id,
            observedUsageMillisToday = 75 * 60_000L
        )

        val updated = repository.getRestrictedAppByIdSync(id)!!
        assertTrue(result!!.baselineInitialized)
        assertEquals(75 * 60_000L, updated.usageStatsBaselineMillisToday)
        assertEquals(120 * 60, updated.remainingSecondsToday)
        assertFalse(updated.isFailed)
    }

    @Test
    fun `usage stats reconciliation charges measured usage when baseline was unknown`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 120)
        val app = repository.getRestrictedAppByIdSync(id)!!
        repository.updateRestrictedApp(
            app.copy(
                usageStatsBaselineMillisToday = GuardianRepository.UNKNOWN_USAGE_STATS_BASELINE,
                lastUsageStatsObservedMillisToday = 0L,
                remainingSecondsToday = 120 * 60
            )
        )

        val result = repository.reconcileRestrictedAppWithObservedUsage(
            appId = id,
            observedUsageMillisToday = 150 * 60_000L,
            measuredUsageMillisSinceTrackingStart = 120 * 60_000L
        )

        val updated = repository.getRestrictedAppByIdSync(id)!!
        assertTrue(result!!.baselineInitialized)
        assertEquals(120 * 60, result.adjustedSeconds)
        assertEquals(30 * 60_000L, updated.usageStatsBaselineMillisToday)
        assertEquals(0, updated.remainingSecondsToday)
        assertFalse(updated.isFailed)
    }

    @Test
    fun `usage stats reconciliation charges usage after restriction baseline`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 120)
        val app = repository.getRestrictedAppByIdSync(id)!!
        repository.updateRestrictedApp(
            app.copy(
                usageStatsBaselineMillisToday = 10 * 60_000L,
                lastUsageStatsObservedMillisToday = 10 * 60_000L,
                remainingSecondsToday = 120 * 60
            )
        )

        val result = repository.reconcileRestrictedAppWithObservedUsage(
            appId = id,
            observedUsageMillisToday = 95 * 60_000L
        )

        val updated = repository.getRestrictedAppByIdSync(id)!!
        assertEquals(85 * 60, result!!.adjustedSeconds)
        assertEquals(35 * 60, updated.remainingSecondsToday)
        assertEquals(35, updated.remainingMinutesToday)
        assertFalse(updated.isFailed)
    }

    @Test
    fun `usage stats reconciliation never gives time back`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 120)
        val app = repository.getRestrictedAppByIdSync(id)!!
        repository.updateRestrictedApp(
            app.copy(
                usageStatsBaselineMillisToday = 0L,
                lastUsageStatsObservedMillisToday = 0L,
                remainingSecondsToday = 30 * 60,
                remainingMinutesToday = 30
            )
        )

        val result = repository.reconcileRestrictedAppWithObservedUsage(
            appId = id,
            observedUsageMillisToday = 10 * 60_000L
        )

        val updated = repository.getRestrictedAppByIdSync(id)!!
        assertEquals(null, result)
        assertEquals(30 * 60, updated.remainingSecondsToday)
        assertEquals(30, updated.remainingMinutesToday)
    }

    @Test
    fun `usage stats reconciliation can exhaust limit without marking failure`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 120)
        val app = repository.getRestrictedAppByIdSync(id)!!
        repository.updateRestrictedApp(
            app.copy(
                usageStatsBaselineMillisToday = 0L,
                lastUsageStatsObservedMillisToday = 0L,
                remainingSecondsToday = 120 * 60
            )
        )

        val result = repository.reconcileRestrictedAppWithObservedUsage(
            appId = id,
            observedUsageMillisToday = 150 * 60_000L
        )

        val updated = repository.getRestrictedAppByIdSync(id)!!
        assertEquals(120 * 60, result!!.adjustedSeconds)
        assertEquals(0, updated.remainingSecondsToday)
        assertFalse(updated.isFailed)
    }

    @Test
    fun `daily reset uses usage stats snapshot as baseline so carried over minutes are not charged`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        clearSecureResetPrefs(context)
        val resetAt = localMillis(2026, 6, 11, 0, 5)
        val dayStart = localMillis(2026, 6, 11, 0, 0)
        val testProvider = TestTimeProvider(
            mockTimeMillis = resetAt,
            mockElapsedRealtime = 2_000_000L,
            mockLocalDateString = "2026-06-11"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStatsShadow = Shadows.shadowOf(usageStatsManager)
        val usageStats = UsageStatsBuilder.newBuilder()
            .setPackageName("test.package")
            .setFirstTimeStamp(dayStart)
            .setLastTimeStamp(resetAt)
            .setLastTimeUsed(resetAt)
            .setTotalTimeInForeground(20 * 60_000L)
            .build()
        usageStatsShadow.addUsageStats(UsageStatsManager.INTERVAL_DAILY, usageStats)
        usageStatsShadow.addUsageStats(UsageStatsManager.INTERVAL_BEST, usageStats)

        val id = secureRepository.upsertRestrictedApp("test.package", "Test", 120)
        val app = secureRepository.getRestrictedAppByIdSync(id)!!
        secureRepository.updateRestrictedApp(
            app.copy(
                lastResetDate = "2026-06-10",
                remainingMinutesToday = 0,
                remainingSecondsToday = 0,
                isFailed = true,
                usageStatsBaselineMillisToday = 0L,
                lastUsageStatsObservedMillisToday = 0L,
                lastUsageStatsReconciledAtMillis = 0L
            )
        )

        secureRepository.resetDailyCountersIfNeeded()

        val resetApp = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(120 * 60, resetApp.remainingSecondsToday)
        assertEquals(20 * 60_000L, resetApp.usageStatsBaselineMillisToday)
        assertEquals(20 * 60_000L, resetApp.lastUsageStatsObservedMillisToday)

        val reconciliation = secureRepository.reconcileRestrictedAppWithObservedUsage(
            appId = id,
            observedUsageMillisToday = 20 * 60_000L
        )

        val updated = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(null, reconciliation)
        assertEquals(120 * 60, updated.remainingSecondsToday)
    }

    @Test
    fun `daily reset carries only active session usage after local midnight`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        clearSecureResetPrefs(context)
        val sessionStart = localMillis(2026, 6, 10, 23, 50)
        val resetAt = localMillis(2026, 6, 11, 0, 5)
        val elapsedAtStart = 1_000_000L
        val elapsedAtReset = elapsedAtStart + (resetAt - sessionStart)
        val testProvider = TestTimeProvider(
            mockTimeMillis = sessionStart,
            mockElapsedRealtime = elapsedAtStart,
            mockLocalDateString = "2026-06-10"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)

        val id = secureRepository.upsertRestrictedApp("test.package", "Test", 30)
        secureRepository.resetDailyCountersIfNeeded()
        secureRepository.startSession(secureRepository.getRestrictedAppByIdSync(id)!!)

        val activeSession = database.guardianDao().getActiveSessionSync()!!
        database.guardianDao().updateActiveSession(
            activeSession.copy(
                lastSeenAtMillis = resetAt - 10_000L,
                lastSeenElapsedRealtime = elapsedAtReset - 10_000L
            )
        )
        secureRepository.updateRestrictedApp(
            secureRepository.getRestrictedAppByIdSync(id)!!.copy(
                lastResetDate = "2026-06-10",
                remainingMinutesToday = 0,
                remainingSecondsToday = 0,
                isFailed = true
            )
        )

        testProvider.mockTimeMillis = resetAt
        testProvider.mockElapsedRealtime = elapsedAtReset
        testProvider.mockLocalDateString = "2026-06-11"

        secureRepository.resetDailyCountersIfNeeded()

        val resetApp = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(25 * 60, resetApp.remainingSecondsToday)
        assertFalse(resetApp.isFailed)

        val realignedSession = database.guardianDao().getActiveSessionSync()!!
        assertEquals(resetAt, realignedSession.entryAtMillis)
        assertEquals(resetAt, realignedSession.lastSeenAtMillis)

        testProvider.mockTimeMillis += 2 * 60_000L
        testProvider.mockElapsedRealtime += 2 * 60_000L
        secureRepository.updateSessionLastSeen("test.package")

        val afterTwoMinutes = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(23 * 60, afterTwoMinutes.remainingSecondsToday)
    }

    class TestTimeProvider(
        var mockTimeMillis: Long = System.currentTimeMillis(),
        var mockElapsedRealtime: Long = android.os.SystemClock.elapsedRealtime(),
        var mockLocalDateString: String = "2026-06-10",
        var mockTimezoneId: String = "UTC"
    ) : com.gardiyan.app.data.time.TimeProvider {
        override fun currentTimeMillis(): Long = mockTimeMillis
        override fun elapsedRealtime(): Long = mockElapsedRealtime
        override fun localDateString(): String = mockLocalDateString
        override fun timezoneId(): String = mockTimezoneId
        override fun todayDayLabel(): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val date = sdf.parse(mockLocalDateString) ?: java.util.Date()
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            return when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.MONDAY -> "Pzt"
                java.util.Calendar.TUESDAY -> "Sal"
                java.util.Calendar.WEDNESDAY -> "Çar"
                java.util.Calendar.THURSDAY -> "Per"
                java.util.Calendar.FRIDAY -> "Cum"
                java.util.Calendar.SATURDAY -> "Cmt"
                java.util.Calendar.SUNDAY -> "Paz"
                else -> ""
            }
        }
    }

    private fun localMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month - 1)
            set(java.util.Calendar.DAY_OF_MONTH, day)
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun clearSecureResetPrefs(context: Context) {
        context.getSharedPreferences("gardiyan_secure_reset_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `resetDailyCountersIfNeeded does not reset if date has not changed`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testProvider = TestTimeProvider(
            mockTimeMillis = 1000000000L,
            mockElapsedRealtime = 1000000L,
            mockLocalDateString = "2026-06-10"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)
        
        val id = secureRepository.upsertRestrictedApp("test.package", "Test", 30)
        database.guardianDao().updateRestrictedApp(
            database.guardianDao().getRestrictedAppByIdSync(id)!!.copy(
                remainingSecondsToday = 0,
                isFailed = true
            )
        )
        
        // İlk kaydetme (lastReset properties initialization)
        secureRepository.resetDailyCountersIfNeeded()
        
        // 24 saat geçsin ama tarih değişmesin (örneğin timezone/saat manipülasyonu)
        testProvider.mockTimeMillis += 24 * 3600 * 1000L
        testProvider.mockElapsedRealtime += 24 * 3600 * 1000L
        
        secureRepository.resetDailyCountersIfNeeded()
        
        // Sıfırlama olmamalı çünkü tarih aynı kalmış
        val result = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(0, result.remainingSecondsToday)
        assertTrue(result.isFailed)
    }

    @Test
    fun `resetDailyCountersIfNeeded resets on a legit short day change with consistent clocks`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testProvider = TestTimeProvider(
            mockTimeMillis = 1000000000L,
            mockElapsedRealtime = 1000000L,
            mockLocalDateString = "2026-06-10"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)

        val id = secureRepository.upsertRestrictedApp("test.package", "Test", 30)
        database.guardianDao().updateRestrictedApp(
            database.guardianDao().getRestrictedAppByIdSync(id)!!.copy(
                remainingSecondsToday = 0,
                isFailed = true
            )
        )

        secureRepository.resetDailyCountersIfNeeded()

        // Tarih ertesi güne geçti ve sadece 5 saat geçti AMA duvar saati ile monotonik
        // saat birlikte ilerledi (saat manipülasyonu yok). Bu yasal bir gün geçişidir;
        // kullanıcı gece yarısını geçti (ör. son sıfırlama 21:00, şimdi 02:00). 22 saat
        // beklenmeden günlük hak yenilenmelidir.
        testProvider.mockLocalDateString = "2026-06-11"
        testProvider.mockTimeMillis += 5 * 3600 * 1000L
        testProvider.mockElapsedRealtime += 5 * 3600 * 1000L

        secureRepository.resetDailyCountersIfNeeded()

        // Sıfırlanmış olmalıdır
        val result = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(1800, result.remainingSecondsToday)
        assertFalse(result.isFailed)
    }

    @Test
    fun `resetDailyCountersIfNeeded blocks reset when clock is jumped forward to fake a new day`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testProvider = TestTimeProvider(
            mockTimeMillis = 1000000000L,
            mockElapsedRealtime = 1000000L,
            mockLocalDateString = "2026-06-10"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)

        val id = secureRepository.upsertRestrictedApp("test.package", "Test", 30)
        database.guardianDao().updateRestrictedApp(
            database.guardianDao().getRestrictedAppByIdSync(id)!!.copy(
                remainingSecondsToday = 0,
                isFailed = true
            )
        )

        secureRepository.resetDailyCountersIfNeeded()

        // Kullanıcı saati ANINDA bir gün ileri aldı: duvar saati +25 saat fırladı ama
        // monotonik saat sadece birkaç saniye ilerledi -> saat ileri alma (hile).
        testProvider.mockLocalDateString = "2026-06-11"
        testProvider.mockTimeMillis += 25 * 3600 * 1000L
        testProvider.mockElapsedRealtime += 5_000L

        secureRepository.resetDailyCountersIfNeeded()

        // Sıfırlama OLMAMALIDIR (anti-cheat)
        val result = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(0, result.remainingSecondsToday)
        assertTrue(result.isFailed)
    }

    @Test
    fun `resetDailyCountersIfNeeded allows a small 30 minute forward jump within tolerance`() = runBlocking {
        // Tolerans 1 saattir (CLOCK_FORWARD_JUMP_TOLERANCE_MS). 30 dk'lık öne fırlama
        // (örn. DST/NTP düzeltmesi veya gece yarısına yakın küçük kayma) yasal sayılır.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testProvider = TestTimeProvider(
            mockTimeMillis = 1000000000L,
            mockElapsedRealtime = 1000000L,
            mockLocalDateString = "2026-06-10"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)

        val id = secureRepository.upsertRestrictedApp("test.package", "Test", 30)
        database.guardianDao().updateRestrictedApp(
            database.guardianDao().getRestrictedAppByIdSync(id)!!.copy(
                remainingSecondsToday = 0,
                isFailed = true
            )
        )

        secureRepository.resetDailyCountersIfNeeded()

        // Duvar saati 30 dk öne fırladı, monotonik saat ~1 sn ilerledi -> forwardJump ~30dk <= 1sa
        testProvider.mockLocalDateString = "2026-06-11"
        testProvider.mockTimeMillis += 30 * 60 * 1000L
        testProvider.mockElapsedRealtime += 1_000L

        secureRepository.resetDailyCountersIfNeeded()

        // Tolerans dahilinde olduğu için sıfırlanmalıdır
        val result = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(1800, result.remainingSecondsToday)
        assertFalse(result.isFailed)
    }

    @Test
    fun `resetDailyCountersIfNeeded blocks a 2 hour forward jump beyond tolerance`() = runBlocking {
        // 2 saatlik öne fırlama, 1 saatlik toleransı aşar -> saat ileri alma (hile) sayılır.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testProvider = TestTimeProvider(
            mockTimeMillis = 1000000000L,
            mockElapsedRealtime = 1000000L,
            mockLocalDateString = "2026-06-10"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)

        val id = secureRepository.upsertRestrictedApp("test.package", "Test", 30)
        database.guardianDao().updateRestrictedApp(
            database.guardianDao().getRestrictedAppByIdSync(id)!!.copy(
                remainingSecondsToday = 0,
                isFailed = true
            )
        )

        secureRepository.resetDailyCountersIfNeeded()

        // Duvar saati 2 saat öne fırladı, monotonik saat ~1 sn ilerledi -> forwardJump ~2sa > 1sa
        testProvider.mockLocalDateString = "2026-06-11"
        testProvider.mockTimeMillis += 2 * 3600 * 1000L
        testProvider.mockElapsedRealtime += 1_000L

        secureRepository.resetDailyCountersIfNeeded()

        // Toleransı aştığı ve 22 saat dolmadığı için sıfırlama OLMAMALIDIR
        val result = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(0, result.remainingSecondsToday)
        assertTrue(result.isFailed)
    }

    @Test
    fun `resetDailyCountersIfNeeded logs an early reset prevention only once per date`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testProvider = TestTimeProvider(
            mockTimeMillis = 1000000000L,
            mockElapsedRealtime = 1000000L,
            mockLocalDateString = "2026-06-10"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)

        secureRepository.upsertRestrictedApp("test.package", "Test", 30)
        secureRepository.resetDailyCountersIfNeeded()

        // Saat ileri alma (hile): duvar saati +25 saat fırladı ama monotonik saat
        // neredeyse hiç ilerlemedi. Erken sıfırlama engellenir ve aynı tarih için
        // RESET_PREVENTED yalnızca BİR kez loglanır.
        testProvider.mockLocalDateString = "2026-06-11"
        testProvider.mockTimeMillis += 25 * 3600 * 1000L
        testProvider.mockElapsedRealtime += 5_000L

        repeat(3) {
            secureRepository.resetDailyCountersIfNeeded()
        }

        assertEquals(
            1,
            database.guardianDao().getAllLogsSync().count { it.eventType == "RESET_PREVENTED" }
        )
    }

    @Test
    fun `resetDailyCountersIfNeeded resets if date changed and at least 22 hours passed`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testProvider = TestTimeProvider(
            mockTimeMillis = 1000000000L,
            mockElapsedRealtime = 1000000L,
            mockLocalDateString = "2026-06-10"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)
        
        val id = secureRepository.upsertRestrictedApp("test.package", "Test", 30)
        database.guardianDao().updateRestrictedApp(
            database.guardianDao().getRestrictedAppByIdSync(id)!!.copy(
                remainingSecondsToday = 0,
                isFailed = true
            )
        )
        
        secureRepository.resetDailyCountersIfNeeded()
        
        // Tarih ertesi gün ve 23 saat geçmiş
        testProvider.mockLocalDateString = "2026-06-11"
        testProvider.mockTimeMillis += 23 * 3600 * 1000L
        testProvider.mockElapsedRealtime += 23 * 3600 * 1000L
        
        secureRepository.resetDailyCountersIfNeeded()
        
        // Sıfırlanmış olmalıdır
        val result = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(1800, result.remainingSecondsToday)
        assertFalse(result.isFailed)
    }

    @Test
    fun `resetDailyCountersIfNeeded blocks reset if time is manipulated backwards`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testProvider = TestTimeProvider(
            mockTimeMillis = 1000000000L,
            mockElapsedRealtime = 1000000L,
            mockLocalDateString = "2026-06-10"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)
        
        val id = secureRepository.upsertRestrictedApp("test.package", "Test", 30)
        database.guardianDao().updateRestrictedApp(
            database.guardianDao().getRestrictedAppByIdSync(id)!!.copy(
                remainingSecondsToday = 0,
                isFailed = true
            )
        )
        
        secureRepository.resetDailyCountersIfNeeded()
        
        // Kullanıcı saati/tarihi geriye çekmiş
        testProvider.mockLocalDateString = "2026-06-09"
        testProvider.mockTimeMillis -= 10 * 3600 * 1000L
        testProvider.mockElapsedRealtime += 10 * 3600 * 1000L
        
        secureRepository.resetDailyCountersIfNeeded()
        
        // Sıfırlama olmamalı
        val result = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(0, result.remainingSecondsToday)
        assertTrue(result.isFailed)
    }

    @Test
    fun `resetDailyCountersIfNeeded handles reboot correctly if wall clock passed`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testProvider = TestTimeProvider(
            mockTimeMillis = 1000000000L,
            mockElapsedRealtime = 50000000L,
            mockLocalDateString = "2026-06-10"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)
        
        val id = secureRepository.upsertRestrictedApp("test.package", "Test", 30)
        database.guardianDao().updateRestrictedApp(
            database.guardianDao().getRestrictedAppByIdSync(id)!!.copy(
                remainingSecondsToday = 0,
                isFailed = true
            )
        )
        
        secureRepository.resetDailyCountersIfNeeded()
        
        // Cihaz reboot edildi (elapsedRealtime 0'a yakın bir değere düştü), ama tarih değişti ve wall clock 24 saat geçti
        testProvider.mockLocalDateString = "2026-06-11"
        testProvider.mockTimeMillis += 24 * 3600 * 1000L
        testProvider.mockElapsedRealtime = 5000L // Reboot sonrası 5. saniye
        
        secureRepository.resetDailyCountersIfNeeded()
        
        // Sıfırlanmış olmalıdır (reboot dayanıklılığı)
        val result = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(1800, result.remainingSecondsToday)
        assertFalse(result.isFailed)
    }

    @Test
    fun `resetDailyCountersIfNeeded blocks reset if rebooted but boot time indicates backward time manipulation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testProvider = TestTimeProvider(
            mockTimeMillis = 1000000000L,
            mockElapsedRealtime = 50000000L,
            mockLocalDateString = "2026-06-10"
        )
        val secureRepository = GuardianRepository(context, database.guardianDao(), testProvider)
        
        val id = secureRepository.upsertRestrictedApp("test.package", "Test", 30)
        database.guardianDao().updateRestrictedApp(
            database.guardianDao().getRestrictedAppByIdSync(id)!!.copy(
                remainingSecondsToday = 0,
                isFailed = true
            )
        )
        
        secureRepository.resetDailyCountersIfNeeded()
        
        // Cihaz reboot edildi ama saat 1 gün GERİYE alındı
        testProvider.mockLocalDateString = "2026-06-09"
        testProvider.mockTimeMillis -= 24 * 3600 * 1000L // Geri alındı
        testProvider.mockElapsedRealtime = 5000L // Reboot sonrası 5. saniye
        
        secureRepository.resetDailyCountersIfNeeded()
        
        // Sıfırlanma OLMAMALIDIR
        val result = secureRepository.getRestrictedAppByIdSync(id)!!
        assertEquals(0, result.remainingSecondsToday)
        assertTrue(result.isFailed)
    }

    @Test
    fun `evaluateMissedDays successfully catches up on multiple missed days`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository.insertDefaultSessionIfMissing()
        val id = repository.upsertRestrictedApp("test.package", "Test", 30)
        
        // Gecmis gunlerde de uygulamanin var sayilmasi icin createdAtMillis'i 10 gun geriye çekelim
        val app = repository.getRestrictedAppByIdSync(id)!!
        repository.updateRestrictedApp(
            app.copy(
                createdAtMillis = System.currentTimeMillis() - 10 * 24 * 3600 * 1000L
            )
        )
        
        // 3 gün önce ve dün için ENGINE_ACTIVE log'ları ekleyelim ki başarı kazanılabilsin
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        
        val cal3 = java.util.Calendar.getInstance()
        cal3.add(java.util.Calendar.DAY_OF_YEAR, -3)
        cal3.set(java.util.Calendar.HOUR_OF_DAY, 12)
        cal3.set(java.util.Calendar.MINUTE, 0)
        cal3.set(java.util.Calendar.SECOND, 0)
        cal3.set(java.util.Calendar.MILLISECOND, 0)
        val day3Key = sdf.format(cal3.time)
        repository.insertLog("ENGINE_ACTIVE", "", "Engine active day -3", customTimestamp = cal3.timeInMillis)
        
        val cal2 = java.util.Calendar.getInstance()
        cal2.add(java.util.Calendar.DAY_OF_YEAR, -2)
        cal2.set(java.util.Calendar.HOUR_OF_DAY, 12)
        cal2.set(java.util.Calendar.MINUTE, 0)
        cal2.set(java.util.Calendar.SECOND, 0)
        cal2.set(java.util.Calendar.MILLISECOND, 0)
        val day2Key = sdf.format(cal2.time)
        repository.insertLog("ENGINE_ACTIVE", "", "Engine active day -2", customTimestamp = cal2.timeInMillis)
        
        val cal1 = java.util.Calendar.getInstance()
        cal1.add(java.util.Calendar.DAY_OF_YEAR, -1)
        cal1.set(java.util.Calendar.HOUR_OF_DAY, 12)
        cal1.set(java.util.Calendar.MINUTE, 0)
        cal1.set(java.util.Calendar.SECOND, 0)
        cal1.set(java.util.Calendar.MILLISECOND, 0)
        val day1Key = sdf.format(cal1.time)
        repository.insertLog("ENGINE_ACTIVE", "", "Engine active day -1", customTimestamp = cal1.timeInMillis)
        
        // gardiyan_eval_prefs'te last_evaluated_date olarak 4 gün öncesini set edelim
        val cal4 = java.util.Calendar.getInstance()
        cal4.add(java.util.Calendar.DAY_OF_YEAR, -4)
        val day4Key = sdf.format(cal4.time)
        
        val sharedPref = context.getSharedPreferences("gardiyan_eval_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("last_evaluated_date", day4Key).apply()
        
        repository.evaluateMissedDays()
        
        // evaluateMissedDays sonrasında last_evaluated_date dün (day1Key) olmalıdır
        assertEquals(day1Key, sharedPref.getString("last_evaluated_date", ""))
        
        // Loglarda 3 adet SUCCESS_DAY olmalıdır
        val logs = database.guardianDao().getAllLogsSync()
        assertEquals(3, logs.count { it.eventType == "SUCCESS_DAY" })
    }
}

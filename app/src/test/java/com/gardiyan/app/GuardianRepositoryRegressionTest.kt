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
    fun `readding an active restriction does not reset its lock state`() = runBlocking {
        val id = repository.upsertRestrictedApp("test.package", "Test", 30)
        val locked = repository.getRestrictedAppByIdSync(id)!!.copy(
            remainingMinutesToday = 0,
            remainingSecondsToday = 0,
            isFailed = true
        )
        repository.updateRestrictedApp(locked)

        repository.upsertRestrictedApp("test.package", "Test", 60)

        val result = repository.getRestrictedAppByIdSync(id)!!
        assertEquals(30, result.dailyLimitMinutes)
        assertEquals(0, result.remainingSecondsToday)
        assertTrue(result.isFailed)
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

        val success = repository.evaluateDailySuccess(GuardianRepository.todayKey())

        assertTrue(success)
        assertEquals(1, database.guardianDao().getAllLogsSync().count { it.eventType == "SUCCESS_DAY" })
    }
}

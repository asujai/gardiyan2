package com.gardiyan.app

import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.viewmodel.shouldPenalizeRestrictionRemoval
import com.gardiyan.app.viewmodel.withReducedDailyLimit
import com.gardiyan.app.viewmodel.buildRestrictionAssignments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianViewModelRulesTest {

    @Test
    fun `unnamed apps are saved as separate app cards with their own names`() {
        val assignments = buildRestrictionAssignments(
            restrictionName = "  ",
            apps = listOf("Netflix" to "com.netflix", "Reddit" to "com.reddit"),
            namedGroupId = "unused-group"
        )

        assertEquals(listOf("com.netflix", "com.reddit"), assignments.map { it.groupId })
        assertEquals(listOf("Netflix", "Reddit"), assignments.map { it.displayName })
    }

    @Test
    fun `custom named apps share one restriction group`() {
        val assignments = buildRestrictionAssignments(
            restrictionName = " Gece Sosyal Medya ",
            apps = listOf("TikTok" to "com.tiktok", "Instagram" to "com.instagram"),
            namedGroupId = "shared-group"
        )

        assertTrue(assignments.all { it.groupId == "shared-group" })
        assertTrue(assignments.all { it.displayName == "Gece Sosyal Medya" })
    }

    @Test
    fun `removing a restriction before its limit is exhausted is not a discipline failure`() {
        val app = restriction(remainingSeconds = 60)

        assertFalse(shouldPenalizeRestrictionRemoval(app))
    }

    @Test
    fun `removing a restriction after its limit is exhausted is a discipline failure`() {
        val app = restriction(remainingSeconds = 0)

        assertTrue(shouldPenalizeRestrictionRemoval(app))
    }

    @Test
    fun `reducing limit to already used time exhausts remaining seconds without failing the day`() {
        val app = restriction(
            dailyLimitMinutes = 120,
            remainingSeconds = 30 * 60,
            isFailed = false
        )

        val updated = app.withReducedDailyLimit(
            newLimitMinutes = 60,
            nextActiveDays = app.nextDayActiveDays
        )

        assertEquals(0, updated.remainingSecondsToday)
        assertEquals(0, updated.remainingMinutesToday)
        assertFalse(updated.isFailed)
    }

    @Test
    fun `reducing limit preserves an existing failure flag instead of creating a new one`() {
        val app = restriction(
            dailyLimitMinutes = 120,
            remainingSeconds = 90 * 60,
            isFailed = true
        )

        val updated = app.withReducedDailyLimit(
            newLimitMinutes = 100,
            nextActiveDays = app.nextDayActiveDays
        )

        assertTrue(updated.isFailed)
    }

    private fun restriction(
        dailyLimitMinutes: Int = 120,
        remainingSeconds: Int,
        isFailed: Boolean = false
    ) = RestrictedAppEntity(
        packageName = "test.package",
        appName = "Test",
        dailyLimitMinutes = dailyLimitMinutes,
        remainingMinutesToday = remainingSeconds / 60,
        remainingSecondsToday = remainingSeconds,
        isFailed = isFailed
    )
}

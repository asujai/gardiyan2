package com.gardiyan.app

import com.gardiyan.app.data.local.entity.StatusLogEntity
import com.gardiyan.app.ui.screens.ProfileProgressMode
import com.gardiyan.app.ui.screens.QuoteSummaryMode
import com.gardiyan.app.ui.screens.calculateProfileProgress
import com.gardiyan.app.ui.screens.deduplicateTimelineLogs
import com.gardiyan.app.ui.screens.deriveQuoteSummaryMode
import com.gardiyan.app.ui.screens.shouldStackProfileSummaryCards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileUiLogicTest {

    @Test
    fun `timeline keeps distinct apps from the same bulk action`() {
        val timestamp = 1_000_000L
        val logs = listOf(
            log("YouTube", "com.google.android.youtube", timestamp),
            log("Instagram", "com.instagram.android", timestamp - 1),
            log("TikTok", "com.zhiliaoapp.musically", timestamp - 2)
        )

        val result = deduplicateTimelineLogs(logs)

        assertEquals(listOf("YouTube", "Instagram", "TikTok"), result.map { it.appName })
    }

    @Test
    fun `timeline removes only a genuine near duplicate for the same package`() {
        val original = log("YouTube", "com.google.android.youtube", 10_000L)
        val duplicate = original.copy(id = 2, timestamp = 8_000L)
        val laterAction = original.copy(id = 3, timestamp = 1_000L)

        val result = deduplicateTimelineLogs(listOf(original, duplicate, laterAction))

        assertEquals(listOf(original, laterAction), result)
    }

    @Test
    fun `legacy blank package logs use app name identity`() {
        val youtube = log("YouTube", "", 2_000L)
        val instagram = log("Instagram", "", 1_999L)

        assertEquals(2, deduplicateTimelineLogs(listOf(youtube, instagram)).size)
    }

    @Test
    fun `quote summary follows selected JSON quotes and show only preference`() {
        val json = """[{"id":"1","text":"Focus","author":"A","isSelected":true}]"""

        assertEquals(
            QuoteSummaryMode.MIXED,
            deriveQuoteSummaryMode(json, showOnlyMyQuotes = false)
        )
        assertEquals(
            QuoteSummaryMode.ONLY_MY_QUOTES,
            deriveQuoteSummaryMode(json, showOnlyMyQuotes = true)
        )
    }

    @Test
    fun `quote summary keeps legacy fallback only when JSON has no usable quote`() {
        assertEquals(
            QuoteSummaryMode.ONLY_MY_QUOTES,
            deriveQuoteSummaryMode(
                customQuotesJson = "[]",
                showOnlyMyQuotes = false,
                legacyHasCustomQuote = true,
                legacyPreference = "always"
            )
        )
        assertEquals(
            QuoteSummaryMode.DEFAULT,
            deriveQuoteSummaryMode("[]", showOnlyMyQuotes = true)
        )
    }

    @Test
    fun `red badge uses redemption progress instead of level progress`() {
        val progress = calculateProfileProgress(
            level = 4,
            consecutiveSuccessDays = 20,
            hasRedBadge = true,
            activeRedemptionDaysLeft = 1,
            redemptionStreakGoal = 2
        )

        assertEquals(ProfileProgressMode.REDEMPTION, progress.mode)
        assertEquals(0.5f, progress.progress)
        assertEquals(1, progress.completed)
        assertEquals(2, progress.goal)
    }

    @Test
    fun `normal progress uses the current level threshold`() {
        val progress = calculateProfileProgress(
            level = 2,
            consecutiveSuccessDays = 4,
            hasRedBadge = false,
            activeRedemptionDaysLeft = 0,
            redemptionStreakGoal = 2
        )

        assertEquals(ProfileProgressMode.LEVEL, progress.mode)
        assertEquals(4f / 7f, progress.progress)
    }

    @Test
    fun `summary cards stack for narrow screens or large fonts`() {
        assertTrue(shouldStackProfileSummaryCards(maxWidthDp = 340f, fontScale = 1f))
        assertTrue(shouldStackProfileSummaryCards(maxWidthDp = 420f, fontScale = 1.4f))
        assertFalse(shouldStackProfileSummaryCards(maxWidthDp = 420f, fontScale = 1f))
    }

    private fun log(appName: String, packageName: String, timestamp: Long) = StatusLogEntity(
        id = timestamp.toInt(),
        eventType = "RESTRICTION_ADDED",
        timestamp = timestamp,
        appName = appName,
        packageName = packageName,
        details = "Daily limit: 45"
    )
}

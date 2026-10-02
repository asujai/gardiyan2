package com.gardiyan.app

import com.gardiyan.app.data.achievements.Achievements
import com.gardiyan.app.data.achievements.FrameTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {

    @Test
    fun `no streak unlocks nothing and points to the first frame`() {
        val p = Achievements.progress(currentStreak = 0, storedBestStreak = 0)
        assertTrue(p.unlocked.isEmpty())
        assertEquals(FrameTier.SPARK, p.next)
        assertEquals(1, p.daysToNext)
        assertEquals(0f, p.progressToNext)
    }

    @Test
    fun `milestones unlock exactly at their day count`() {
        val p = Achievements.progress(currentStreak = 7, storedBestStreak = 0)
        assertEquals(listOf(FrameTier.SPARK, FrameTier.BRONZE, FrameTier.SILVER_LAUREL), p.unlocked)
        assertEquals(FrameTier.GOLD_SEAL, p.next)
        assertEquals(8, p.daysToNext)
    }

    @Test
    fun `earned frames stay after the streak resets`() {
        val p = Achievements.progress(currentStreak = 0, storedBestStreak = 31)
        assertEquals(31, p.bestStreak)
        assertTrue(p.isUnlocked(FrameTier.MOONSTONE))
        // Bir sonraki hedef kazanılmamış ilk çerçevedir; mesafe güncel seriden ölçülür.
        assertEquals(FrameTier.OBSIDIAN_CROWN, p.next)
        assertEquals(60, p.daysToNext)
    }

    @Test
    fun `best streak never decreases`() {
        val p = Achievements.progress(currentStreak = 4, storedBestStreak = 12)
        assertEquals(12, p.bestStreak)
        val q = Achievements.progress(currentStreak = 20, storedBestStreak = 12)
        assertEquals(20, q.bestStreak)
    }

    @Test
    fun `all frames earned leaves no next target`() {
        val p = Achievements.progress(currentStreak = 400, storedBestStreak = 0)
        assertEquals(FrameTier.entries.size, p.unlocked.size)
        assertNull(p.next)
        assertEquals(1f, p.progressToNext)
    }

    @Test
    fun `equipped falls back to highest earned frame`() {
        val p = Achievements.progress(currentStreak = 16, storedBestStreak = 0)
        assertEquals(FrameTier.GOLD_SEAL, Achievements.resolveEquipped(p, chosen = null))
        assertEquals(FrameTier.BRONZE, Achievements.resolveEquipped(p, chosen = FrameTier.BRONZE))
        // Kilitli bir çerçeve seçili kalamaz.
        assertEquals(FrameTier.GOLD_SEAL, Achievements.resolveEquipped(p, chosen = FrameTier.STOIC))
    }

    @Test
    fun `only the highest uncelebrated frame is celebrated first`() {
        val p = Achievements.progress(currentStreak = 30, storedBestStreak = 0)
        val pending = Achievements.pendingCelebrations(p, seen = setOf(FrameTier.SPARK))
        assertEquals(FrameTier.MOONSTONE, pending.first())
        assertTrue(FrameTier.SPARK !in pending)
    }
}

package com.gardiyan.app

import com.gardiyan.app.data.repository.GuardianRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Kısıtlama kurulurken alınan UsageStats başlangıç çizgisinin normalizasyonu.
 *
 * Gerçek olay (25 Ağustos 2026): Facebook'a 1 dakikalık limit konduğunda
 * UsageStats o an 0 ms döndü, birkaç dakika sonra aynı gün için 82.721 ms
 * raporladı. Başlangıç çizgisi 0 kaydedildiği için günün ESKİ kullanımı da yeni
 * limite yazıldı ve kullanıcı neredeyse anında kilitlendi.
 */
class UsageStatsBaselineTest {

    @Test
    fun `zero reading is treated as unknown`() {
        assertEquals(
            GuardianRepository.UNKNOWN_USAGE_STATS_BASELINE,
            GuardianRepository.normalizeInitialUsageStatsBaseline(0L)
        )
    }

    @Test
    fun `null reading is treated as unknown`() {
        assertEquals(
            GuardianRepository.UNKNOWN_USAGE_STATS_BASELINE,
            GuardianRepository.normalizeInitialUsageStatsBaseline(null)
        )
    }

    @Test
    fun `negative reading is treated as unknown`() {
        assertEquals(
            GuardianRepository.UNKNOWN_USAGE_STATS_BASELINE,
            GuardianRepository.normalizeInitialUsageStatsBaseline(-5L)
        )
    }

    @Test
    fun `real reading is preserved`() {
        assertEquals(82_721L, GuardianRepository.normalizeInitialUsageStatsBaseline(82_721L))
        assertEquals(1L, GuardianRepository.normalizeInitialUsageStatsBaseline(1L))
    }
}

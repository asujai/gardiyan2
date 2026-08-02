package com.gardiyan.app

import com.gardiyan.app.data.model.DayStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Disiplin Özeti / detay kutularının ortak renk (durum) mantığının birim testleri.
 * Hem 21 günlük özet hem 100 günlük detay ekranı DayStatus.evaluate kullanır.
 */
class DayStatusTest {

    @Test
    fun `future day is gray none`() {
        val status = DayStatus.evaluate(
            isFuture = true, isToday = false, hasActiveTargets = true,
            todayHasViolation = false, dayHasSuccessLog = false, dayHasFailureLog = false
        )
        assertEquals(DayStatus.NONE, status)
    }

    @Test
    fun `failure log makes the day red`() {
        val status = DayStatus.evaluate(
            isFuture = false, isToday = false, hasActiveTargets = true,
            todayHasViolation = false, dayHasSuccessLog = false, dayHasFailureLog = true
        )
        assertEquals(DayStatus.FAILURE, status)
    }

    @Test
    fun `success log makes the day green`() {
        val status = DayStatus.evaluate(
            isFuture = false, isToday = false, hasActiveTargets = true,
            todayHasViolation = false, dayHasSuccessLog = true, dayHasFailureLog = false
        )
        assertEquals(DayStatus.SUCCESS, status)
    }

    @Test
    fun `today with active targets and no usage is green`() {
        // Bugün korunan uygulamalara hiç girilmedi (log yok), ihlal de yok -> yeşil.
        val status = DayStatus.evaluate(
            isFuture = false, isToday = true, hasActiveTargets = true,
            todayHasViolation = false, dayHasSuccessLog = false, dayHasFailureLog = false
        )
        assertEquals(DayStatus.SUCCESS, status)
    }

    @Test
    fun `today with violation is red`() {
        val status = DayStatus.evaluate(
            isFuture = false, isToday = true, hasActiveTargets = true,
            todayHasViolation = true, dayHasSuccessLog = false, dayHasFailureLog = false
        )
        assertEquals(DayStatus.FAILURE, status)
    }

    @Test
    fun `today without any active targets is gray`() {
        val status = DayStatus.evaluate(
            isFuture = false, isToday = true, hasActiveTargets = false,
            todayHasViolation = false, dayHasSuccessLog = false, dayHasFailureLog = false
        )
        assertEquals(DayStatus.NONE, status)
    }

    @Test
    fun `past day with an active target and no violation is green`() {
        val status = DayStatus.evaluate(
            isFuture = false, isToday = false, hasActiveTargets = true,
            todayHasViolation = false, dayHasSuccessLog = false, dayHasFailureLog = false
        )
        assertEquals(DayStatus.SUCCESS, status)
    }

    @Test
    fun `failure log takes priority over success log on the same day`() {
        val status = DayStatus.evaluate(
            isFuture = false, isToday = true, hasActiveTargets = true,
            todayHasViolation = false, dayHasSuccessLog = true, dayHasFailureLog = true
        )
        assertEquals(DayStatus.FAILURE, status)
    }
}

package com.gardiyan.app

import com.gardiyan.app.data.model.RestrictionSchedule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestrictionScheduleTest {

    @Test
    fun `day-only restriction is active throughout a selected day`() {
        assertTrue(
            RestrictionSchedule.isActiveAt(
                activeDays = "Pzt,Çar",
                activeWindowEnabled = false,
                activeStartMinutes = 0,
                activeEndMinutes = 0,
                currentDayLabel = "Pzt",
                previousDayLabel = "Paz",
                minuteOfDay = 22 * 60
            )
        )
    }

    @Test
    fun `daytime window excludes usage after its end`() {
        assertFalse(
            RestrictionSchedule.isActiveAt(
                activeDays = "Pzt",
                activeWindowEnabled = true,
                activeStartMinutes = 9 * 60,
                activeEndMinutes = 12 * 60,
                currentDayLabel = "Pzt",
                previousDayLabel = "Paz",
                minuteOfDay = 12 * 60
            )
        )
    }

    @Test
    fun `overnight window remains active on the following morning`() {
        assertTrue(
            RestrictionSchedule.isActiveAt(
                activeDays = "Pzt",
                activeWindowEnabled = true,
                activeStartMinutes = 22 * 60,
                activeEndMinutes = 6 * 60,
                currentDayLabel = "Sal",
                previousDayLabel = "Pzt",
                minuteOfDay = 2 * 60
            )
        )
    }

    @Test
    fun `overnight window does not borrow an unselected previous day`() {
        assertFalse(
            RestrictionSchedule.isActiveAt(
                activeDays = "Sal",
                activeWindowEnabled = true,
                activeStartMinutes = 22 * 60,
                activeEndMinutes = 6 * 60,
                currentDayLabel = "Sal",
                previousDayLabel = "Pzt",
                minuteOfDay = 2 * 60
            )
        )
    }

    @Test
    fun `portuguese weekdays match calendar day labels correctly`() {
        // Seg-Sex (Monday-Friday in Portuguese)
        val activeDays = "Seg, Ter, Qua, Qui, Sex"
        // Should be active on Pzt (Monday) at 10:00
        assertTrue(
            RestrictionSchedule.isActiveAt(
                activeDays = activeDays,
                activeWindowEnabled = true,
                activeStartMinutes = 8 * 60,
                activeEndMinutes = 17 * 60,
                currentDayLabel = "Pzt",
                previousDayLabel = "Paz",
                minuteOfDay = 10 * 60
            )
        )
        // Should be active on Cum (Friday) at 16:59
        assertTrue(
            RestrictionSchedule.isActiveAt(
                activeDays = activeDays,
                activeWindowEnabled = true,
                activeStartMinutes = 8 * 60,
                activeEndMinutes = 17 * 60,
                currentDayLabel = "Cum",
                previousDayLabel = "Per",
                minuteOfDay = 16 * 60 + 59
            )
        )
        // Should NOT be active on Cmt (Saturday) or Paz (Sunday)
        assertFalse(
            RestrictionSchedule.isActiveAt(
                activeDays = activeDays,
                activeWindowEnabled = true,
                activeStartMinutes = 8 * 60,
                activeEndMinutes = 17 * 60,
                currentDayLabel = "Cmt",
                previousDayLabel = "Cum",
                minuteOfDay = 10 * 60
            )
        )
        // Should NOT be active outside 08:00 - 17:00 on weekdays
        assertFalse(
            RestrictionSchedule.isActiveAt(
                activeDays = activeDays,
                activeWindowEnabled = true,
                activeStartMinutes = 8 * 60,
                activeEndMinutes = 17 * 60,
                currentDayLabel = "Pzt",
                previousDayLabel = "Paz",
                minuteOfDay = 17 * 60 + 30
            )
        )
    }

    @Test
    fun `english weekdays match correctly`() {
        val activeDays = "Mon,Tue,Wed,Thu,Fri"
        assertTrue(
            RestrictionSchedule.isActiveAt(
                activeDays = activeDays,
                activeWindowEnabled = false,
                activeStartMinutes = 0,
                activeEndMinutes = 0,
                currentDayLabel = "Çar",
                previousDayLabel = "Sal",
                minuteOfDay = 12 * 60
            )
        )
        assertFalse(
            RestrictionSchedule.isActiveAt(
                activeDays = activeDays,
                activeWindowEnabled = false,
                activeStartMinutes = 0,
                activeEndMinutes = 0,
                currentDayLabel = "Paz",
                previousDayLabel = "Cmt",
                minuteOfDay = 12 * 60
            )
        )
    }
}

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
}

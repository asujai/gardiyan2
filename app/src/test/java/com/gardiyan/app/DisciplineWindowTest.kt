package com.gardiyan.app

import com.gardiyan.app.data.model.DisciplineWindow
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ana ekran 21 kutucuklu Disiplin Özeti'nin kayan pencere mantığının testleri.
 * İlk 21 gün boyunca pencere serinin 1. gününe sabitlenir (bugün 1..21 ilerler),
 * 21. günden sonra "son 21 gün" gibi kayar (bugün sağ alt = index 20).
 */
class DisciplineWindowTest {

    private fun lastVisibleDay(day: Int) =
        DisciplineWindow.dayNumberForCell(DisciplineWindow.SUMMARY_DAYS - 1, day)

    @Test
    fun `day 1 - today is first cell, window shows days 1 to 21`() {
        assertEquals(1, DisciplineWindow.firstVisibleDayNumber(1))
        assertEquals(0, DisciplineWindow.todayCellIndex(1))
        assertEquals(21, lastVisibleDay(1))
        // bugün (gün 1) 0. kutuda
        assertEquals(1, DisciplineWindow.dayNumberForCell(0, 1))
    }

    @Test
    fun `day 2 - today is second cell`() {
        assertEquals(1, DisciplineWindow.firstVisibleDayNumber(2))
        assertEquals(1, DisciplineWindow.todayCellIndex(2))
        assertEquals(2, DisciplineWindow.dayNumberForCell(1, 2))
    }

    @Test
    fun `day 3 - today is third cell`() {
        assertEquals(1, DisciplineWindow.firstVisibleDayNumber(3))
        assertEquals(2, DisciplineWindow.todayCellIndex(3))
        assertEquals(3, DisciplineWindow.dayNumberForCell(2, 3))
    }

    @Test
    fun `day 21 - today is last cell, window shows days 1 to 21`() {
        assertEquals(1, DisciplineWindow.firstVisibleDayNumber(21))
        assertEquals(20, DisciplineWindow.todayCellIndex(21))
        assertEquals(21, lastVisibleDay(21))
        assertEquals(21, DisciplineWindow.dayNumberForCell(20, 21))
    }

    @Test
    fun `day 22 - window slides to days 2 to 22, today stays last cell`() {
        assertEquals(2, DisciplineWindow.firstVisibleDayNumber(22))
        assertEquals(20, DisciplineWindow.todayCellIndex(22))
        assertEquals(22, lastVisibleDay(22))
        // sol üst artık gün 2
        assertEquals(2, DisciplineWindow.dayNumberForCell(0, 22))
        assertEquals(22, DisciplineWindow.dayNumberForCell(20, 22))
    }

    @Test
    fun `day 50 - window shows days 30 to 50, today stays last cell`() {
        assertEquals(30, DisciplineWindow.firstVisibleDayNumber(50))
        assertEquals(20, DisciplineWindow.todayCellIndex(50))
        assertEquals(50, lastVisibleDay(50))
        assertEquals(30, DisciplineWindow.dayNumberForCell(0, 50))
        assertEquals(50, DisciplineWindow.dayNumberForCell(20, 50))
    }

    @Test
    fun `today always lands on its own computed cell index`() {
        for (day in 1..120) {
            val cell = DisciplineWindow.todayCellIndex(day)
            assertEquals(day, DisciplineWindow.dayNumberForCell(cell, day))
        }
    }
}

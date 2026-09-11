package com.gardiyan.app

import com.gardiyan.app.data.usage.UsageEventAggregator
import com.gardiyan.app.data.usage.UsageEventAggregator.Event
import com.gardiyan.app.data.usage.UsageEventAggregator.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageEventAggregatorTest {

    private val minute = 60_000L
    private val dayStart = 1_000_000_000L

    @Test
    fun yesterdayUsageDoesNotLeakIntoToday() {
        // Dün 57 dk YouTube, bugün 2 dk YouTube + 3 dk Clash Royale.
        val events = sequenceOf(
            Event(dayStart - 60 * minute, "youtube", Kind.FOREGROUND),
            Event(dayStart - 3 * minute, "youtube", Kind.BACKGROUND),
            Event(dayStart + 1 * minute, "youtube", Kind.FOREGROUND),
            Event(dayStart + 3 * minute, "clash", Kind.FOREGROUND), // youtube kapanmadan başkası açıldı
            Event(dayStart + 6 * minute, "clash", Kind.BACKGROUND)
        )
        val totals = UsageEventAggregator.aggregate(events, dayStart, dayStart + 6 * minute)
        assertEquals(2 * minute, totals["youtube"])
        assertEquals(3 * minute, totals["clash"])
    }

    @Test
    fun sessionSpanningMidnightCountsOnlyTodayPart() {
        val events = sequenceOf(Event(dayStart - 10 * minute, "youtube", Kind.FOREGROUND))
        val totals = UsageEventAggregator.aggregate(events, dayStart, dayStart + 4 * minute)
        assertEquals(4 * minute, totals["youtube"])
    }

    @Test
    fun staleBackgroundOfOtherPackageIsIgnored() {
        val events = sequenceOf(
            Event(dayStart + 1 * minute, "youtube", Kind.FOREGROUND),
            Event(dayStart + 2 * minute, "clash", Kind.BACKGROUND)
        )
        val totals = UsageEventAggregator.aggregate(events, dayStart, dayStart + 5 * minute)
        assertEquals(4 * minute, totals["youtube"])
        assertNull(totals["clash"])
    }
}

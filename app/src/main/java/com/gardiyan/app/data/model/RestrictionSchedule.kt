package com.gardiyan.app.data.model

import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import java.util.Calendar

/** Shared day/time eligibility rules used by both UI and protection services. */
object RestrictionSchedule {
    val dayLabels = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    fun isActiveAt(
        activeDays: String,
        activeWindowEnabled: Boolean,
        activeStartMinutes: Int,
        activeEndMinutes: Int,
        currentDayLabel: String,
        previousDayLabel: String,
        minuteOfDay: Int
    ): Boolean {
        val days = activeDays.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val currentDaySelected = days.isEmpty() || currentDayLabel in days
        if (!activeWindowEnabled) return currentDaySelected

        val start = activeStartMinutes.coerceIn(0, 1439)
        val end = activeEndMinutes.coerceIn(0, 1439)
        val minute = minuteOfDay.coerceIn(0, 1439)

        // Equal endpoints represent a full-day window on selected days.
        if (start == end) return currentDaySelected
        if (start < end) return currentDaySelected && minute in start until end

        // Overnight window: late portion belongs to today, early portion to yesterday.
        val previousDaySelected = days.isEmpty() || previousDayLabel in days
        return (currentDaySelected && minute >= start) || (previousDaySelected && minute < end)
    }

    fun previousDayLabel(currentDayLabel: String): String {
        val index = dayLabels.indexOf(currentDayLabel)
        return if (index < 0) "" else dayLabels[(index + dayLabels.size - 1) % dayLabels.size]
    }

    fun dayLabel(calendar: Calendar): String = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Pzt"
        Calendar.TUESDAY -> "Sal"
        Calendar.WEDNESDAY -> "Çar"
        Calendar.THURSDAY -> "Per"
        Calendar.FRIDAY -> "Cum"
        Calendar.SATURDAY -> "Cmt"
        Calendar.SUNDAY -> "Paz"
        else -> ""
    }
}

fun RestrictedAppEntity.isScheduledAt(timestampMillis: Long): Boolean {
    if (!isActive) return false
    val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    val currentDay = RestrictionSchedule.dayLabel(calendar)
    return RestrictionSchedule.isActiveAt(
        activeDays = activeDays,
        activeWindowEnabled = activeWindowEnabled,
        activeStartMinutes = activeStartMinutes,
        activeEndMinutes = activeEndMinutes,
        currentDayLabel = currentDay,
        previousDayLabel = RestrictionSchedule.previousDayLabel(currentDay),
        minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    )
}

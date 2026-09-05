package com.gardiyan.app.data.model

import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import java.util.Calendar

/** Shared day/time eligibility rules used by both UI and protection services. */
object RestrictionSchedule {
    val dayLabels = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    fun normalizeToCalendarDay(raw: String): Int? {
        val clean = raw.trim().lowercase(java.util.Locale.ROOT)
        return when {
            clean in listOf("pzt", "pazartesi", "mon", "monday", "seg", "segunda", "lun", "lunes", "lundi", "mo", "montag", "1") -> Calendar.MONDAY
            clean in listOf("sal", "salı", "sali", "tue", "tuesday", "ter", "terça", "terca", "mar", "martes", "mardi", "di", "dienstag", "2") -> Calendar.TUESDAY
            clean in listOf("çar", "car", "çarşamba", "carsamba", "wed", "wednesday", "qua", "quarta", "mié", "mie", "miércoles", "miercoles", "mer", "mercredi", "mi", "mittwoch", "3") -> Calendar.WEDNESDAY
            clean in listOf("per", "perşembe", "persembe", "thu", "thursday", "qui", "quinta", "jue", "jueves", "jeu", "jeudi", "do", "donnerstag", "4") -> Calendar.THURSDAY
            clean in listOf("cum", "cuma", "fri", "friday", "sex", "sexta", "vie", "viernes", "ven", "vendredi", "fr", "freitag", "5") -> Calendar.FRIDAY
            clean in listOf("cmt", "cumartesi", "sat", "saturday", "sáb", "sab", "sábado", "sabado", "sam", "samedi", "sa", "samstag", "6") -> Calendar.SATURDAY
            clean in listOf("paz", "pazar", "sun", "sunday", "dom", "domingo", "dim", "dimanche", "so", "sonntag", "7") -> Calendar.SUNDAY
            else -> null
        }
    }

    fun isDaySelected(selectedDays: Set<String>, dayLabel: String): Boolean {
        if (selectedDays.isEmpty()) return true
        if (dayLabel in selectedDays) return true
        val targetCalDay = normalizeToCalendarDay(dayLabel) ?: return false
        return selectedDays.any { normalizeToCalendarDay(it) == targetCalDay }
    }

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
        val currentDaySelected = isDaySelected(days, currentDayLabel)
        if (!activeWindowEnabled) return currentDaySelected

        val start = activeStartMinutes.coerceIn(0, 1439)
        val end = activeEndMinutes.coerceIn(0, 1439)
        val minute = minuteOfDay.coerceIn(0, 1439)

        // Equal endpoints represent a full-day window on selected days.
        if (start == end) return currentDaySelected
        if (start < end) return currentDaySelected && minute in start until end

        // Overnight window: late portion belongs to today, early portion to yesterday.
        val previousDaySelected = isDaySelected(days, previousDayLabel)
        return (currentDaySelected && minute >= start) || (previousDaySelected && minute < end)
    }

    fun previousDayLabel(currentDayLabel: String): String {
        val index = dayLabels.indexOf(currentDayLabel)
        if (index >= 0) return dayLabels[(index + dayLabels.size - 1) % dayLabels.size]
        val calDay = normalizeToCalendarDay(currentDayLabel)
        return when (calDay) {
            Calendar.MONDAY -> "Paz"
            Calendar.TUESDAY -> "Pzt"
            Calendar.WEDNESDAY -> "Sal"
            Calendar.THURSDAY -> "Çar"
            Calendar.FRIDAY -> "Per"
            Calendar.SATURDAY -> "Cum"
            Calendar.SUNDAY -> "Cmt"
            else -> ""
        }
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

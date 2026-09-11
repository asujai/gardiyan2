package com.gardiyan.app.data.usage

import android.app.usage.UsageEvents
import android.os.Build

/**
 * Olay bazlı ön plan süresi toplayıcı.
 *
 * queryAndAggregateUsageStats gün kovasını (bucket) döndürür ve gece yarısında
 * hemen devrilmez; 00:06'da dünkü toplam "bugün" diye gelir. Olay günlüğü ise
 * anlıktır: [startMillis, endMillis) aralığında hangi paket ön plandaysa süre
 * ona yazılır. Aynı anda tek paket ön planda olabilir.
 */
object UsageEventAggregator {

    enum class Kind { FOREGROUND, BACKGROUND, OTHER }

    data class Event(val timestamp: Long, val packageName: String, val kind: Kind)

    fun aggregate(events: Sequence<Event>, startMillis: Long, endMillis: Long): Map<String, Long> {
        if (endMillis <= startMillis) return emptyMap()
        val totals = HashMap<String, Long>()
        var current: String? = null
        var lastTimestamp = startMillis

        fun close(until: Long) {
            val pkg = current ?: return
            val clamped = until.coerceIn(startMillis, endMillis)
            if (clamped > lastTimestamp) totals[pkg] = (totals[pkg] ?: 0L) + (clamped - lastTimestamp)
            lastTimestamp = clamped
        }

        for (e in events) {
            when (e.kind) {
                Kind.FOREGROUND -> {
                    close(e.timestamp)
                    current = e.packageName
                    lastTimestamp = e.timestamp.coerceIn(startMillis, endMillis)
                }
                Kind.BACKGROUND -> if (e.packageName == current) {
                    close(e.timestamp)
                    current = null
                }
                Kind.OTHER -> Unit
            }
        }
        close(endMillis)
        return totals
    }

    fun kindOf(eventType: Int): Kind = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        when (eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> Kind.FOREGROUND
            UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> Kind.BACKGROUND
            else -> Kind.OTHER
        }
    } else {
        @Suppress("DEPRECATION")
        when (eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> Kind.FOREGROUND
            UsageEvents.Event.MOVE_TO_BACKGROUND -> Kind.BACKGROUND
            else -> Kind.OTHER
        }
    }

    /** Olay günlüğünü lazy sırayla akıtır. Aralık öncesi olaylar durum tespiti için okunur. */
    fun stream(usageEvents: UsageEvents): Sequence<Event> = sequence {
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            yield(Event(event.timeStamp, event.packageName ?: continue, kindOf(event.eventType)))
        }
    }
}

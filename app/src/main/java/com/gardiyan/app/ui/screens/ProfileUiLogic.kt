package com.gardiyan.app.ui.screens

import com.gardiyan.app.data.local.entity.StatusLogEntity
import kotlin.math.abs
import org.json.JSONArray

internal const val TIMELINE_DUPLICATE_WINDOW_MILLIS = 5_000L

internal fun deduplicateTimelineLogs(
    logs: List<StatusLogEntity>,
    maxItems: Int = 100,
    duplicateWindowMillis: Long = TIMELINE_DUPLICATE_WINDOW_MILLIS
): List<StatusLogEntity> {
    val result = ArrayList<StatusLogEntity>(logs.size.coerceAtMost(maxItems))
    var previous: StatusLogEntity? = null

    for (log in logs) {
        val last = previous
        val isDuplicate = last != null &&
            last.eventType == log.eventType &&
            last.timelineIdentity() == log.timelineIdentity() &&
            last.details.trim() == log.details.trim() &&
            abs(last.timestamp - log.timestamp) <= duplicateWindowMillis

        if (!isDuplicate) {
            result += log
            if (result.size == maxItems) break
        }
        previous = log
    }
    return result
}

private fun StatusLogEntity.timelineIdentity(): String {
    val stablePackage = packageName.trim()
    return if (stablePackage.isNotEmpty()) {
        "package:${stablePackage.lowercase()}"
    } else {
        "legacy:${appName.trim().lowercase()}"
    }
}

data class CustomQuoteItem(
    val id: String,
    val text: String,
    val author: String,
    val isSelected: Boolean
)

internal fun parseCustomQuotes(json: String): List<CustomQuoteItem> {
    return runCatching {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val text = item.optString("text", "").trim()
                if (text.isEmpty()) continue
                add(
                    CustomQuoteItem(
                        id = item.optString("id", ""),
                        text = text,
                        author = item.optString("author", "").trim(),
                        isSelected = item.optBoolean("isSelected", true)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

internal enum class QuoteSummaryMode {
    DEFAULT,
    MIXED,
    ONLY_MY_QUOTES
}

internal fun deriveQuoteSummaryMode(
    customQuotesJson: String,
    showOnlyMyQuotes: Boolean,
    legacyHasCustomQuote: Boolean = false,
    legacyPreference: String = "mix"
): QuoteSummaryMode {
    val customQuotes = parseCustomQuotes(customQuotesJson)
    val selectedQuotes = customQuotes.filter { it.isSelected }.ifEmpty { customQuotes }
    if (selectedQuotes.isNotEmpty()) {
        return if (showOnlyMyQuotes) QuoteSummaryMode.ONLY_MY_QUOTES else QuoteSummaryMode.MIXED
    }
    if (legacyHasCustomQuote) {
        return if (legacyPreference == "always") QuoteSummaryMode.ONLY_MY_QUOTES else QuoteSummaryMode.MIXED
    }
    return QuoteSummaryMode.DEFAULT
}

internal enum class ProfileProgressMode {
    LEVEL,
    REDEMPTION
}

internal data class ProfileProgressState(
    val mode: ProfileProgressMode,
    val progress: Float,
    val completed: Int,
    val goal: Int
)

internal fun calculateProfileProgress(
    level: Int,
    consecutiveSuccessDays: Int,
    hasRedBadge: Boolean,
    activeRedemptionDaysLeft: Int,
    redemptionStreakGoal: Int
): ProfileProgressState {
    val safeGoal = redemptionStreakGoal.coerceAtLeast(1)
    if (hasRedBadge && activeRedemptionDaysLeft > 0) {
        val completed = (safeGoal - activeRedemptionDaysLeft).coerceIn(0, safeGoal)
        return ProfileProgressState(
            mode = ProfileProgressMode.REDEMPTION,
            progress = completed.toFloat() / safeGoal,
            completed = completed,
            goal = safeGoal
        )
    }

    val levelGoal = when (level) {
        1 -> 3
        2 -> 7
        3 -> 15
        4 -> 30
        5 -> 60
        else -> 1
    }
    val progress = if (level >= 6) 1f else consecutiveSuccessDays.toFloat() / levelGoal
    return ProfileProgressState(
        mode = ProfileProgressMode.LEVEL,
        progress = progress.coerceIn(0f, 1f),
        completed = consecutiveSuccessDays.coerceAtLeast(0),
        goal = levelGoal
    )
}

internal fun shouldStackProfileSummaryCards(maxWidthDp: Float, fontScale: Float): Boolean =
    maxWidthDp < 360f || fontScale >= 1.3f

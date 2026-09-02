package com.gardiyan.app.data.timeline

private const val RESTRICTION_LOG_DETAILS_PREFIX = "restriction:v1|"

internal data class RestrictionLogDetails(
    val dailyLimitMinutes: Int,
    val restrictionName: String
)

internal fun encodeRestrictionLogDetails(
    dailyLimitMinutes: Int,
    restrictionName: String
): String = "$RESTRICTION_LOG_DETAILS_PREFIX${dailyLimitMinutes.coerceAtLeast(0)}|${restrictionName.trim()}"

internal fun parseRestrictionLogDetails(details: String): RestrictionLogDetails? {
    if (!details.startsWith(RESTRICTION_LOG_DETAILS_PREFIX)) return null
    val parts = details.removePrefix(RESTRICTION_LOG_DETAILS_PREFIX).split("|", limit = 2)
    val dailyLimitMinutes = parts.firstOrNull()?.toIntOrNull() ?: return null
    return RestrictionLogDetails(
        dailyLimitMinutes = dailyLimitMinutes.coerceAtLeast(0),
        restrictionName = parts.getOrElse(1) { "" }.trim()
    )
}

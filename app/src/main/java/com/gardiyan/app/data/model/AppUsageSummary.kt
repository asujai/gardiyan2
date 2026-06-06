package com.gardiyan.app.data.model

enum class UsagePeriod(val label: String) {
    DAILY("Günlük"),
    WEEKLY("Haftalık"),
    MONTHLY("Aylık"),
    AVERAGE("Ortalama")
}

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val usageMillis: Long
)

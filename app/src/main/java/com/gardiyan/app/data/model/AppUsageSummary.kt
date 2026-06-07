package com.gardiyan.app.data.model

import com.gardiyan.app.R

enum class UsagePeriod(val labelResId: Int) {
    DAILY(R.string.period_daily),
    WEEKLY(R.string.period_weekly),
    MONTHLY(R.string.period_monthly),
    AVERAGE(R.string.period_average)
}

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val usageMillis: Long
)

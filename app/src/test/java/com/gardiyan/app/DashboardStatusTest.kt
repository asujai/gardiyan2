package com.gardiyan.app

import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.ui.screens.getExceededPackageNames
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardStatusTest {

    @Test
    fun `dashboard only marks restrictions failed by Gardiyan as exceeded`() {
        val protected = restriction(
            packageName = "protected.app",
            remainingSeconds = 10,
            isFailed = false
        )
        val failed = restriction(
            packageName = "failed.app",
            remainingSeconds = 10,
            isFailed = true
        )
        val exhausted = restriction(
            packageName = "exhausted.app",
            remainingSeconds = 0,
            isFailed = false
        )

        assertEquals(
            setOf("failed.app", "exhausted.app"),
            getExceededPackageNames(listOf(protected, failed, exhausted))
        )
    }

    private fun restriction(
        packageName: String,
        remainingSeconds: Int,
        isFailed: Boolean
    ) = RestrictedAppEntity(
        packageName = packageName,
        appName = packageName,
        dailyLimitMinutes = 1,
        remainingMinutesToday = remainingSeconds / 60,
        remainingSecondsToday = remainingSeconds,
        isFailed = isFailed
    )
}

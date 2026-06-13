package com.gardiyan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.gardiyan.app.data.model.AppUsageSummary
import com.gardiyan.app.data.model.UsagePeriod
import com.gardiyan.app.ui.components.UsageRankingSection
import com.gardiyan.app.ui.theme.DashboardIvory
import com.gardiyan.app.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class UsageRankingVisualCheckTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun usageRankingVisualCheck() {
        val items = listOf(
            AppUsageSummary("com.instagram.android", "Instagram", 6_120_000),
            AppUsageSummary("com.zhiliaoapp.musically", "TikTok", 3_480_000),
            AppUsageSummary("com.google.android.youtube", "YouTube", 2_640_000),
            AppUsageSummary("com.whatsapp", "WhatsApp", 1_860_000),
            AppUsageSummary("com.twitter.android", "X / Twitter", 1_320_000),
            AppUsageSummary("com.android.chrome", "Chrome", 1_080_000)
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DashboardIvory)
                        .padding(20.dp)
                ) {
                    UsageRankingSection(
                        selectedPeriod = UsagePeriod.DAILY,
                        onPeriodSelected = {},
                        usageItems = items,
                        appLimits = mapOf(
                            "com.instagram.android" to 60,
                            "com.google.android.youtube" to 120
                        ),
                        exceededPackages = setOf("com.instagram.android")
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/usage-ranking-visual-check.png"
        )
    }
}

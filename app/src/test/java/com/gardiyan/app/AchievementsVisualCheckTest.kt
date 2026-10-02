package com.gardiyan.app

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.onRoot
import com.gardiyan.app.data.achievements.Achievements
import com.gardiyan.app.data.achievements.FrameTier
import com.gardiyan.app.ui.screens.AchievementsContent
import com.gardiyan.app.ui.theme.AppThemeMode
import com.gardiyan.app.ui.theme.AppThemePalette
import com.gardiyan.app.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Başarılar ekranının görsel kontrolü: kazanılmış ve kilitli çerçeveler birlikte. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AchievementsVisualCheckTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun render(mode: AppThemeMode, palette: AppThemePalette, file: String) {
        val progress = Achievements.progress(currentStreak = 42, storedBestStreak = 70)
        // Tema, açılışta tercihlerden okunur; test paletini oraya yaz.
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("theme_mode", mode.name)
            .putString("theme_palette", palette.name)
            .commit()
        composeTestRule.setContent {
            MyApplicationTheme {
                AchievementsContent(
                    progress = progress,
                    equipped = FrameTier.OBSIDIAN_CROWN,
                    onEquip = {},
                    onBack = {},
                    animated = false
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$file")
    }

    @Test
    fun achievementsLightBlue() = render(AppThemeMode.LIGHT, AppThemePalette.BLUE, "achievements-light.png")

    @Test
    fun achievementsPremiumDark() = render(AppThemeMode.DARK, AppThemePalette.PREMIUM_DARK, "achievements-premium-dark.png")
}

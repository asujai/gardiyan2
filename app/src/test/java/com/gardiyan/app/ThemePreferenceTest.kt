package com.gardiyan.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gardiyan.app.ui.theme.AppThemeMode
import com.gardiyan.app.ui.theme.AppThemePalette
import com.gardiyan.app.ui.theme.currentThemeMode
import com.gardiyan.app.ui.theme.currentThemePalette
import com.gardiyan.app.ui.theme.updateThemeMode
import com.gardiyan.app.ui.theme.updateThemePalette
import com.gardiyan.app.ui.theme.MutedGray
import com.gardiyan.app.ui.theme.DashboardMuted
import com.gardiyan.app.ui.theme.updateAppColors
import androidx.compose.ui.graphics.Color
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemePreferenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE).edit().clear().commit()
        currentThemeMode.value = AppThemeMode.SYSTEM
        currentThemePalette.value = AppThemePalette.PREMIUM_DARK
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `light mode replaces premium dark with a compatible palette`() {
        updateThemeMode(context, AppThemeMode.LIGHT)

        assertEquals(AppThemeMode.LIGHT, currentThemeMode.value)
        assertEquals(AppThemePalette.BLUE, currentThemePalette.value)
    }

    @Test
    fun `premium dark palette forces dark mode`() {
        currentThemePalette.value = AppThemePalette.BLUE
        currentThemeMode.value = AppThemeMode.LIGHT

        updateThemePalette(context, AppThemePalette.PREMIUM_DARK)

        assertEquals(AppThemeMode.DARK, currentThemeMode.value)
        assertEquals(AppThemePalette.PREMIUM_DARK, currentThemePalette.value)
    }

    @Test
    fun `premium dark uses accessible muted text instead of the old low contrast gray`() {
        updateAppColors(isDark = true, palette = AppThemePalette.PREMIUM_DARK)

        assertEquals(Color(0xFFA3A3A3), MutedGray)
        assertEquals(Color(0xFFA3A3A3), DashboardMuted)
    }
}

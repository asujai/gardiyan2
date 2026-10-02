package com.gardiyan.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppThemePalette {
    BLUE, MONOCHROME, RED, PREMIUM_DARK
}

// Global olarak tema durumunu tutan MutableState'ler
val currentThemeMode = mutableStateOf(AppThemeMode.LIGHT)
val currentThemePalette = mutableStateOf(AppThemePalette.BLUE)

private val LimitraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    // Senkron olarak SharedPreferences'tan oku ve global state'leri güncelle
    remember(context) {
        val prefs = context.getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE)
        val savedMode = prefs.getString("theme_mode", AppThemeMode.LIGHT.name) ?: AppThemeMode.LIGHT.name
        val restoredMode = runCatching { AppThemeMode.valueOf(savedMode) }.getOrDefault(AppThemeMode.LIGHT)

        val savedPalette = prefs.getString("theme_palette", AppThemePalette.BLUE.name) ?: AppThemePalette.BLUE.name
        val restoredPalette = runCatching { AppThemePalette.valueOf(savedPalette) }.getOrDefault(AppThemePalette.BLUE)

        currentThemeMode.value = restoredMode
        currentThemePalette.value = if (restoredMode != AppThemeMode.DARK && restoredPalette == AppThemePalette.PREMIUM_DARK) {
            prefs.edit().putString("theme_palette", AppThemePalette.BLUE.name).apply()
            AppThemePalette.BLUE
        } else {
            restoredPalette
        }
        true
    }

    val isDark = when (currentThemeMode.value) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val currentPalette = currentThemePalette.value

    // Renkleri ilk kareden önce uygula; aksi halde açılışta bir kare yanlış palet görünür.
    remember(isDark, currentPalette) {
        updateAppColors(isDark, currentPalette)
        true
    }

    val darkUi = IsDarkUi
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !darkUi
            controller.isAppearanceLightNavigationBars = !darkUi
        }
    }

    val colorScheme = if (darkUi) {
        darkColorScheme(
            primary = CopperAccent,
            onPrimary = OnAccent,
            primaryContainer = SoftCopper,
            onPrimaryContainer = PureBlack,
            secondary = WineAccent,
            background = MatteSurface,
            onBackground = PureBlack,
            surface = DarkCharcoal,
            onSurface = PureBlack,
            surfaceVariant = WarmGray,
            onSurfaceVariant = MutedGray,
            surfaceContainerLowest = MatteSurface,
            surfaceContainerLow = DarkCharcoal,
            surfaceContainer = DarkCharcoal,
            surfaceContainerHigh = DarkCharcoal,
            surfaceContainerHighest = WarmGray,
            surfaceTint = Color.Transparent,
            outline = BorderGray,
            outlineVariant = BorderGray,
            error = DangerRed,
            inverseSurface = PureBlack,
            inverseOnSurface = OnPureBlack,
            inversePrimary = CopperAccent
        )
    } else {
        lightColorScheme(
            primary = CopperAccent,
            onPrimary = OnAccent,
            primaryContainer = SoftCopper,
            onPrimaryContainer = PureBlack,
            secondary = WineAccent,
            background = MatteSurface,
            onBackground = PureBlack,
            surface = DarkCharcoal,
            onSurface = PureBlack,
            surfaceVariant = WarmGray,
            onSurfaceVariant = MutedGray,
            surfaceContainerLowest = DarkCharcoal,
            surfaceContainerLow = DarkCharcoal,
            surfaceContainer = DarkCharcoal,
            surfaceContainerHigh = DarkCharcoal,
            surfaceContainerHighest = WarmGray,
            surfaceTint = Color.Transparent,
            outline = BorderGray,
            outlineVariant = BorderGray,
            error = DangerRed,
            inverseSurface = PureBlack,
            inverseOnSurface = OnPureBlack,
            inversePrimary = CopperAccent
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = LimitraShapes
    ) {
        CompositionLocalProvider(LocalIndication provides PressScaleIndication) {
            content()
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun updateThemeMode(context: Context, mode: AppThemeMode) {
    currentThemeMode.value = mode
    val prefs = context.getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE)
    val editor = prefs.edit().putString("theme_mode", mode.name)
    if (mode != AppThemeMode.DARK && currentThemePalette.value == AppThemePalette.PREMIUM_DARK) {
        currentThemePalette.value = AppThemePalette.BLUE
        editor.putString("theme_palette", AppThemePalette.BLUE.name)
    }
    editor.apply()
}

fun updateThemePalette(context: Context, palette: AppThemePalette) {
    currentThemePalette.value = palette
    val prefs = context.getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE)
    val editor = prefs.edit().putString("theme_palette", palette.name)
    if (palette == AppThemePalette.PREMIUM_DARK && currentThemeMode.value != AppThemeMode.DARK) {
        currentThemeMode.value = AppThemeMode.DARK
        editor.putString("theme_mode", AppThemeMode.DARK.name)
    }
    editor.apply()
}

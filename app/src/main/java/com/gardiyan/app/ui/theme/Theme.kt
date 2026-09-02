package com.gardiyan.app.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppThemePalette {
    BLUE, MONOCHROME, RED, PREMIUM_DARK
}

// Global olarak tema durumunu tutan MutableState'ler
val currentThemeMode = mutableStateOf(AppThemeMode.LIGHT)
val currentThemePalette = mutableStateOf(AppThemePalette.BLUE)

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

    // Renk değerlerini güncelle
    LaunchedEffect(isDark, currentPalette) {
        updateAppColors(isDark, currentPalette)
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = CopperAccent,
            background = MatteSurface,
            surface = DarkCharcoal,
            onBackground = PureBlack,
            onSurface = PureBlack
        )
    } else {
        lightColorScheme(
            primary = CopperAccent,
            background = MatteSurface,
            surface = DarkCharcoal,
            onBackground = PureBlack,
            onSurface = PureBlack
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
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

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
val currentThemeMode = mutableStateOf(AppThemeMode.SYSTEM)
val currentThemePalette = mutableStateOf(AppThemePalette.PREMIUM_DARK)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()
    
    // Tema modunu SharedPreferences'tan okuyup eşitleyelim
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE)
        val savedMode = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        currentThemeMode.value = runCatching { AppThemeMode.valueOf(savedMode) }.getOrDefault(AppThemeMode.SYSTEM)
        
        val savedPalette = prefs.getString("theme_palette", AppThemePalette.PREMIUM_DARK.name) ?: AppThemePalette.PREMIUM_DARK.name
        currentThemePalette.value = runCatching { AppThemePalette.valueOf(savedPalette) }.getOrDefault(AppThemePalette.PREMIUM_DARK)
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
    prefs.edit().putString("theme_mode", mode.name).apply()
}

fun updateThemePalette(context: Context, palette: AppThemePalette) {
    currentThemePalette.value = palette
    val prefs = context.getSharedPreferences("gardiyan_settings", Context.MODE_PRIVATE)
    prefs.edit().putString("theme_palette", palette.name).apply()
}

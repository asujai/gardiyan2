package com.gardiyan.app.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

// İç MutableState'ler
private val _pureBlack = mutableStateOf(Color(0xFF0E1726))
private val _onPureBlack = mutableStateOf(Color(0xFFFFFFFF))
private val _darkCharcoal = mutableStateOf(Color(0xFFFFFFFF))
private val _matteSurface = mutableStateOf(Color(0xFFF3F5F9))
private val _pureWhite = mutableStateOf(Color(0xFFFFFFFF))
private val _mutedGray = mutableStateOf(Color(0xFF5B6577))
private val _borderGray = mutableStateOf(Color(0xFFE2E6EE))
private val _dangerRed = mutableStateOf(Color(0xFFD23F45))
private val _softDangerRed = mutableStateOf(Color(0xFFFCEDED))
private val _successGreen = mutableStateOf(Color(0xFF16875D))

private val _dashboardInk = mutableStateOf(Color(0xFF0E1726))
private val _dashboardCard = mutableStateOf(Color(0xFFFFFFFF))
private val _dashboardIvory = mutableStateOf(Color(0xFFF3F5F9))
private val _copperAccent = mutableStateOf(Color(0xFF2D5BE3))
private val _wineAccent = mutableStateOf(Color(0xFF1E3FB0))
private val _softCopper = mutableStateOf(Color(0xFFE8EEFD))
private val _warmGray = mutableStateOf(Color(0xFFE9EDF3))
private val _dashboardMuted = mutableStateOf(Color(0xFF5B6577))
private val _dashboardBorder = mutableStateOf(Color(0xFFE2E6EE))
private val _dashboardDanger = mutableStateOf(Color(0xFFD23F45))
private val _dashboardSoftDanger = mutableStateOf(Color(0xFFFCEDED))
private val _dashboardSuccess = mutableStateOf(Color(0xFF16875D))
private val _isDarkUi = mutableStateOf(false)

// Dışarıya sunulan dinamik özellikler (Compose state okumasını algılar)
val PureBlack: Color get() = _pureBlack.value
val OnPureBlack: Color get() = _onPureBlack.value
val DarkCharcoal: Color get() = _darkCharcoal.value
val MatteSurface: Color get() = _matteSurface.value
val PureWhite: Color get() = _pureWhite.value
val MutedGray: Color get() = _mutedGray.value
val BorderGray: Color get() = _borderGray.value
val DangerRed: Color get() = _dangerRed.value
val SoftDangerRed: Color get() = _softDangerRed.value
val SuccessGreen: Color get() = _successGreen.value

val DashboardInk: Color get() = _dashboardInk.value
val DashboardCard: Color get() = _dashboardCard.value
val DashboardIvory: Color get() = _dashboardIvory.value
val CopperAccent: Color get() = _copperAccent.value
val WineAccent: Color get() = _wineAccent.value
val SoftCopper: Color get() = _softCopper.value
val WarmGray: Color get() = _warmGray.value
val DashboardMuted: Color get() = _dashboardMuted.value
val DashboardBorder: Color get() = _dashboardBorder.value
val DashboardDanger: Color get() = _dashboardDanger.value
val DashboardSoftDanger: Color get() = _dashboardSoftDanger.value
val DashboardSuccess: Color get() = _dashboardSuccess.value

/** Etkin palet koyu mu? Gölge/parıltı gibi yalnız bir temada anlamlı detaylar için. */
val IsDarkUi: Boolean get() = _isDarkUi.value

/** Accent üzerine yazılan metin rengi: beyaz yeterli kontrast vermiyorsa koyu döner. */
val OnAccent: Color get() = onColorFor(_copperAccent.value)

fun onColorFor(background: Color): Color {
    val whiteContrast = 1.05f / (background.luminance() + 0.05f)
    return if (whiteContrast >= 4.0f) Color.White else Color(0xFF111111)
}

/**
 * Bir paletin tüm rolleri. Ink = ana metin, Card = kart yüzeyi, Ground = ekran zemini,
 * Accent = tek vurgu rengi (birincil eylem, aktif durum), Soft = accent'in yumuşak zemini.
 */
private data class Palette(
    val ink: Color,
    val onInk: Color,
    val card: Color,
    val ground: Color,
    val muted: Color,
    val border: Color,
    val accent: Color,
    val accentDeep: Color,
    val soft: Color,
    val track: Color,
    val danger: Color,
    val softDanger: Color,
    val success: Color,
    val dark: Boolean
)

private fun Palette.apply() {
    _pureBlack.value = ink
    _onPureBlack.value = onInk
    _darkCharcoal.value = card
    _matteSurface.value = ground
    _pureWhite.value = Color.White
    _mutedGray.value = muted
    _borderGray.value = border
    _dangerRed.value = danger
    _softDangerRed.value = softDanger
    _successGreen.value = success

    _dashboardInk.value = ink
    _dashboardCard.value = card
    _dashboardIvory.value = ground
    _copperAccent.value = accent
    _wineAccent.value = accentDeep
    _softCopper.value = soft
    _warmGray.value = track
    _dashboardMuted.value = muted
    _dashboardBorder.value = border
    _dashboardDanger.value = danger
    _dashboardSoftDanger.value = softDanger
    _dashboardSuccess.value = success
    _isDarkUi.value = dark
}

// Gece mavisi: soğuk porselen zemin, safir vurgu.
private val BlueLight = Palette(
    ink = Color(0xFF0E1726), onInk = Color.White, card = Color.White, ground = Color(0xFFF3F5F9),
    muted = Color(0xFF5B6577), border = Color(0xFFE2E6EE), accent = Color(0xFF2D5BE3),
    accentDeep = Color(0xFF1E3FB0), soft = Color(0xFFE8EEFD), track = Color(0xFFE9EDF3),
    danger = Color(0xFFD23F45), softDanger = Color(0xFFFCEDED), success = Color(0xFF16875D), dark = false
)
private val BlueDark = Palette(
    ink = Color(0xFFEEF2F8), onInk = Color(0xFF0A0F1A), card = Color(0xFF121A2A), ground = Color(0xFF0A0F1A),
    muted = Color(0xFF8E9AAF), border = Color(0xFF212C40), accent = Color(0xFF6F93FF),
    accentDeep = Color(0xFF9AB3FF), soft = Color(0xFF17244A), track = Color(0xFF1B2537),
    danger = Color(0xFFF06B6B), softDanger = Color(0xFF3A1518), success = Color(0xFF3FD39B), dark = true
)

// Siyah & beyaz: kâğıt ve mürekkep.
private val MonoLight = Palette(
    ink = Color(0xFF121212), onInk = Color.White, card = Color.White, ground = Color(0xFFF4F4F2),
    muted = Color(0xFF666662), border = Color(0xFFE4E4E0), accent = Color(0xFF121212),
    accentDeep = Color(0xFF2B2B2B), soft = Color(0xFFEDEDEA), track = Color(0xFFEAEAE6),
    danger = Color(0xFFD03A3A), softDanger = Color(0xFFFBECEC), success = Color(0xFF1C8A55), dark = false
)
private val MonoDark = Palette(
    ink = Color(0xFFF2F2F0), onInk = Color(0xFF0B0B0B), card = Color(0xFF161616), ground = Color(0xFF0B0B0B),
    muted = Color(0xFF9C9C98), border = Color(0xFF282828), accent = Color(0xFFF2F2F0),
    accentDeep = Color(0xFFD6D6D2), soft = Color(0xFF242424), track = Color(0xFF202020),
    danger = Color(0xFFF06B6B), softDanger = Color(0xFF361414), success = Color(0xFF45D08E), dark = true
)

// Kırmızı: sıcak beyaz zemin, lal vurgu.
private val RedLight = Palette(
    ink = Color(0xFF1E1414), onInk = Color.White, card = Color.White, ground = Color(0xFFFAF6F4),
    muted = Color(0xFF75625F), border = Color(0xFFF0E2DE), accent = Color(0xFFC42E37),
    accentDeep = Color(0xFF9E1F28), soft = Color(0xFFFBE6E5), track = Color(0xFFF3E8E5),
    danger = Color(0xFFC42E37), softDanger = Color(0xFFFCEAEA), success = Color(0xFF1D8250), dark = false
)
private val RedDark = Palette(
    ink = Color(0xFFF6ECEA), onInk = Color(0xFF140C0C), card = Color(0xFF1F1414), ground = Color(0xFF140C0C),
    muted = Color(0xFFB9A2A0), border = Color(0xFF3A2626), accent = Color(0xFFE0464E),
    accentDeep = Color(0xFFFF8A8E), soft = Color(0xFF3D1A1C), track = Color(0xFF2E1E1E),
    danger = Color(0xFFF0585E), softDanger = Color(0xFF3D1A1C), success = Color(0xFF45CF8B), dark = true
)

// Premium koyu: mürekkep siyahı, kemik metin, antika altın vurgu.
private val PremiumDark = Palette(
    ink = Color(0xFFEDE6D8), onInk = Color(0xFF0F0E0C), card = Color(0xFF181613), ground = Color(0xFF0F0E0C),
    muted = Color(0xFFA39C8F), border = Color(0xFF2C2823), accent = Color(0xFFD9A55B),
    accentDeep = Color(0xFFE8743B), soft = Color(0xFF2A2117), track = Color(0xFF221F1A),
    danger = Color(0xFFE0614A), softDanger = Color(0xFF34190F), success = Color(0xFF93C28C), dark = true
)

private fun resolvePalette(isDark: Boolean, palette: AppThemePalette): Palette = when (palette) {
    AppThemePalette.BLUE -> if (isDark) BlueDark else BlueLight
    AppThemePalette.MONOCHROME -> if (isDark) MonoDark else MonoLight
    AppThemePalette.RED -> if (isDark) RedDark else RedLight
    // Premium Koyu her iki modda da koyu kalır.
    AppThemePalette.PREMIUM_DARK -> PremiumDark
}

fun updateAppColors(isDark: Boolean, palette: AppThemePalette) {
    resolvePalette(isDark, palette).apply()
}

/** Compose dışı ekranlar (kilit ekranı katmanı) için paletin ARGB değerleri. */
data class OverlayColors(
    val ground: Int,
    val ink: Int,
    val muted: Int,
    val border: Int,
    val accent: Int,
    val onAccent: Int,
    val danger: Int,
    val soft: Int
)

fun overlayColorsFor(isDark: Boolean, palette: AppThemePalette): OverlayColors {
    val p = resolvePalette(isDark, palette)
    return OverlayColors(
        ground = p.ground.toArgb(),
        ink = p.ink.toArgb(),
        muted = p.muted.toArgb(),
        border = p.border.toArgb(),
        accent = p.accent.toArgb(),
        onAccent = onColorFor(p.accent).toArgb(),
        danger = p.danger.toArgb(),
        soft = p.soft.toArgb()
    )
}

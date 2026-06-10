package com.gardiyan.app.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

// İç MutableState'ler
private val _pureBlack = mutableStateOf(Color(0xFF0F172A))
private val _onPureBlack = mutableStateOf(Color(0xFFFFFFFF))
private val _darkCharcoal = mutableStateOf(Color(0xFFFFFFFF))
private val _matteSurface = mutableStateOf(Color(0xFFF8FAFC))
private val _pureWhite = mutableStateOf(Color(0xFFFFFFFF))
private val _mutedGray = mutableStateOf(Color(0xFF64748B))
private val _borderGray = mutableStateOf(Color(0xFFE2E8F0))
private val _dangerRed = mutableStateOf(Color(0xFFEF4444))
private val _softDangerRed = mutableStateOf(Color(0xFFFEF2F2))
private val _successGreen = mutableStateOf(Color(0xFF059669))

private val _dashboardInk = mutableStateOf(Color(0xFF28221E))
private val _dashboardCard = mutableStateOf(Color(0xFFFFFDF8))
private val _dashboardIvory = mutableStateOf(Color(0xFFF7F1E7))
private val _copperAccent = mutableStateOf(Color(0xFFB66A32))
private val _wineAccent = mutableStateOf(Color(0xFF713A3E))
private val _softCopper = mutableStateOf(Color(0xFFF4E5D3))
private val _warmGray = mutableStateOf(Color(0xFFEDE6DC))
private val _dashboardMuted = mutableStateOf(Color(0xFF786F68))
private val _dashboardBorder = mutableStateOf(Color(0xFFE5DACE))
private val _dashboardDanger = mutableStateOf(Color(0xFFB7463E))
private val _dashboardSoftDanger = mutableStateOf(Color(0xFFF8E6E2))
private val _dashboardSuccess = mutableStateOf(Color(0xFF6D7551))

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

fun updateAppColors(isDark: Boolean, palette: AppThemePalette) {
    when (palette) {
        AppThemePalette.BLUE -> {
            if (isDark) {
                _pureBlack.value = Color(0xFFF1F5F9)
                _onPureBlack.value = Color(0xFF0F172A)
                _darkCharcoal.value = Color(0xFF1E293B)
                _matteSurface.value = Color(0xFF0F172A)
                _pureWhite.value = Color(0xFFFFFFFF)
                _mutedGray.value = Color(0xFF94A3B8)
                _borderGray.value = Color(0xFF334155)
                _dangerRed.value = Color(0xFFF87171)
                _softDangerRed.value = Color(0xFF450A0A)
                _successGreen.value = Color(0xFF34D399)

                _dashboardInk.value = Color(0xFFF1F5F9)
                _dashboardCard.value = Color(0xFF1E293B)
                _dashboardIvory.value = Color(0xFF0F172A)
                _copperAccent.value = Color(0xFF3B82F6)
                _wineAccent.value = Color(0xFF60A5FA)
                _softCopper.value = Color(0xFF1E3A8A)
                _warmGray.value = Color(0xFF2E3D52)
                _dashboardMuted.value = Color(0xFF94A3B8)
                _dashboardBorder.value = Color(0xFF334155)
                _dashboardDanger.value = Color(0xFFF87171)
                _dashboardSoftDanger.value = Color(0xFF450A0A)
                _dashboardSuccess.value = Color(0xFF34D399)
            } else {
                _pureBlack.value = Color(0xFF0F172A)
                _onPureBlack.value = Color(0xFFFFFFFF)
                _darkCharcoal.value = Color(0xFFFFFFFF)
                _matteSurface.value = Color(0xFFF8FAFC)
                _pureWhite.value = Color(0xFFFFFFFF)
                _mutedGray.value = Color(0xFF64748B)
                _borderGray.value = Color(0xFFE2E8F0)
                _dangerRed.value = Color(0xFFEF4444)
                _softDangerRed.value = Color(0xFFFEF2F2)
                _successGreen.value = Color(0xFF059669)

                _dashboardInk.value = Color(0xFF0F172A)
                _dashboardCard.value = Color(0xFFFFFFFF)
                _dashboardIvory.value = Color(0xFFF8FAFC)
                _copperAccent.value = Color(0xFF2563EB)
                _wineAccent.value = Color(0xFF1D4ED8)
                _softCopper.value = Color(0xFFEFF6FF)
                _warmGray.value = Color(0xFFF1F5F9)
                _dashboardMuted.value = Color(0xFF64748B)
                _dashboardBorder.value = Color(0xFFE2E8F0)
                _dashboardDanger.value = Color(0xFFEF4444)
                _dashboardSoftDanger.value = Color(0xFFFEF2F2)
                _dashboardSuccess.value = Color(0xFF059669)
            }
        }
        AppThemePalette.MONOCHROME -> {
            if (isDark) {
                _pureBlack.value = Color(0xFFF4F4F5)
                _onPureBlack.value = Color(0xFF18181B)
                _darkCharcoal.value = Color(0xFF18181B)
                _matteSurface.value = Color(0xFF09090B)
                _pureWhite.value = Color(0xFFFFFFFF)
                _mutedGray.value = Color(0xFFA1A1AA)
                _borderGray.value = Color(0xFF27272A)
                _dangerRed.value = Color(0xFFF87171)
                _softDangerRed.value = Color(0xFF450A0A)
                _successGreen.value = Color(0xFF34D399)

                _dashboardInk.value = Color(0xFFF4F4F5)
                _dashboardCard.value = Color(0xFF18181B)
                _dashboardIvory.value = Color(0xFF09090B)
                _copperAccent.value = Color(0xFFF4F4F5)
                _wineAccent.value = Color(0xFFE4E4E7)
                _softCopper.value = Color(0xFF27272A)
                _warmGray.value = Color(0xFF27272A)
                _dashboardMuted.value = Color(0xFFA1A1AA)
                _dashboardBorder.value = Color(0xFF27272A)
                _dashboardDanger.value = Color(0xFFF87171)
                _dashboardSoftDanger.value = Color(0xFF450A0A)
                _dashboardSuccess.value = Color(0xFF34D399)
            } else {
                _pureBlack.value = Color(0xFF18181B)
                _onPureBlack.value = Color(0xFFFFFFFF)
                _darkCharcoal.value = Color(0xFFFFFFFF)
                _matteSurface.value = Color(0xFFFAFAFA)
                _pureWhite.value = Color(0xFFFFFFFF)
                _mutedGray.value = Color(0xFF71717A)
                _borderGray.value = Color(0xFFE4E4E7)
                _dangerRed.value = Color(0xFFEF4444)
                _softDangerRed.value = Color(0xFFFEF2F2)
                _successGreen.value = Color(0xFF059669)

                _dashboardInk.value = Color(0xFF18181B)
                _dashboardCard.value = Color(0xFFFFFFFF)
                _dashboardIvory.value = Color(0xFFFAFAFA)
                _copperAccent.value = Color(0xFF18181B)
                _wineAccent.value = Color(0xFF27272A)
                _softCopper.value = Color(0xFFF4F4F5)
                _warmGray.value = Color(0xFFE4E4E7)
                _dashboardMuted.value = Color(0xFF71717A)
                _dashboardBorder.value = Color(0xFFE4E4E7)
                _dashboardDanger.value = Color(0xFFEF4444)
                _dashboardSoftDanger.value = Color(0xFFFEF2F2)
                _dashboardSuccess.value = Color(0xFF059669)
            }
        }
        AppThemePalette.RED -> {
            if (isDark) {
                _pureBlack.value = Color(0xFFFCE8E8)
                _onPureBlack.value = Color(0xFF2D1A1A)
                _darkCharcoal.value = Color(0xFF2D1A1A)
                _matteSurface.value = Color(0xFF1A0F0F)
                _pureWhite.value = Color(0xFFFFFFFF)
                _mutedGray.value = Color(0xFFC7A5A5)
                _borderGray.value = Color(0xFF4A2B2B)
                _dangerRed.value = Color(0xFFEF4444)
                _softDangerRed.value = Color(0xFF450A0A)
                _successGreen.value = Color(0xFF34D399)

                _dashboardInk.value = Color(0xFFFCE8E8)
                _dashboardCard.value = Color(0xFF2D1A1A)
                _dashboardIvory.value = Color(0xFF1A0F0F)
                _copperAccent.value = Color(0xFFEF4444)
                _wineAccent.value = Color(0xFFF87171)
                _softCopper.value = Color(0xFF7F1D1D)
                _warmGray.value = Color(0xFF3D2626)
                _dashboardMuted.value = Color(0xFFC7A5A5)
                _dashboardBorder.value = Color(0xFF4A2B2B)
                _dashboardDanger.value = Color(0xFFEF4444)
                _dashboardSoftDanger.value = Color(0xFF450A0A)
                _dashboardSuccess.value = Color(0xFF34D399)
            } else {
                _pureBlack.value = Color(0xFF1F1F1F)
                _onPureBlack.value = Color(0xFFFFFFFF)
                _darkCharcoal.value = Color(0xFFFFFFFF)
                _matteSurface.value = Color(0xFFFFFDFD)
                _pureWhite.value = Color(0xFFFFFFFF)
                _mutedGray.value = Color(0xFF7F7F7F)
                _borderGray.value = Color(0xFFF3E8E8)
                _dangerRed.value = Color(0xFFDC2626)
                _softDangerRed.value = Color(0xFFFFF1F1)
                _successGreen.value = Color(0xFF15803D)

                _dashboardInk.value = Color(0xFF1F1F1F)
                _dashboardCard.value = Color(0xFFFFFFFF)
                _dashboardIvory.value = Color(0xFFFFFDFD)
                _copperAccent.value = Color(0xFFDC2626)
                _wineAccent.value = Color(0xFFB91C1C)
                _softCopper.value = Color(0xFFFEE2E2)
                _warmGray.value = Color(0xFFF3E8E8)
                _dashboardMuted.value = Color(0xFF7F7F7F)
                _dashboardBorder.value = Color(0xFFF3E8E8)
                _dashboardDanger.value = Color(0xFFDC2626)
                _dashboardSoftDanger.value = Color(0xFFFFF1F1)
                _dashboardSuccess.value = Color(0xFF15803D)
            }
        }
        AppThemePalette.PREMIUM_DARK -> {
            // Premium Dark her iki modda da koyu kalacaktır.
            _pureBlack.value = Color(0xFFEDEDED)
            _onPureBlack.value = Color(0xFF171717)
            _darkCharcoal.value = Color(0xFF171717)
            _matteSurface.value = Color(0xFF0A0A0A)
            _pureWhite.value = Color(0xFFFFFFFF)
            _mutedGray.value = Color(0xFF737373)
            _borderGray.value = Color(0xFF262626)
            _dangerRed.value = Color(0xFFEF4444)
            _softDangerRed.value = Color(0xFF450A0A)
            _successGreen.value = Color(0xFF10B981)

            _dashboardInk.value = Color(0xFFEDEDED)
            _dashboardCard.value = Color(0xFF171717)
            _dashboardIvory.value = Color(0xFF0A0A0A)
            _copperAccent.value = Color(0xFFF59E0B)
            _wineAccent.value = Color(0xFFD97706)
            _softCopper.value = Color(0xFF451A03)
            _warmGray.value = Color(0xFF262626)
            _dashboardMuted.value = Color(0xFF737373)
            _dashboardBorder.value = Color(0xFF262626)
            _dashboardDanger.value = Color(0xFFEF4444)
            _dashboardSoftDanger.value = Color(0xFF450A0A)
            _dashboardSuccess.value = Color(0xFF10B981)
        }
    }
}

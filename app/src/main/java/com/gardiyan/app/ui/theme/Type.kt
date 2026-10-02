package com.gardiyan.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gardiyan.app.R

/**
 * Arayüz yazı ailesi. Manrope; Latin dışı yazılarda (Arapça, Hintçe, Tayca)
 * Android eksik glifleri sistem fontundan tamamlar.
 */
val LimitraSans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
    Font(R.font.manrope_extrabold, FontWeight.Black)
)

/** Başlık, büyük rakam ve söz yazı ailesi (serif). Ölçülü kullanılır. */
val LimitraDisplay = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal),
    Font(R.font.newsreader_medium, FontWeight.Medium),
    Font(R.font.newsreader_medium, FontWeight.SemiBold),
    Font(R.font.newsreader_medium, FontWeight.Bold),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic)
)

private fun sans(size: Int, line: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
    fontFamily = LimitraSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

private fun display(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, spacing: Double = 0.0) = TextStyle(
    fontFamily = LimitraDisplay,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

val Typography = Typography(
    displayLarge = display(52, 56, spacing = -1.0),
    displayMedium = display(42, 46, spacing = -0.8),
    displaySmall = display(34, 38, spacing = -0.5),
    headlineLarge = display(30, 36, spacing = -0.4),
    headlineMedium = display(26, 32, spacing = -0.3),
    headlineSmall = display(22, 28, spacing = -0.2),
    titleLarge = sans(20, 26, FontWeight.Bold, -0.2),
    titleMedium = sans(16, 22, FontWeight.Bold, -0.1),
    titleSmall = sans(14, 20, FontWeight.SemiBold),
    bodyLarge = sans(16, 24, FontWeight.Normal),
    bodyMedium = sans(14, 20, FontWeight.Normal),
    bodySmall = sans(12, 17, FontWeight.Normal),
    labelLarge = sans(14, 20, FontWeight.Bold, 0.1),
    labelMedium = sans(12, 16, FontWeight.SemiBold, 0.2),
    labelSmall = sans(11, 14, FontWeight.SemiBold, 0.3)
)

package com.aquagrow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aquagrow.R

val Outfit = FontFamily(
    Font(R.font.outfit_500, FontWeight.Medium),
    Font(R.font.outfit_600, FontWeight.SemiBold),
    Font(R.font.outfit_700, FontWeight.Bold),
)

val DmSans = FontFamily(
    Font(R.font.dmsans_400, FontWeight.Normal),
    Font(R.font.dmsans_500, FontWeight.Medium),
    Font(R.font.dmsans_600, FontWeight.SemiBold),
    Font(R.font.dmsans_700, FontWeight.Bold),
)

private fun heading(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold) = TextStyle(fontFamily = Outfit, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(fontFamily = DmSans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

/** Outfit for headings (600–700), DM Sans for body and labels (400–600). */
val AquaGrowTypography = Typography(
    displayLarge = heading(48, 56, FontWeight.Bold),
    displayMedium = heading(40, 48, FontWeight.Bold),
    displaySmall = heading(34, 42, FontWeight.Bold),
    headlineLarge = heading(30, 38, FontWeight.Bold),
    headlineMedium = heading(26, 34, FontWeight.Bold),
    headlineSmall = heading(22, 30),
    titleLarge = heading(20, 28),
    titleMedium = heading(17, 24),
    titleSmall = heading(15, 22),
    bodyLarge = body(16, 24),
    bodyMedium = body(14, 20),
    bodySmall = body(12, 16),
    labelLarge = body(15, 20, FontWeight.SemiBold),
    labelMedium = body(13, 18, FontWeight.Medium),
    labelSmall = body(11, 16, FontWeight.Medium),
)

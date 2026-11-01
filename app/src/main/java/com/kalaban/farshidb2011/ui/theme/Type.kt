package com.kalaban.farshidb2011.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kalaban.farshidb2011.R

/**
 * Vazirmatn (successor of Vazir from rastikerdar/vazir-font) with Farsi digits,
 * used as the app-wide default typeface for Persian UI.
 */
val VazirFontFamily = FontFamily(
    Font(R.font.vazirmatn_ui_fd_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_ui_fd_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_ui_fd_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_ui_fd_bold, FontWeight.Bold),
)

private fun vazirStyle(
    weight: FontWeight,
    size: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
) = TextStyle(
    fontFamily = VazirFontFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

val Typography = Typography(
    displayLarge = vazirStyle(FontWeight.Normal, 57.sp, 64.sp, (-0.25).sp),
    displayMedium = vazirStyle(FontWeight.Normal, 45.sp, 52.sp),
    displaySmall = vazirStyle(FontWeight.Normal, 36.sp, 44.sp),
    headlineLarge = vazirStyle(FontWeight.SemiBold, 32.sp, 40.sp),
    headlineMedium = vazirStyle(FontWeight.SemiBold, 28.sp, 36.sp),
    headlineSmall = vazirStyle(FontWeight.SemiBold, 24.sp, 32.sp),
    titleLarge = vazirStyle(FontWeight.SemiBold, 22.sp, 28.sp),
    titleMedium = vazirStyle(FontWeight.Medium, 16.sp, 24.sp, 0.15.sp),
    titleSmall = vazirStyle(FontWeight.Medium, 14.sp, 20.sp, 0.1.sp),
    bodyLarge = vazirStyle(FontWeight.Normal, 16.sp, 24.sp, 0.5.sp),
    bodyMedium = vazirStyle(FontWeight.Normal, 14.sp, 20.sp, 0.25.sp),
    bodySmall = vazirStyle(FontWeight.Normal, 12.sp, 16.sp, 0.4.sp),
    labelLarge = vazirStyle(FontWeight.Medium, 14.sp, 20.sp, 0.1.sp),
    labelMedium = vazirStyle(FontWeight.Medium, 12.sp, 16.sp, 0.5.sp),
    labelSmall = vazirStyle(FontWeight.Medium, 11.sp, 16.sp, 0.5.sp),
)

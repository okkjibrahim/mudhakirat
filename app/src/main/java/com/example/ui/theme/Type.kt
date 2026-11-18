package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal, FontWeight.Normal),
    Font(R.font.tajawal, FontWeight.Medium),
    Font(R.font.tajawal, FontWeight.SemiBold),
    Font(R.font.tajawal, FontWeight.Bold),
    Font(R.font.tajawal, FontWeight.ExtraBold),
)

val RubikFontFamily = FontFamily(
    Font(R.font.rubik, FontWeight.Normal),
    Font(R.font.rubik, FontWeight.Medium),
    Font(R.font.rubik, FontWeight.SemiBold),
    Font(R.font.rubik, FontWeight.Bold),
    Font(R.font.rubik, FontWeight.ExtraBold),
)

val FontScaleSteps = floatArrayOf(0.8f, 0.9f, 1.0f, 1.12f, 1.25f)

fun rubikNumStyle(base: TextStyle): TextStyle = base.copy(
    fontFamily = RubikFontFamily,
    fontFeatureSettings = "tnum",
)

fun mudhakiratiTypography(scale: Float = 1f): Typography {
    fun s(v: Float) = (v * scale).sp
    return Typography(
        displayLarge = TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(52f),
            lineHeight = s(56f),
            fontFeatureSettings = "tnum",
        ),
        displayMedium = TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(36f),
            lineHeight = s(42f),
            fontFeatureSettings = "tnum",
        ),
        headlineMedium = TextStyle(
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(22f),
            lineHeight = s(28f),
        ),
        headlineSmall = TextStyle(
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(19f),
            lineHeight = s(25f),
        ),
        titleLarge = TextStyle(
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(18f),
            lineHeight = s(24f),
        ),
        titleMedium = TextStyle(
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(16f),
            lineHeight = s(22f),
        ),
        titleSmall = TextStyle(
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(14f),
            lineHeight = s(20f),
        ),
        bodyLarge = TextStyle(
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = s(16f),
            lineHeight = s(24f),
        ),
        bodyMedium = TextStyle(
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = s(14.5f),
            lineHeight = s(21f),
        ),
        bodySmall = TextStyle(
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = s(12.5f),
            lineHeight = s(18f),
        ),
        labelLarge = TextStyle(
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(15f),
            lineHeight = s(20f),
        ),
        labelMedium = TextStyle(
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(13f),
            lineHeight = s(17f),
        ),
        labelSmall = TextStyle(
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = s(11.5f),
            lineHeight = s(15f),
        ),
    )
}

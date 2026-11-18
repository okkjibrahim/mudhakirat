package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

data class AppPalette(
    val id: String,
    val isDark: Boolean,
    val ar: String,
    val en: String,
    val bg: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val text: Color,
    val muted: Color,
    val input: Color,
    val border: Color,
)

val SubjectColors = listOf(
    Color(0xFF2D5BFF),
    Color(0xFF12A579),
    Color(0xFFF2552C),
    Color(0xFFE8478B),
    Color(0xFF8B5CF6),
    Color(0xFFE8A317),
)

val AppPalettes: List<AppPalette> = listOf(
    AppPalette(
        id = "white-yellow",
        isDark = false,
        ar = "أبيض وأصفر",
        en = "White & Yellow",
        bg = Color(0xFFFFFDF7),
        surface = Color(0xFFFFFFFF),
        primary = Color(0xFFF5B800),
        secondary = Color(0xFFD98A00),
        text = Color(0xFF1C1914),
        muted = Color(0xFF6F6756),
        input = Color(0xFFFBF6E6),
        border = Color(0xFFEFE5C6),
    ),
    AppPalette(
        id = "pearl-ruby",
        isDark = false,
        ar = "لؤلؤ وياقوت",
        en = "Pearl & Ruby",
        bg = Color(0xFFF3EEE3),
        surface = Color(0xFFFBF8F1),
        primary = Color(0xFF9B1239),
        secondary = Color(0xFF7A6A58),
        text = Color(0xFF2A2320),
        muted = Color(0xFF7C7266),
        input = Color(0xFFEDE6D8),
        border = Color(0xFFD8CFBE),
    ),
    AppPalette(
        id = "night-orange",
        isDark = true,
        ar = "ليل وبرتقال",
        en = "Night & Orange",
        bg = Color(0xFF1E1B1A),
        surface = Color(0xFF2A2625),
        primary = Color(0xFFFF6A2B),
        secondary = Color(0xFFE0B49A),
        text = Color(0xFFF7F1EC),
        muted = Color(0xFFB3A9A2),
        input = Color(0xFF35302E),
        border = Color(0xFF4F4743),
    ),
    AppPalette(
        id = "olive-gold",
        isDark = true,
        ar = "زيتون وذهب",
        en = "Olive & Gold",
        bg = Color(0xFF1C2112),
        surface = Color(0xFF272E19),
        primary = Color(0xFFE3B23C),
        secondary = Color(0xFFB9C27A),
        text = Color(0xFFF6F2E2),
        muted = Color(0xFFB5B79A),
        input = Color(0xFF323B21),
        border = Color(0xFF505B38),
    ),
    AppPalette(
        id = "sand-teal",
        isDark = false,
        ar = "رمل وتركوازي",
        en = "Sand & Teal",
        bg = Color(0xFFF2E8D2),
        surface = Color(0xFFFAF4E6),
        primary = Color(0xFF0E9494),
        secondary = Color(0xFF8A6F45),
        text = Color(0xFF2B2418),
        muted = Color(0xFF7E7461),
        input = Color(0xFFEADCC0),
        border = Color(0xFFD6C5A2),
    ),
    AppPalette(
        id = "navy-berry",
        isDark = true,
        ar = "كحلي وعنابي",
        en = "Navy & Berry",
        bg = Color(0xFF141F38),
        surface = Color(0xFF1D2C4D),
        primary = Color(0xFFD6335F),
        secondary = Color(0xFFB7C3DD),
        text = Color(0xFFF4F6FB),
        muted = Color(0xFFA9B4CC),
        input = Color(0xFF263760),
        border = Color(0xFF41527C),
    ),
    AppPalette(
        id = "ice-orange",
        isDark = false,
        ar = "جليد وبرتقالي",
        en = "Ice & Orange",
        bg = Color(0xFFE3F1F9),
        surface = Color(0xFFF4FAFE),
        primary = Color(0xFFF26B1D),
        secondary = Color(0xFF3F6F8C),
        text = Color(0xFF14283A),
        muted = Color(0xFF587388),
        input = Color(0xFFD5E8F3),
        border = Color(0xFFB9D5E6),
    ),
    AppPalette(
        id = "pine-cream",
        isDark = true,
        ar = "صنوبر وكريمي",
        en = "Pine & Cream",
        bg = Color(0xFF1B3027),
        surface = Color(0xFF243D32),
        primary = Color(0xFFF4EBD6),
        secondary = Color(0xFFCFC29E),
        text = Color(0xFFFBF6EA),
        muted = Color(0xFFAFC0B5),
        input = Color(0xFF2E4A3D),
        border = Color(0xFF4A6A5A),
    ),
    AppPalette(
        id = "beige-orange",
        isDark = true,
        ar = "بيجي وبرتقالي داكن",
        en = "Dark Beige & Orange",
        bg = Color(0xFF29231F),
        surface = Color(0xFF382F29),
        primary = Color(0xFFF28C28),
        secondary = Color(0xFFD6BFA5),
        text = Color(0xFFF5EDE4),
        muted = Color(0xFFB8A99B),
        input = Color(0xFF453A32),
        border = Color(0xFF665345),
    ),
    AppPalette(
        id = "navy-cream",
        isDark = true,
        ar = "كحلي وكريمي (داكن)",
        en = "Navy & Cream",
        bg = Color(0xFF101D35),
        surface = Color(0xFF192944),
        primary = Color(0xFFF3EBDD),
        secondary = Color(0xFFD7C6A5),
        text = Color(0xFFFFFFFF),
        muted = Color(0xFFB8C3D4),
        input = Color(0xFF223451),
        border = Color(0xFF40516B),
    ),
    AppPalette(
        id = "burgundy-gold",
        isDark = true,
        ar = "عنابي وذهبي (داكن)",
        en = "Burgundy & Gold",
        bg = Color(0xFF30121E),
        surface = Color(0xFF401725),
        primary = Color(0xFFD4AF37),
        secondary = Color(0xFFE5C878),
        text = Color(0xFFFFF7E8),
        muted = Color(0xFFC7AEB5),
        input = Color(0xFF512332),
        border = Color(0xFF744354),
    ),
    AppPalette(
        id = "olive-dark",
        isDark = true,
        ar = "زيتوني داكن",
        en = "Olive Dark",
        bg = Color(0xFF20291B),
        surface = Color(0xFF2D3825),
        primary = Color(0xFF78864B),
        secondary = Color(0xFFC5B98A),
        text = Color(0xFFF5F2E8),
        muted = Color(0xFFB9B99F),
        input = Color(0xFF39452F),
        border = Color(0xFF59664A),
    ),
    AppPalette(
        id = "mocha-dark",
        isDark = true,
        ar = "موكا داكن",
        en = "Dark Mocha",
        bg = Color(0xFF251C19),
        surface = Color(0xFF342721),
        primary = Color(0xFFA9876A),
        secondary = Color(0xFFE6D5C3),
        text = Color(0xFFFFF8F0),
        muted = Color(0xFFBDAA9B),
        input = Color(0xFF44342B),
        border = Color(0xFF665044),
    ),
)

val LightSystemPalette = AppPalette(
    id = "light",
    isDark = false,
    ar = "فاتح",
    en = "Light",
    bg = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF2D5BFF),
    secondary = Color(0xFF5B78FF),
    text = Color(0xFF0D1226),
    muted = Color(0xFF636B83),
    input = Color(0xFFE6ECFF),
    border = Color(0xFFE3E7F0),
)

val DarkSystemPalette = AppPalette(
    id = "dark",
    isDark = true,
    ar = "داكن",
    en = "Dark",
    bg = Color(0xFF0A0E1C),
    surface = Color(0xFF131A2E),
    primary = Color(0xFF7C98FF),
    secondary = Color(0xFF5B78FF),
    text = Color(0xFFEEF1FA),
    muted = Color(0xFF8E97B3),
    input = Color(0xFF1B2650),
    border = Color(0xFF222B45),
)

val AutoWarmDarkPalette = AppPalette(
    id = "auto-dark",
    isDark = true,
    ar = "تلقائي",
    en = "Auto",
    bg = Color(0xFF14120B),
    surface = Color(0xFF1E1B12),
    primary = Color(0xFFFFC93C),
    secondary = Color(0xFFFFE07D),
    text = Color(0xFFF6F1E3),
    muted = Color(0xFFA39C86),
    input = Color(0xFF2E2610),
    border = Color(0xFF2E2A1C),
)

fun findPalette(id: String): AppPalette = AppPalettes.firstOrNull { it.id == id } ?: AppPalettes.first()

fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

fun onPrimaryFor(palette: AppPalette): Color {
    val candidates = listOf(palette.bg, palette.text, Color.Black, Color.White)
    for (c in candidates) {
        if (contrastRatio(palette.primary, c) >= 4.5f) return c
    }
    return if (contrastRatio(palette.primary, Color.Black) >= contrastRatio(palette.primary, Color.White)) {
        Color.Black
    } else {
        Color.White
    }
}

fun primaryTextFor(palette: AppPalette): Color {
    if (palette.id == "white-yellow") return Color(0xFF9A6B00)
    var step = 0f
    while (step <= 1.001f) {
        val mixed = lerp(palette.primary, palette.text, step.coerceIn(0f, 1f))
        if (contrastRatio(mixed, palette.surface) >= 4.5f && contrastRatio(mixed, palette.bg) >= 4.5f) {
            return mixed
        }
        step += 0.05f
    }
    return palette.text
}

fun parseHexColor(hex: String, fallback: Color = SubjectColors[0]): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> Color(("FF$clean").toLong(16))
            8 -> Color(clean.toLong(16))
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}

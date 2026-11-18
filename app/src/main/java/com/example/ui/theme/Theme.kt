package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class MudhakiratiColors(
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val field: Color,
    val accent: Color,
    val secondary: Color,
    val accentText: Color,
    val accentInk: Color,
    val soft: Color,
    val bar: Color,
    val track: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroInk: Color,
    val hot: Color,
    val ok: Color,
    val isDark: Boolean,
    val isWhiteYellow: Boolean,
)

val LocalMudhakiratiColors = staticCompositionLocalOf {
    buildMudhakiratiColors(AppPalettes[0])
}

fun buildMudhakiratiColors(p: AppPalette): MudhakiratiColors {
    val isWy = p.id == "white-yellow"
    val onPrim = if (isWy) Color(0xFF1B1A14) else onPrimaryFor(p)
    val primTxt = primaryTextFor(p)
    val soft = if (isWy) Color(0xFFFFF4CC) else lerp(p.surface, p.primary, 0.18f)
    val bar = if (isWy) Color(0xFFFFE082) else lerp(p.surface, p.secondary, 0.55f)
    val track = if (isWy) Color(0xFFF4EFE0) else lerp(p.bg, p.border, 0.62f)
    val heroStart = when (p.id) {
        "white-yellow" -> Color(0xFFF5B800)
        "dark" -> Color(0xFF2F49C7)
        else -> p.primary
    }
    val heroEnd = when (p.id) {
        "white-yellow" -> Color(0xFFFFD54A)
        "light" -> Color(0xFF5B78FF)
        "dark" -> Color(0xFF5B78FF)
        else -> lerp(heroStart, p.secondary, 0.28f)
    }
    val heroInk = when (p.id) {
        "white-yellow" -> Color(0xFF1B1A14)
        "dark", "light" -> Color.White
        else -> onPrim
    }
    val hot = if (p.isDark) Color(0xFFFF826E) else Color(0xFFD94A38)
    val ok = if (p.isDark) Color(0xFF3DD6A3) else Color(0xFF158058)
    return MudhakiratiColors(
        bg = p.bg,
        surface = p.surface,
        ink = p.text,
        muted = p.muted,
        line = p.border,
        field = p.input,
        accent = p.primary,
        secondary = p.secondary,
        accentText = primTxt,
        accentInk = onPrim,
        soft = soft,
        bar = bar,
        track = track,
        heroStart = heroStart,
        heroEnd = heroEnd,
        heroInk = heroInk,
        hot = hot,
        ok = ok,
        isDark = p.isDark,
        isWhiteYellow = isWy,
    )
}

fun resolvePalette(themeMode: String, paletteId: String, systemDark: Boolean): AppPalette {
    return when (themeMode) {
        "custom" -> findPalette(paletteId)
        "light" -> LightSystemPalette
        "dark" -> DarkSystemPalette
        else -> if (systemDark) AutoWarmDarkPalette else AppPalettes[0]
    }
}

private val MudhakiratiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MudhakiratiTheme(
    themeMode: String = "custom",
    paletteId: String = "white-yellow",
    fontScale: Float = 1f,
    lang: String = "ar",
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val palette = resolvePalette(themeMode, paletteId, systemDark)
    val mzColors = buildMudhakiratiColors(palette)

    val m3Scheme = if (mzColors.isDark) {
        darkColorScheme(
            primary = mzColors.accent,
            onPrimary = mzColors.accentInk,
            primaryContainer = mzColors.soft,
            onPrimaryContainer = mzColors.accentText,
            secondary = mzColors.secondary,
            background = mzColors.bg,
            onBackground = mzColors.ink,
            surface = mzColors.surface,
            onSurface = mzColors.ink,
            surfaceVariant = mzColors.field,
            onSurfaceVariant = mzColors.muted,
            outline = mzColors.line,
            error = mzColors.hot,
            onError = Color.White,
        )
    } else {
        lightColorScheme(
            primary = mzColors.accent,
            onPrimary = mzColors.accentInk,
            primaryContainer = mzColors.soft,
            onPrimaryContainer = mzColors.accentText,
            secondary = mzColors.secondary,
            background = mzColors.bg,
            onBackground = mzColors.ink,
            surface = mzColors.surface,
            onSurface = mzColors.ink,
            surfaceVariant = mzColors.field,
            onSurfaceVariant = mzColors.muted,
            outline = mzColors.line,
            error = mzColors.hot,
            onError = Color.White,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val ctrl = WindowCompat.getInsetsController(window, view)
            ctrl.isAppearanceLightStatusBars = !mzColors.isDark
            ctrl.isAppearanceLightNavigationBars = !mzColors.isDark
        }
    }

    val dir = if (lang == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(
        LocalMudhakiratiColors provides mzColors,
        LocalLayoutDirection provides dir,
    ) {
        MaterialTheme(
            colorScheme = m3Scheme,
            typography = mudhakiratiTypography(fontScale),
            shapes = MudhakiratiShapes,
            content = content,
        )
    }
}

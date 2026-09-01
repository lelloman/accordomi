package com.lelloman.accordomi.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.lelloman.accordomi.domain.settings.BuiltInTheme
import com.lelloman.accordomi.domain.settings.ThemeId
import com.lelloman.accordomi.domain.settings.ThemePalette
import com.lelloman.accordomi.domain.settings.resolvePalette

@Immutable
data class AccordomiColors(
    val inTune: Color,
    val offPitch: Color,
    val pianoWhite: Color,
    val pianoBlack: Color,
)

private val LocalAccordomiColors = staticCompositionLocalOf {
    AccordomiColors(
        inTune = Color.Unspecified,
        offPitch = Color.Unspecified,
        pianoWhite = Color.Unspecified,
        pianoBlack = Color.Unspecified,
    )
}

val MaterialTheme.accordomiColors: AccordomiColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAccordomiColors.current

@Composable
fun AccordomiTheme(
    selectedThemeId: ThemeId = BuiltInTheme.System.id,
    customPalette: ThemePalette? = null,
    systemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val selectedTheme = BuiltInTheme.fromId(selectedThemeId) ?: BuiltInTheme.System
    val palette = customPalette ?: selectedTheme.resolvePalette(systemDark)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !palette.isDark
                isAppearanceLightNavigationBars = !palette.isDark
            }
        }
    }
    CompositionLocalProvider(LocalAccordomiColors provides palette.toAccordomiColors()) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = Typography,
            content = content,
        )
    }
}

private fun ThemePalette.toAccordomiColors() = AccordomiColors(
    inTune = Color(inTune),
    offPitch = Color(offPitch),
    pianoWhite = Color(pianoWhite),
    pianoBlack = Color(pianoBlack),
)

private fun ThemePalette.toColorScheme(): ColorScheme {
    val backgroundColor = Color(background)
    val surfaceColor = Color(surface)
    val variantColor = Color(surfaceVariant)
    val accentColor = Color(accent)
    val textColor = Color(text)
    val secondaryTextColor = Color(secondaryText)
    val outlineColor = Color(outline)
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accentColor,
        onPrimary = Color(onAccent),
        primaryContainer = variantColor,
        onPrimaryContainer = textColor,
        secondary = accentColor,
        onSecondary = Color(onAccent),
        secondaryContainer = variantColor,
        onSecondaryContainer = textColor,
        tertiary = Color(offPitch),
        onTertiary = Color(onAccent),
        tertiaryContainer = variantColor,
        onTertiaryContainer = textColor,
        error = Color(error),
        onError = Color(onError),
        errorContainer = variantColor,
        onErrorContainer = textColor,
        background = backgroundColor,
        onBackground = textColor,
        surface = surfaceColor,
        onSurface = textColor,
        surfaceVariant = variantColor,
        onSurfaceVariant = secondaryTextColor,
        outline = outlineColor,
        outlineVariant = outlineColor.copy(alpha = 0.55f),
        surfaceContainer = surfaceColor,
        surfaceContainerHigh = variantColor,
    )
}

package com.lelloman.accordomi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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
    systemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val selectedTheme = BuiltInTheme.fromId(selectedThemeId) ?: BuiltInTheme.System
    val palette = selectedTheme.resolvePalette(systemDark)
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

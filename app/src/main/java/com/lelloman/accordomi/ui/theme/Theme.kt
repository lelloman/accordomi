package com.lelloman.accordomi.ui.theme

import android.app.Activity
import com.lelloman.lellodesign.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
import com.lelloman.accordomi.domain.settings.androidTheme
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
    val selectedTheme = selectedThemeId.androidTheme()
    val palette = selectedTheme.resolvePalette(systemDark)
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
        val lello = LelloPalettes.forProduct("green", palette.isDark)
        LelloTheme(palette = lello, content = content)
    }
}

private fun ThemePalette.toAccordomiColors() = AccordomiColors(
    inTune = Color(inTune),
    offPitch = Color(offPitch),
    pianoWhite = Color(pianoWhite),
    pianoBlack = Color(pianoBlack),
)

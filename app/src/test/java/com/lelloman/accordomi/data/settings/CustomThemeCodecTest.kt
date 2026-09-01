package com.lelloman.accordomi.data.settings

import com.lelloman.accordomi.domain.settings.CustomTheme
import com.lelloman.accordomi.domain.settings.ThemeId
import com.lelloman.accordomi.domain.settings.ThemePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomThemeCodecTest {
    @Test
    fun roundTripsNamesAndUnsignedArgbColors() {
        val themes = listOf(
            CustomTheme(
                id = ThemeId("custom:test"),
                name = "Blu; notte | 🎹",
                palette = samplePalette,
            ),
        )

        assertEquals(themes, decodeCustomThemes(encodeCustomThemes(themes)))
    }

    @Test
    fun skipsMalformedEntriesWithoutDroppingValidThemes() {
        val valid = CustomTheme(
            id = ThemeId("custom:valid"),
            name = "Valid",
            palette = samplePalette,
        )
        val encoded = "broken;${encodeCustomThemes(listOf(valid))};also|broken"

        assertEquals(listOf(valid), decodeCustomThemes(encoded))
    }

    @Test
    fun emptyValueProducesEmptyList() {
        assertTrue(decodeCustomThemes(null).isEmpty())
        assertTrue(decodeCustomThemes("").isEmpty())
    }

    private val samplePalette = ThemePalette(
        isDark = true,
        background = 0xFF010203.toInt(),
        surface = 0xFF111213.toInt(),
        surfaceVariant = 0xFF212223.toInt(),
        accent = 0xFF313233.toInt(),
        onAccent = 0xFF414243.toInt(),
        text = 0xFF515253.toInt(),
        secondaryText = 0xFF616263.toInt(),
        outline = 0xFF717273.toInt(),
        inTune = 0xFF818283.toInt(),
        offPitch = 0xFF919293.toInt(),
        pianoWhite = 0xFFA1A2A3.toInt(),
        pianoBlack = 0xFFB1B2B3.toInt(),
        error = 0xFFC1C2C3.toInt(),
        onError = 0xFFD1D2D3.toInt(),
    )
}

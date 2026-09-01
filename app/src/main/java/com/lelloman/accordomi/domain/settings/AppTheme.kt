package com.lelloman.accordomi.domain.settings

@JvmInline
value class ThemeId(val value: String)

enum class BuiltInTheme(
    val id: ThemeId,
) {
    System(ThemeId("system")),
    Light(ThemeId("light")),
    Dark(ThemeId("dark")),
    Concert(ThemeId("concert")),
    Ocean(ThemeId("ocean")),
    Forest(ThemeId("forest"));

    companion object {
        fun fromId(id: ThemeId): BuiltInTheme? = entries.firstOrNull { it.id == id }
    }
}

data class ThemePalette(
    val isDark: Boolean,
    val background: Int,
    val surface: Int,
    val surfaceVariant: Int,
    val accent: Int,
    val onAccent: Int,
    val text: Int,
    val secondaryText: Int,
    val outline: Int,
    val inTune: Int,
    val offPitch: Int,
    val pianoWhite: Int,
    val pianoBlack: Int,
)

fun BuiltInTheme.resolvePalette(systemDark: Boolean): ThemePalette = when (this) {
    BuiltInTheme.System -> if (systemDark) DarkPalette else LightPalette
    BuiltInTheme.Light -> LightPalette
    BuiltInTheme.Dark -> DarkPalette
    BuiltInTheme.Concert -> ConcertPalette
    BuiltInTheme.Ocean -> OceanPalette
    BuiltInTheme.Forest -> ForestPalette
}

private val LightPalette = ThemePalette(
    isDark = false,
    background = 0xFFFFFBF5.toInt(),
    surface = 0xFFFFFFFF.toInt(),
    surfaceVariant = 0xFFF2EDE5.toInt(),
    accent = 0xFF5F5791.toInt(),
    onAccent = 0xFFFFFFFF.toInt(),
    text = 0xFF1D1B20.toInt(),
    secondaryText = 0xFF625F68.toInt(),
    outline = 0xFF817D87.toInt(),
    inTune = 0xFF167A55.toInt(),
    offPitch = 0xFFB05B22.toInt(),
    pianoWhite = 0xFFF7F4EB.toInt(),
    pianoBlack = 0xFF191A1F.toInt(),
)

private val DarkPalette = ThemePalette(
    isDark = true,
    background = 0xFF101318.toInt(),
    surface = 0xFF181B21.toInt(),
    surfaceVariant = 0xFF252A33.toInt(),
    accent = 0xFFAEC6FF.toInt(),
    onAccent = 0xFF17305F.toInt(),
    text = 0xFFE6EAF2.toInt(),
    secondaryText = 0xFFBCC2CC.toInt(),
    outline = 0xFF858B95.toInt(),
    inTune = 0xFF68D6A6.toInt(),
    offPitch = 0xFFF2A66F.toInt(),
    pianoWhite = 0xFFF4F1E8.toInt(),
    pianoBlack = 0xFF17181C.toInt(),
)

private val ConcertPalette = ThemePalette(
    isDark = true,
    background = 0xFF15110E.toInt(),
    surface = 0xFF211A16.toInt(),
    surfaceVariant = 0xFF2C221B.toInt(),
    accent = 0xFFD9B36C.toInt(),
    onAccent = 0xFF3A2B0E.toInt(),
    text = 0xFFF8EBD8.toInt(),
    secondaryText = 0xFFCBBBA5.toInt(),
    outline = 0xFF8C7B68.toInt(),
    inTune = 0xFF78C99B.toInt(),
    offPitch = 0xFFDB8468.toInt(),
    pianoWhite = 0xFFFFF4DD.toInt(),
    pianoBlack = 0xFF211813.toInt(),
)

private val OceanPalette = ThemePalette(
    isDark = true,
    background = 0xFF071820.toInt(),
    surface = 0xFF0D2733.toInt(),
    surfaceVariant = 0xFF123745.toInt(),
    accent = 0xFF5CD5E8.toInt(),
    onAccent = 0xFF00363E.toInt(),
    text = 0xFFE2F6FA.toInt(),
    secondaryText = 0xFFA8C8D0.toInt(),
    outline = 0xFF6D9099.toInt(),
    inTune = 0xFF62E6B2.toInt(),
    offPitch = 0xFFFFB36B.toInt(),
    pianoWhite = 0xFFF1FAFA.toInt(),
    pianoBlack = 0xFF071A20.toInt(),
)

private val ForestPalette = ThemePalette(
    isDark = false,
    background = 0xFFF5F7EF.toInt(),
    surface = 0xFFFCFFF7.toInt(),
    surfaceVariant = 0xFFE4EBDD.toInt(),
    accent = 0xFF42664B.toInt(),
    onAccent = 0xFFFFFFFF.toInt(),
    text = 0xFF182019.toInt(),
    secondaryText = 0xFF566158.toInt(),
    outline = 0xFF727D74.toInt(),
    inTune = 0xFF25845B.toInt(),
    offPitch = 0xFFB7643D.toInt(),
    pianoWhite = 0xFFFFFCF1.toInt(),
    pianoBlack = 0xFF182019.toInt(),
)

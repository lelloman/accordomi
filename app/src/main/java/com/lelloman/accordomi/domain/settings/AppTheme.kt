package com.lelloman.accordomi.domain.settings

@JvmInline
value class ThemeId(val value: String)

data class CustomTheme(
    val id: ThemeId,
    val name: String,
    val palette: ThemePalette,
)

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

val ThemeId.isCustom: Boolean
    get() = value.startsWith(CustomThemeIdPrefix)

fun customThemeId(uniquePart: String): ThemeId = ThemeId("$CustomThemeIdPrefix$uniquePart")

private const val CustomThemeIdPrefix = "custom:"

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
    val error: Int,
    val onError: Int,
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
    background = 0xFFF9FAFB.toInt(),
    surface = 0xFFFFFFFF.toInt(),
    surfaceVariant = 0xFFF3F4F6.toInt(),
    accent = 0xFF006C45.toInt(),
    onAccent = 0xFFFFFFFF.toInt(),
    text = 0xFF111827.toInt(),
    secondaryText = 0xFF374151.toInt(),
    outline = 0xFF6B7280.toInt(),
    inTune = 0xFF166534.toInt(),
    offPitch = 0xFF92400E.toInt(),
    pianoWhite = 0xFFF4F1E8.toInt(),
    pianoBlack = 0xFF17181C.toInt(),
    error = 0xFFB91C1C.toInt(),
    onError = 0xFFFFFFFF.toInt(),
)

private val DarkPalette = ThemePalette(
    isDark = true,
    background = 0xFF030712.toInt(),
    surface = 0xFF111827.toInt(),
    surfaceVariant = 0xFF030712.toInt(),
    accent = 0xFF3DDC84.toInt(),
    onAccent = 0xFF002113.toInt(),
    text = 0xFFF9FAFB.toInt(),
    secondaryText = 0xFFE5E7EB.toInt(),
    outline = 0xFFD1D5DB.toInt(),
    inTune = 0xFF86EFAC.toInt(),
    offPitch = 0xFFFCD34D.toInt(),
    pianoWhite = 0xFFF4F1E8.toInt(),
    pianoBlack = 0xFF17181C.toInt(),
    error = 0xFFFCA5A5.toInt(),
    onError = 0xFF450A0A.toInt(),
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
    error = 0xFFFFB4AB.toInt(),
    onError = 0xFF690005.toInt(),
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
    error = 0xFFFFB4AB.toInt(),
    onError = 0xFF690005.toInt(),
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
    error = 0xFFBA1A1A.toInt(),
    onError = 0xFFFFFFFF.toInt(),
)

/** Android appearance choices; retired saved themes fall back to the system. */
val AndroidThemes = listOf(BuiltInTheme.Light, BuiltInTheme.Dark, BuiltInTheme.System)

fun ThemeId.androidTheme(): BuiltInTheme =
    AndroidThemes.firstOrNull { it.id == this } ?: BuiltInTheme.System

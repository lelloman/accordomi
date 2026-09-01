package com.lelloman.accordomi.data.settings

import com.lelloman.accordomi.domain.settings.CustomTheme
import com.lelloman.accordomi.domain.settings.ThemeId
import com.lelloman.accordomi.domain.settings.ThemePalette

internal fun encodeCustomThemes(themes: List<CustomTheme>): String = themes.joinToString(";") { theme ->
    buildList {
        add(theme.id.value)
        add(theme.name.encodeHex())
        add(if (theme.palette.isDark) "1" else "0")
        addAll(theme.palette.colors().map(Int::encodeColor))
    }.joinToString("|")
}

internal fun decodeCustomThemes(value: String?): List<CustomTheme> =
    value.orEmpty()
        .split(';')
        .filter(String::isNotBlank)
        .mapNotNull(::decodeCustomTheme)

private fun decodeCustomTheme(value: String): CustomTheme? = runCatching {
    val fields = value.split('|')
    require(fields.size == EncodedFieldCount)
    val colors = fields.drop(3).map { it.toUInt(16).toInt() }
    CustomTheme(
        id = ThemeId(fields[0]),
        name = fields[1].decodeHex(),
        palette = ThemePalette(
            isDark = fields[2] == "1",
            background = colors[0],
            surface = colors[1],
            surfaceVariant = colors[2],
            accent = colors[3],
            onAccent = colors[4],
            text = colors[5],
            secondaryText = colors[6],
            outline = colors[7],
            inTune = colors[8],
            offPitch = colors[9],
            pianoWhite = colors[10],
            pianoBlack = colors[11],
            error = colors[12],
            onError = colors[13],
        ),
    )
}.getOrNull()

private fun ThemePalette.colors() = listOf(
    background,
    surface,
    surfaceVariant,
    accent,
    onAccent,
    text,
    secondaryText,
    outline,
    inTune,
    offPitch,
    pianoWhite,
    pianoBlack,
    error,
    onError,
)

private fun Int.encodeColor(): String = toUInt().toString(16).padStart(8, '0')

private fun String.encodeHex(): String = encodeToByteArray().joinToString("") { byte ->
    (byte.toInt() and 0xff).toString(16).padStart(2, '0')
}

private fun String.decodeHex(): String {
    require(length % 2 == 0)
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
        .decodeToString()
}

private const val EncodedFieldCount = 17

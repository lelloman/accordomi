package com.lelloman.accordomi.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lelloman.accordomi.R
import com.lelloman.accordomi.domain.settings.*
import com.lelloman.lellodesign.*

@Composable
fun CustomThemeEditorDialog(existingTheme: CustomTheme?, startingPalette: ThemePalette,
    onDismiss: () -> Unit, onSave: (ThemeId?, String, ThemePalette) -> Unit, onDelete: (() -> Unit)?) {
    LelloThemeEditorDialog(
        initialName = existingTheme?.name ?: stringResource(R.string.theme_custom_default_name),
        initialDark = startingPalette.isDark,
        fields = PaletteRole.entries.map { LelloColorField(it.name, stringResource(it.labelRes), startingPalette.color(it)) },
        onDismiss = onDismiss, onDelete = onDelete,
        onSave = { name, dark, colors ->
            val palette = PaletteRole.entries.fold(startingPalette.copy(isDark = dark)) { palette, role ->
                palette.withColor(role, colors.getValue(role.name))
            }
            onSave(existingTheme?.id, name, palette)
        },
        labels = LelloThemeEditorLabels(
            title = stringResource(if (existingTheme == null) R.string.theme_create_custom else R.string.theme_edit_custom),
            name = stringResource(R.string.theme_name), dark = stringResource(R.string.theme_dark_palette),
            darkDescription = stringResource(R.string.theme_dark_palette_description), save = stringResource(R.string.save),
            cancel = stringResource(R.string.cancel), delete = stringResource(R.string.delete), apply = stringResource(R.string.apply),
            red = stringResource(R.string.color_red), green = stringResource(R.string.color_green), blue = stringResource(R.string.color_blue),
        ),
    )
}

private enum class PaletteRole(val labelRes: Int) {
    Background(R.string.palette_background),
    Surface(R.string.palette_surface),
    SurfaceVariant(R.string.palette_surface_variant),
    Accent(R.string.palette_accent),
    OnAccent(R.string.palette_on_accent),
    Text(R.string.palette_text),
    SecondaryText(R.string.palette_secondary_text),
    Outline(R.string.palette_outline),
    InTune(R.string.palette_in_tune),
    OffPitch(R.string.palette_off_pitch),
    PianoWhite(R.string.palette_piano_white),
    PianoBlack(R.string.palette_piano_black),
    Error(R.string.palette_error),
    OnError(R.string.palette_on_error),
}

private fun ThemePalette.color(role: PaletteRole): Int = when (role) {
    PaletteRole.Background -> background
    PaletteRole.Surface -> surface
    PaletteRole.SurfaceVariant -> surfaceVariant
    PaletteRole.Accent -> accent
    PaletteRole.OnAccent -> onAccent
    PaletteRole.Text -> text
    PaletteRole.SecondaryText -> secondaryText
    PaletteRole.Outline -> outline
    PaletteRole.InTune -> inTune
    PaletteRole.OffPitch -> offPitch
    PaletteRole.PianoWhite -> pianoWhite
    PaletteRole.PianoBlack -> pianoBlack
    PaletteRole.Error -> error
    PaletteRole.OnError -> onError
}

private fun ThemePalette.withColor(role: PaletteRole, color: Int): ThemePalette = when (role) {
    PaletteRole.Background -> copy(background = color)
    PaletteRole.Surface -> copy(surface = color)
    PaletteRole.SurfaceVariant -> copy(surfaceVariant = color)
    PaletteRole.Accent -> copy(accent = color)
    PaletteRole.OnAccent -> copy(onAccent = color)
    PaletteRole.Text -> copy(text = color)
    PaletteRole.SecondaryText -> copy(secondaryText = color)
    PaletteRole.Outline -> copy(outline = color)
    PaletteRole.InTune -> copy(inTune = color)
    PaletteRole.OffPitch -> copy(offPitch = color)
    PaletteRole.PianoWhite -> copy(pianoWhite = color)
    PaletteRole.PianoBlack -> copy(pianoBlack = color)
    PaletteRole.Error -> copy(error = color)
    PaletteRole.OnError -> copy(onError = color)
}

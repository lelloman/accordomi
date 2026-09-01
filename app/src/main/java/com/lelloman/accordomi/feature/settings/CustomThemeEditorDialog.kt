package com.lelloman.accordomi.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lelloman.accordomi.R
import com.lelloman.accordomi.domain.settings.CustomTheme
import com.lelloman.accordomi.domain.settings.ThemeId
import com.lelloman.accordomi.domain.settings.ThemePalette

@Composable
fun CustomThemeEditorDialog(
    existingTheme: CustomTheme?,
    startingPalette: ThemePalette,
    onDismiss: () -> Unit,
    onSave: (ThemeId?, String, ThemePalette) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val defaultName = stringResource(R.string.theme_custom_default_name)
    var name by remember(existingTheme?.id) {
        mutableStateOf(existingTheme?.name ?: defaultName)
    }
    var palette by remember(existingTheme?.id) { mutableStateOf(startingPalette) }
    var selectedRole by remember { mutableStateOf<PaletteRole?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(
                        if (existingTheme == null) {
                            R.string.theme_create_custom
                        } else {
                            R.string.theme_edit_custom
                        },
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.theme_name)) },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.theme_dark_palette))
                        Text(
                            text = stringResource(R.string.theme_dark_palette_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = palette.isDark,
                        onCheckedChange = { palette = palette.copy(isDark = it) },
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaletteRole.entries.forEach { role ->
                        PaletteColorRow(
                            role = role,
                            color = palette.color(role),
                            onClick = { selectedRole = role },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text(
                                text = stringResource(R.string.delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = { onSave(existingTheme?.id, name, palette) },
                        enabled = name.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }

    selectedRole?.let { role ->
        RgbColorDialog(
            role = role,
            initialColor = palette.color(role),
            onDismiss = { selectedRole = null },
            onConfirm = { color ->
                palette = palette.withColor(role, color)
                selectedRole = null
            },
        )
    }
}

@Composable
private fun PaletteColorRow(
    role: PaletteRole,
    color: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(color), RoundedCornerShape(10.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp),
                ),
        )
        Text(
            text = stringResource(role.labelRes),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = color.toHexColor(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RgbColorDialog(
    role: PaletteRole,
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var red by remember(initialColor) { mutableFloatStateOf(initialColor.red().toFloat()) }
    var green by remember(initialColor) { mutableFloatStateOf(initialColor.green().toFloat()) }
    var blue by remember(initialColor) { mutableFloatStateOf(initialColor.blue().toFloat()) }
    val color = rgb(red.toInt(), green.toInt(), blue.toInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(role.labelRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(Color(color), MaterialTheme.shapes.medium)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            MaterialTheme.shapes.medium,
                        ),
                )
                RgbSlider("R", red) { red = it }
                RgbSlider("G", green) { green = it }
                RgbSlider("B", blue) { blue = it }
                Text(
                    text = color.toHexColor(),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(color) }) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun RgbSlider(
    channel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(channel, modifier = Modifier.size(20.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            valueRange = 0f..255f,
            steps = 254,
        )
        Text(value.toInt().toString(), modifier = Modifier.size(width = 34.dp, height = 24.dp))
    }
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

private fun Int.red(): Int = this ushr 16 and 0xff
private fun Int.green(): Int = this ushr 8 and 0xff
private fun Int.blue(): Int = this and 0xff
private fun rgb(red: Int, green: Int, blue: Int): Int =
    (0xff shl 24) or (red shl 16) or (green shl 8) or blue

private fun Int.toHexColor(): String = "#${toUInt().toString(16).takeLast(6).uppercase()}"

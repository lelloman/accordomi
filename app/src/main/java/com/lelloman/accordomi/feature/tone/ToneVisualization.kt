package com.lelloman.accordomi.feature.tone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelloman.accordomi.R
import com.lelloman.accordomi.domain.tone.PitchReading
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ToneVisualization(
    reading: PitchReading,
    style: ToneVisualizationStyle,
    modifier: Modifier = Modifier,
) {
    when (style) {
        ToneVisualizationStyle.Text -> DetailedToneVisualization(reading, modifier)
        ToneVisualizationStyle.Needle -> NeedleToneVisualization(reading, modifier)
        ToneVisualizationStyle.SideWheel -> SideWheelToneVisualization(reading, modifier)
        ToneVisualizationStyle.PianoKeyboard -> PianoKeyboardToneVisualization(reading, modifier)
    }
}

@Composable
private fun DetailedToneVisualization(
    reading: PitchReading,
    modifier: Modifier = Modifier,
) {
    TunerPanel(modifier = modifier) {
        NoteHeading(reading)
        Text(
            text = stringResource(R.string.frequency_hz, reading.frequencyHz),
            style = MaterialTheme.typography.headlineSmall,
        )
        TuningStatus(reading)
        TuningRail(
            centsOff = reading.centsOff,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.target_frequency_hz, reading.targetFrequencyHz),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NeedleToneVisualization(
    reading: PitchReading,
    modifier: Modifier = Modifier,
) {
    val marker = tuningMarker(reading.centsOff)
    val outline = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface
    val normalized = reading.centsOff.normalizedCents()

    TunerPanel(modifier = modifier) {
        NoteHeading(reading)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        ) {
            val pivot = Offset(size.width / 2f, size.height * 0.86f)
            val radius = minOf(size.width * 0.42f, size.height * 0.78f)
            val startAngle = 210f
            val sweepAngle = 120f
            val bounds = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(pivot.x - radius, pivot.y - radius)

            drawArc(
                color = outline,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = bounds,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
            repeat(NeedleTickCount) { index ->
                val fraction = index / (NeedleTickCount - 1f)
                val angle = startAngle + sweepAngle * fraction
                val radians = angle.toRadians()
                val isCenter = index == NeedleTickCount / 2
                val outer = Offset(
                    pivot.x + cos(radians) * radius,
                    pivot.y + sin(radians) * radius,
                )
                val innerRadius = radius - (if (isCenter) 22.dp.toPx() else 13.dp.toPx())
                val inner = Offset(
                    pivot.x + cos(radians) * innerRadius,
                    pivot.y + sin(radians) * innerRadius,
                )
                drawLine(
                    color = if (isCenter) marker else outline,
                    start = inner,
                    end = outer,
                    strokeWidth = if (isCenter) 4.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            val needleAngle = 270f + normalized * 60f
            val radians = needleAngle.toRadians()
            val needleEnd = Offset(
                pivot.x + cos(radians) * radius * 0.88f,
                pivot.y + sin(radians) * radius * 0.88f,
            )
            drawLine(
                color = marker,
                start = pivot,
                end = needleEnd,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = marker, radius = 9.dp.toPx(), center = pivot)
            drawCircle(color = surface, radius = 3.dp.toPx(), center = pivot)
        }
        TuningStatus(reading)
        ReadingSummary(reading)
    }
}

@Composable
private fun SideWheelToneVisualization(
    reading: PitchReading,
    modifier: Modifier = Modifier,
) {
    val marker = tuningMarker(reading.centsOff)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outlineVariant
    val normalized = reading.centsOff.normalizedCents()

    TunerPanel(modifier = modifier) {
        NoteHeading(reading)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            val horizontalInset = 10.dp.toPx()
            val top = 14.dp.toPx()
            val wheelHeight = size.height - 28.dp.toPx()
            val wheelWidth = size.width - horizontalInset * 2f
            val cornerRadius = wheelHeight / 2f
            drawRoundRect(
                color = track,
                topLeft = Offset(horizontalInset, top),
                size = Size(wheelWidth, wheelHeight),
                cornerRadius = CornerRadius(cornerRadius),
            )
            drawRoundRect(
                color = outline,
                topLeft = Offset(horizontalInset, top),
                size = Size(wheelWidth, wheelHeight),
                cornerRadius = CornerRadius(cornerRadius),
                style = Stroke(width = 2.dp.toPx()),
            )

            val centerX = size.width / 2f
            val spacing = wheelWidth / 10f
            val shift = -normalized * spacing * 4f
            for (index in -9..9) {
                val x = centerX + index * spacing + shift
                if (x <= horizontalInset + 8.dp.toPx() ||
                    x >= size.width - horizontalInset - 8.dp.toPx()
                ) {
                    continue
                }
                val isMajor = index % 2 == 0
                val tickHeight = wheelHeight * if (isMajor) 0.62f else 0.34f
                drawLine(
                    color = outline,
                    start = Offset(x, size.height / 2f - tickHeight / 2f),
                    end = Offset(x, size.height / 2f + tickHeight / 2f),
                    strokeWidth = if (isMajor) 3.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawRoundRect(
                color = marker.copy(alpha = 0.16f),
                topLeft = Offset(centerX - 16.dp.toPx(), top + 4.dp.toPx()),
                size = Size(32.dp.toPx(), wheelHeight - 8.dp.toPx()),
                cornerRadius = CornerRadius(16.dp.toPx()),
            )
            drawLine(
                color = marker,
                start = Offset(centerX, top + 12.dp.toPx()),
                end = Offset(centerX, top + wheelHeight - 12.dp.toPx()),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        TuningStatus(reading)
        ReadingSummary(reading)
    }
}

@Composable
private fun PianoKeyboardToneVisualization(
    reading: PitchReading,
    modifier: Modifier = Modifier,
) {
    val marker = tuningMarker(reading.centsOff)
    val outline = MaterialTheme.colorScheme.outline
    val selectedNote = reading.noteName.takeWhile { !it.isDigit() }

    TunerPanel(modifier = modifier) {
        NoteHeading(reading)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
        ) {
            val whiteNotes = listOf("C", "D", "E", "F", "G", "A", "B")
            val blackNotes = listOf("C#" to 0, "D#" to 1, "F#" to 3, "G#" to 4, "A#" to 5)
            val gap = 2.dp.toPx()
            val whiteWidth = size.width / whiteNotes.size
            val whiteKeyColor = Color(0xFFF7F4EB)
            val blackKeyColor = Color(0xFF191A1F)

            whiteNotes.forEachIndexed { index, note ->
                val topLeft = Offset(index * whiteWidth + gap / 2f, 0f)
                val keySize = Size(whiteWidth - gap, size.height)
                drawRoundRect(
                    color = if (selectedNote == note) marker else whiteKeyColor,
                    topLeft = topLeft,
                    size = keySize,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                drawRoundRect(
                    color = outline,
                    topLeft = topLeft,
                    size = keySize,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            blackNotes.forEach { (note, leftWhiteIndex) ->
                val blackWidth = whiteWidth * 0.58f
                val x = (leftWhiteIndex + 1) * whiteWidth - blackWidth / 2f
                drawRoundRect(
                    color = if (selectedNote == note) marker else blackKeyColor,
                    topLeft = Offset(x, 0f),
                    size = Size(blackWidth, size.height * 0.62f),
                    cornerRadius = CornerRadius(
                        x = 4.dp.toPx(),
                        y = 8.dp.toPx(),
                    ),
                )
            }
        }
        TuningStatus(reading)
        TuningRail(centsOff = reading.centsOff)
    }
}

@Composable
private fun TunerPanel(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun NoteHeading(reading: PitchReading) {
    Text(
        text = reading.noteName,
        style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp),
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun TuningStatus(reading: PitchReading) {
    val marker = tuningMarker(reading.centsOff)
    Text(
        text = stringResource(reading.centsOff.statusLabel()),
        color = marker,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = stringResource(R.string.cents_off, reading.centsOff),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun TuningRail(
    centsOff: Double,
    modifier: Modifier = Modifier,
) {
    val marker = tuningMarker(centsOff)
    val track = MaterialTheme.colorScheme.outlineVariant
    val panel = MaterialTheme.colorScheme.surfaceContainer
    val normalized = centsOff.normalizedCents()
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            val inset = 10.dp.toPx()
            val centerY = size.height / 2f
            drawLine(
                color = track,
                start = Offset(inset, centerY),
                end = Offset(size.width - inset, centerY),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            repeat(9) { index ->
                val fraction = index / 8f
                val x = inset + (size.width - inset * 2f) * fraction
                val isCenter = index == 4
                val halfHeight = if (isCenter) 11.dp.toPx() else 6.dp.toPx()
                drawLine(
                    color = if (isCenter) marker else track,
                    start = Offset(x, centerY - halfHeight),
                    end = Offset(x, centerY + halfHeight),
                    strokeWidth = if (isCenter) 3.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            val markerX = inset + (normalized + 1f) / 2f * (size.width - inset * 2f)
            drawCircle(color = panel, radius = 9.dp.toPx(), center = Offset(markerX, centerY))
            drawCircle(color = marker, radius = 6.dp.toPx(), center = Offset(markerX, centerY))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.tuning_flat_short),
                style = MaterialTheme.typography.labelSmall,
            )
            Text("0", style = MaterialTheme.typography.labelSmall)
            Text(
                stringResource(R.string.tuning_sharp_short),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ReadingSummary(reading: PitchReading) {
    Text(
        text = stringResource(R.string.frequency_hz, reading.frequencyHz),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.target_frequency_hz, reading.targetFrequencyHz),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun tuningMarker(centsOff: Double): Color =
    if (abs(centsOff) <= InTuneCents) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

private fun Double.statusLabel(): Int = when {
    this < -InTuneCents -> R.string.tuning_flat
    this > InTuneCents -> R.string.tuning_sharp
    else -> R.string.tuning_in_tune
}

private fun Double.normalizedCents(): Float =
    (coerceIn(-MaximumVisibleCents, MaximumVisibleCents) / MaximumVisibleCents).toFloat()

private fun Float.toRadians(): Float = (this * PI / 180.0).toFloat()

private const val InTuneCents = 5.0
private const val MaximumVisibleCents = 50.0
private const val NeedleTickCount = 9

package com.lelloman.accordomi.feature.tone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        ToneVisualizationStyle.Text -> TextToneVisualization(
            reading = reading,
            modifier = modifier,
        )
        ToneVisualizationStyle.Needle -> NeedleToneVisualization(
            reading = reading,
            modifier = modifier,
        )
        ToneVisualizationStyle.SideWheel -> SideWheelToneVisualization(
            reading = reading,
            modifier = modifier,
        )
    }
}

@Composable
private fun TextToneVisualization(
    reading: PitchReading,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = reading.noteName,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.frequency_hz, reading.frequencyHz),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.cents_off, reading.centsOff),
            color = if (abs(reading.centsOff) <= InTuneCents) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = stringResource(R.string.target_frequency_hz, reading.targetFrequencyHz),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun NeedleToneVisualization(
    reading: PitchReading,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val normalized = reading.centsOff.normalizedCents()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = reading.noteName,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            val pivot = Offset(size.width / 2f, size.height * 0.9f)
            val radius = size.minDimension * 0.72f
            val sweep = 60f
            val startAngle = 240f
            drawArc(
                color = outline,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(pivot.x - radius, pivot.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
            )
            val centerAngle = 270f
            val needleAngle = centerAngle + normalized * (sweep / 2f)
            val radians = needleAngle.toRadians()
            val needleEnd = Offset(
                x = pivot.x + cos(radians) * radius * 0.92f,
                y = pivot.y + sin(radians) * radius * 0.92f,
            )
            drawLine(
                color = if (abs(reading.centsOff) <= InTuneCents) primary else onSurface,
                start = pivot,
                end = needleEnd,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = primary, radius = 7.dp.toPx(), center = pivot)
        }
        ReadingSummary(reading = reading)
    }
}

@Composable
private fun SideWheelToneVisualization(
    reading: PitchReading,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val normalized = reading.centsOff.normalizedCents()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = reading.noteName,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            val wheelWidth = size.width * 0.82f
            val wheelHeight = size.height * 0.62f
            val left = (size.width - wheelWidth) / 2f
            val top = (size.height - wheelHeight) / 2f
            drawOval(
                color = surfaceVariant,
                topLeft = Offset(left, top),
                size = Size(wheelWidth, wheelHeight),
            )
            drawOval(
                color = outline,
                topLeft = Offset(left, top),
                size = Size(wheelWidth, wheelHeight),
                style = Stroke(width = 2.dp.toPx()),
            )

            val markerX = size.width / 2f + normalized * wheelWidth * 0.35f
            drawLine(
                color = primary,
                start = Offset(markerX, top + 10.dp.toPx()),
                end = Offset(markerX, top + wheelHeight - 10.dp.toPx()),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = outline,
                start = Offset(size.width / 2f, top),
                end = Offset(size.width / 2f, top + wheelHeight),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        ReadingSummary(reading = reading)
    }
}

@Composable
private fun ReadingSummary(reading: PitchReading) {
    Text(
        text = stringResource(R.string.reading_summary, reading.frequencyHz, reading.centsOff),
        style = MaterialTheme.typography.titleMedium,
        color = if (abs(reading.centsOff) <= InTuneCents) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
    Text(
        text = stringResource(R.string.target_frequency_hz, reading.targetFrequencyHz),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun Double.normalizedCents(): Float =
    (coerceIn(-MaximumVisibleCents, MaximumVisibleCents) / MaximumVisibleCents).toFloat()

private fun Float.toRadians(): Float = (this * PI / 180.0).toFloat()

private const val InTuneCents = 5.0
private const val MaximumVisibleCents = 50.0

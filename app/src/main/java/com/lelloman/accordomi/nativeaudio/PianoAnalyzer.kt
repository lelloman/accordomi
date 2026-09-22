package com.lelloman.accordomi.nativeaudio

/** Experimental known-note measurement API; it does not alter chromatic tuning targets.
 * Supply an isolated string and a long PCM window (typically 1–2 seconds).
 */
class PianoAnalyzer {
    fun analyze(samples: FloatArray, sampleRate: Int, expectedFirstPartialHz: Double): PianoMeasurement {
        val raw = NativeAudio.piano(samples, sampleRate, expectedFirstPartialHz)
        return PianoMeasurement(
            status = PianoMeasurementStatus.entries[raw[0].toInt()],
            firstPartialHz = raw[1], inharmonicity = raw[2], rmsCents = raw[3], quality = raw[4],
            partials = List(raw[6].toInt()) { index ->
                val offset = 7 + 6 * index
                PianoPartial(raw[offset].toInt(), raw[offset + 1], raw[offset + 2],
                    raw[offset + 3], raw[offset + 4], raw[offset + 5] != 0.0)
            },
        )
    }
}

enum class PianoMeasurementStatus { Usable, InvalidInput, Quiet, InsufficientPartials, PoorFit }

data class PianoPartial(
    val number: Int, val frequencyHz: Double, val amplitude: Double,
    val predictedHz: Double, val residualCents: Double, val used: Boolean,
)

data class PianoMeasurement(
    val status: PianoMeasurementStatus,
    val firstPartialHz: Double,
    val inharmonicity: Double,
    val rmsCents: Double,
    /** Heuristic fit quality, not a calibrated probability. */
    val quality: Double,
    val partials: List<PianoPartial>,
)

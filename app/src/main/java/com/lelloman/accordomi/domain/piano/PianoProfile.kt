package com.lelloman.accordomi.domain.piano

import com.lelloman.accordomi.nativeaudio.PianoMeasurement

val CalibrationNotes = listOf(21, 33, 45, 57, 60, 69, 81, 88, 93)

data class PianoSample(
    val midi: Int,
    val firstPartialHz: Double,
    val inharmonicity: Double,
    val quality: Double,
    val measuredAt: Long,
    val recordings: List<String>,
    val observations: List<PianoMeasurement>,
)

data class PianoTarget(val midi: Int, val frequencyHz: Double, val inharmonicity: Double)

data class PianoProfile(
    val id: String,
    val name: String,
    val referenceHz: Double,
    val createdAt: Long,
    val samples: List<PianoSample> = emptyList(),
    val targets: List<PianoTarget> = emptyList(),
    val modelVersion: Int = 1,
) {
    val ready: Boolean get() = targets.size == 88
}

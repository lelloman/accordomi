package com.lelloman.accordomi.domain.tone

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

object TuningMath {
    fun readingFor(
        frequencyHz: Double,
        clarity: Float,
        referencePitchHz: Double,
    ): PitchReading {
        val semitonesFromA4 = (12.0 * log2(frequencyHz / referencePitchHz)).roundToInt()
        val targetFrequencyHz = referencePitchHz * 2.0.pow(semitonesFromA4 / 12.0)
        val absoluteMidiNote = A4MidiNote + semitonesFromA4
        val noteName = noteName(absoluteMidiNote)
        val centsOff = 1200.0 * log2(frequencyHz / targetFrequencyHz)

        return PitchReading(
            frequencyHz = frequencyHz,
            clarity = clarity,
            noteName = noteName,
            centsOff = centsOff,
            targetFrequencyHz = targetFrequencyHz,
        )
    }

    private fun noteName(midiNote: Int): String {
        val note = NoteNames.floorMod(midiNote)
        val octave = midiNote / NoteNames.size - 1
        return "${NoteNames[note]}$octave"
    }

    private fun log2(value: Double): Double = ln(value) / ln(2.0)

    private fun Array<String>.floorMod(value: Int): Int =
        ((value % size) + size) % size

    private const val A4MidiNote = 69

    private val NoteNames = arrayOf(
        "C",
        "C#",
        "D",
        "D#",
        "E",
        "F",
        "F#",
        "G",
        "G#",
        "A",
        "A#",
        "B",
    )
}


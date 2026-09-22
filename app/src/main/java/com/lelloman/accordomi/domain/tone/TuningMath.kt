package com.lelloman.accordomi.domain.tone

import com.lelloman.accordomi.nativeaudio.NativeAudio

object TuningMath {
    fun readingFor(
        frequencyHz: Double,
        clarity: Float,
        referencePitchHz: Double,
    ): PitchReading {
        val result = NativeAudio.tuning(frequencyHz, referencePitchHz)
        val noteName = noteName(result[0].toInt())
        val targetFrequencyHz = result[1]
        val centsOff = result[2]

        return PitchReading(
            frequencyHz = frequencyHz,
            clarity = clarity,
            noteName = noteName,
            centsOff = centsOff,
            targetFrequencyHz = targetFrequencyHz,
        )
    }

    private fun noteName(midiNote: Int): String {
        val note = Math.floorMod(midiNote, NoteNames.size)
        val octave = Math.floorDiv(midiNote, NoteNames.size) - 1
        return "${NoteNames[note]}$octave"
    }

    fun frequencyFor(midiNote: Int, referencePitchHz: Double): Double =
        NativeAudio.equalTemperedHz(midiNote, referencePitchHz)

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

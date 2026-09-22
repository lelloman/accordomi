package com.lelloman.accordomi.data.audio

import com.lelloman.accordomi.nativeaudio.NativeAudio

/** Continuous phase and attack/release state, evaluated by the shared C engine. */
internal class ReferenceOscillator(val frequencyHz: Double, val sampleRate: Int = 44_100) {
    private val state = DoubleArray(2)
    init {
        require(frequencyHz.isFinite() && frequencyHz > 0 && frequencyHz < sampleRate / 2.0)
    }
    fun fill(buffer: ShortArray, releasing: Boolean = false) {
        NativeAudio.oscillator(state, frequencyHz, sampleRate, releasing, buffer)
    }
}

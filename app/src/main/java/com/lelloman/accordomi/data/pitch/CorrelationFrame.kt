package com.lelloman.accordomi.data.pitch

import com.lelloman.accordomi.nativeaudio.NativeAudio

/** Diagnostic access to the same native correlation used by the detectors. */
internal class CorrelationFrame(samples: FloatArray) {
    private val size = samples.size
    private val values = NativeAudio.correlate(samples)
    fun correlation(lag: Int): Double = values[lag]
    fun energyA(lag: Int): Double = values[size + lag]
    fun energyB(lag: Int): Double = values[2 * size + lag]
    fun difference(lag: Int): Double =
        (energyA(lag) + energyB(lag) - 2.0 * correlation(lag)).coerceAtLeast(0.0)
}

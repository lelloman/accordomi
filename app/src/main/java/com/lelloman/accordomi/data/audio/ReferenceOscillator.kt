package com.lelloman.accordomi.data.audio

import kotlin.math.PI
import kotlin.math.sin

/** Continuous phase across buffers; the attack avoids an abrupt amplitude step. */
internal class ReferenceOscillator(val frequencyHz: Double, val sampleRate: Int = 44_100) {
    private var phase = 0.0
    private var gain = 0.0
    private val gainStep = 1.0 / (sampleRate * 0.01)

    init {
        require(frequencyHz.isFinite() && frequencyHz > 0 && frequencyHz < sampleRate / 2.0)
    }

    fun fill(buffer: ShortArray, releasing: Boolean = false) {
        val step = 2.0 * PI * frequencyHz / sampleRate
        for (index in buffer.indices) {
            gain = (gain + if (releasing) -gainStep else gainStep).coerceIn(0.0, 1.0)
            buffer[index] = (sin(phase) * gain * 0.2 * Short.MAX_VALUE).toInt().toShort()
            phase = (phase + step) % (2.0 * PI)
        }
    }
}

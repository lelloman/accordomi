package com.lelloman.accordomi.data.pitch

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Linear (not circular) autocorrelation using a zero-padded radix-2 FFT. */
internal class CorrelationFrame(samples: FloatArray) {
    private val size = samples.size
    private val energy = DoubleArray(size + 1)
    private val correlation: DoubleArray

    init {
        var fftSize = 1
        while (fftSize < 2 * size) fftSize *= 2
        val real = DoubleArray(fftSize)
        val imaginary = DoubleArray(fftSize)
        val mean = samples.sumOf { it.toDouble() } / size
        for (index in samples.indices) {
            val sample = samples[index] - mean
            real[index] = sample
            energy[index + 1] = energy[index] + sample * sample
        }
        transform(real, imaginary, inverse = false)
        for (index in real.indices) {
            real[index] = real[index] * real[index] + imaginary[index] * imaginary[index]
            imaginary[index] = 0.0
        }
        transform(real, imaginary, inverse = true)
        correlation = real
    }

    fun correlation(lag: Int): Double = correlation[lag]
    fun energyA(lag: Int): Double = energy[size - lag]
    fun energyB(lag: Int): Double = energy[size] - energy[lag]
    fun difference(lag: Int): Double =
        (energyA(lag) + energyB(lag) - 2.0 * correlation(lag)).coerceAtLeast(0.0)

    private fun transform(real: DoubleArray, imaginary: DoubleArray, inverse: Boolean) {
        val n = real.size
        var reversed = 0
        for (index in 1 until n) {
            var bit = n shr 1
            while (reversed and bit != 0) {
                reversed = reversed xor bit
                bit = bit shr 1
            }
            reversed = reversed xor bit
            if (index < reversed) {
                val r = real[index]
                real[index] = real[reversed]
                real[reversed] = r
                val i = imaginary[index]
                imaginary[index] = imaginary[reversed]
                imaginary[reversed] = i
            }
        }
        var width = 2
        while (width <= n) {
            val angle = (if (inverse) 2.0 else -2.0) * PI / width
            val stepReal = cos(angle)
            val stepImaginary = sin(angle)
            val half = width / 2
            for (start in 0 until n step width) {
                var wr = 1.0
                var wi = 0.0
                for (offset in 0 until half) {
                    val a = start + offset
                    val b = a + half
                    val r = wr * real[b] - wi * imaginary[b]
                    val i = wr * imaginary[b] + wi * real[b]
                    real[b] = real[a] - r
                    imaginary[b] = imaginary[a] - i
                    real[a] += r
                    imaginary[a] += i
                    val nextReal = wr * stepReal - wi * stepImaginary
                    wi = wr * stepImaginary + wi * stepReal
                    wr = nextReal
                }
            }
            width *= 2
        }
        if (inverse) {
            for (index in real.indices) {
                real[index] /= n
                imaginary[index] /= n
            }
        }
    }
}

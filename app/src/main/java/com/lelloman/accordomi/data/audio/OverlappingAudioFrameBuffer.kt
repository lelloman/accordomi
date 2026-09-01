package com.lelloman.accordomi.data.audio

internal class OverlappingAudioFrameBuffer(
    private val frameSize: Int,
    private val hopSize: Int,
) {
    private val ringBuffer = FloatArray(frameSize)
    private var writeIndex = 0
    private var totalSamples = 0L
    private var samplesSinceEmission = 0
    private var hasEmitted = false

    init {
        require(frameSize > 0)
        require(hopSize in 1..frameSize)
    }

    fun append(samples: ShortArray, count: Int): List<FloatArray> {
        require(count in 0..samples.size)
        val frames = mutableListOf<FloatArray>()
        repeat(count) { index ->
            ringBuffer[writeIndex] = samples[index] / Short.MAX_VALUE.toFloat()
            writeIndex = (writeIndex + 1) % frameSize
            totalSamples++

            if (!hasEmitted && totalSamples >= frameSize) {
                frames += snapshot()
                hasEmitted = true
                samplesSinceEmission = 0
            } else if (hasEmitted) {
                samplesSinceEmission++
                if (samplesSinceEmission >= hopSize) {
                    frames += snapshot()
                    samplesSinceEmission = 0
                }
            }
        }
        return frames
    }

    private fun snapshot(): FloatArray = FloatArray(frameSize) { index ->
        ringBuffer[(writeIndex + index) % frameSize]
    }
}

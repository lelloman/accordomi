package com.lelloman.accordomi.data.audio

data class AudioFrame(
    val samples: FloatArray,
    val sampleRate: Int,
    val sequenceNumber: Long = 0L,
)

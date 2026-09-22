package com.lelloman.accordomi.data.pitch

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.nativeaudio.NativeAudio
import javax.inject.Inject

class McLeodPitchDetector @Inject constructor() : PitchDetector {
    override val method = ToneDetectionMethod.McLeod

    override fun detect(samples: FloatArray, sampleRate: Int): PitchDetectionResult? =
        NativeAudio.detect(samples, sampleRate, 2)?.let {
            PitchDetectionResult(it[0], it[1].toFloat())
        }
}

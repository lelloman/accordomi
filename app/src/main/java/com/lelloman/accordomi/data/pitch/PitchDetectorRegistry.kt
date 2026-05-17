package com.lelloman.accordomi.data.pitch

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PitchDetectorRegistry @Inject constructor(
    detectors: Set<@JvmSuppressWildcards PitchDetector>,
) {
    private val detectorsByMethod = detectors.associateBy { it.method }

    fun detectorFor(method: ToneDetectionMethod): PitchDetector =
        detectorsByMethod[method]
            ?: error("No pitch detector registered for ${method.storageKey}.")
}


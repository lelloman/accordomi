package com.lelloman.accordomi.data.pitch

class AutoCorrelationPitchDetectorTest : PitchDetectorContract() {
    override val detector: PitchDetector = AutoCorrelationPitchDetector()
}

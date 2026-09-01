package com.lelloman.accordomi.data.pitch

class YinPitchDetectorTest : PitchDetectorContract() {
    override val detector: PitchDetector = YinPitchDetector()
}

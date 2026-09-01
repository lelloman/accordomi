package com.lelloman.accordomi.data.pitch

class McLeodPitchDetectorTest : PitchDetectorContract() {
    override val detector: PitchDetector = McLeodPitchDetector()
}

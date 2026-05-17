package com.lelloman.accordomi.domain.tone

import javax.inject.Inject

class ObserveToneDetectionUseCase @Inject constructor(
    private val toneDetectionRepository: ToneDetectionRepository,
) {
    operator fun invoke() = toneDetectionRepository.readings()
}


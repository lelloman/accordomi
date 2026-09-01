package com.lelloman.accordomi.domain.tone

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveToneDetectionUseCase @Inject constructor(
    private val toneDetectionRepository: ToneDetectionRepository,
) {
    operator fun invoke(): Flow<ToneDetectionStatus> = toneDetectionRepository.readings()
}

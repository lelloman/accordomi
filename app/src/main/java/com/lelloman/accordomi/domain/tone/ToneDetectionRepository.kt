package com.lelloman.accordomi.domain.tone

import kotlinx.coroutines.flow.Flow

interface ToneDetectionRepository {
    fun readings(): Flow<ToneDetectionStatus>
}

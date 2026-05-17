package com.lelloman.accordomi.data.audio

import kotlinx.coroutines.flow.Flow

interface AudioRecorder {
    fun frames(): Flow<AudioFrame>
}


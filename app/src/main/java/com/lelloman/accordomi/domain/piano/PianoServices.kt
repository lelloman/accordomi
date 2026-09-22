package com.lelloman.accordomi.domain.piano

import com.lelloman.accordomi.nativeaudio.PianoMeasurement
import kotlinx.coroutines.flow.Flow

interface PianoProfiles {
    val profiles: Flow<List<PianoProfile>>
    suspend fun save(profile: PianoProfile)
}
data class PianoFrame(val measurement: PianoMeasurement, val sequence: Long, val seconds: Double)
interface PianoCaptureSource {
    fun readings(expectedHz: Double, knownB: Double? = null, recording: String? = null): Flow<PianoFrame>
    suspend fun discard(recording: String)
}

package com.lelloman.accordomi.domain.tone

fun interface ReferenceToneOutput {
    suspend fun play(frequencyHz: Double, shouldStop: () -> Boolean)
}

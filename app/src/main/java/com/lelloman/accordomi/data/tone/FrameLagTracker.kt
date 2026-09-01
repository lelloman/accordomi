package com.lelloman.accordomi.data.tone

internal class FrameLagTracker(
    private val recoveryFrameCount: Int = DefaultRecoveryFrameCount,
) {
    private var previousSequenceNumber: Long? = null
    private var consecutiveCleanFrames = 0

    var isLagging: Boolean = false
        private set

    fun update(sequenceNumber: Long): Boolean {
        val previous = previousSequenceNumber
        previousSequenceNumber = sequenceNumber

        if (previous == null || sequenceNumber <= previous) {
            isLagging = false
            consecutiveCleanFrames = 0
            return isLagging
        }

        if (sequenceNumber > previous + 1) {
            isLagging = true
            consecutiveCleanFrames = 0
        } else if (isLagging) {
            consecutiveCleanFrames++
            if (consecutiveCleanFrames >= recoveryFrameCount) {
                isLagging = false
                consecutiveCleanFrames = 0
            }
        }

        return isLagging
    }

    private companion object {
        const val DefaultRecoveryFrameCount = 10
    }
}

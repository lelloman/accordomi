package com.lelloman.accordomi.data.tone

internal class FrameLagTracker(
    private val recoveryFrameCount: Int = DefaultRecoveryFrameCount,
) {
    private var previousSequenceNumber: Long? = null
    private var consecutiveCleanFrames = 0

    var isLagging: Boolean = false
        private set

    fun reset() {
        previousSequenceNumber = null
        consecutiveCleanFrames = 0
        isLagging = false
    }

    fun update(sequenceNumber: Long, expectedSequenceStep: Long = 1): Boolean {
        require(expectedSequenceStep > 0)
        val previous = previousSequenceNumber
        previousSequenceNumber = sequenceNumber

        if (previous == null || sequenceNumber <= previous) {
            isLagging = false
            consecutiveCleanFrames = 0
            return isLagging
        }

        if (sequenceNumber > previous + expectedSequenceStep) {
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

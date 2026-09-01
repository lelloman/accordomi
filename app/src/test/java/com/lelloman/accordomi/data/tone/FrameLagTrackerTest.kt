package com.lelloman.accordomi.data.tone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameLagTrackerTest {
    @Test
    fun reportsSequenceGapsAndRecoversAfterCleanFrames() {
        val tracker = FrameLagTracker(recoveryFrameCount = 3)

        assertFalse(tracker.update(10))
        assertTrue(tracker.update(12))
        assertTrue(tracker.update(13))
        assertTrue(tracker.update(14))
        assertFalse(tracker.update(15))
    }

    @Test
    fun resetsWhenRecorderSequenceRestarts() {
        val tracker = FrameLagTracker()

        tracker.update(10)
        assertTrue(tracker.update(12))

        assertFalse(tracker.update(0))
    }

    @Test
    fun usesTheConfiguredSequenceStep() {
        val tracker = FrameLagTracker(recoveryFrameCount = 1)

        assertFalse(tracker.update(sequenceNumber = 10, expectedSequenceStep = 2))
        assertFalse(tracker.update(sequenceNumber = 12, expectedSequenceStep = 2))
        assertTrue(tracker.update(sequenceNumber = 15, expectedSequenceStep = 2))
        assertFalse(tracker.update(sequenceNumber = 17, expectedSequenceStep = 2))
    }

    @Test
    fun explicitResetClearsLagState() {
        val tracker = FrameLagTracker()

        tracker.update(10)
        assertTrue(tracker.update(12))
        tracker.reset()

        assertFalse(tracker.update(20))
    }
}

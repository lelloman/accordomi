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
}

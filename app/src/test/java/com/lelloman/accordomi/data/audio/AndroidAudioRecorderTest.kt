package com.lelloman.accordomi.data.audio

import android.media.AudioRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidAudioRecorderTest {
    @Test
    fun rejectsInvalidMinimumBufferSizeBeforeCreatingRecorder() = runTest {
        val factory = FakeAudioRecordFactory(minimumBufferSize = AudioRecord.ERROR_BAD_VALUE)
        val recorder = recorder(factory)

        val error = runCatching { recorder.frames().first() }.exceptionOrNull()

        assertTrue(error is AudioRecordingException)
        assertFalse(factory.wasSessionCreated)
    }

    @Test
    fun releasesUninitializedRecorderWithoutTryingToStopIt() = runTest {
        val session = FakeAudioRecordSession(isInitialized = false)
        val recorder = recorder(FakeAudioRecordFactory(session = session))

        val error = runCatching { recorder.frames().first() }.exceptionOrNull()

        assertTrue(error is AudioRecordingException)
        assertEquals(0, session.stopCallCount)
        assertEquals(1, session.releaseCallCount)
    }

    @Test
    fun releasesRecorderWithoutStoppingWhenStartupFails() = runTest {
        val startFailure = IllegalStateException("start failed")
        val session = FakeAudioRecordSession(startFailure = startFailure)
        val recorder = recorder(FakeAudioRecordFactory(session = session))

        val error = runCatching { recorder.frames().first() }.exceptionOrNull()

        assertTrue(error is AudioRecordingException)
        assertTrue(error?.hasCause(startFailure) == true)
        assertEquals(0, session.stopCallCount)
        assertEquals(1, session.releaseCallCount)
    }

    @Test
    fun convertsSamplesAndCleansUpAfterCollectionStops() = runTest {
        val session = FakeAudioRecordSession(
            samples = shortArrayOf(Short.MIN_VALUE, 0, Short.MAX_VALUE),
            readResult = 3,
        )
        val recorder = recorder(FakeAudioRecordFactory(session = session))

        val frame = recorder.frames().first()

        assertEquals(44_100, frame.sampleRate)
        assertEquals(-1.00003f, frame.samples[0], 0.00001f)
        assertEquals(0f, frame.samples[1], 0f)
        assertEquals(1f, frame.samples[2], 0f)
        assertEquals(1, session.startCallCount)
        assertEquals(1, session.stopCallCount)
        assertEquals(1, session.releaseCallCount)
    }

    @Test
    fun reportsDeadRecorderAndStillCleansUp() = runTest {
        val session = FakeAudioRecordSession(readResult = AudioRecord.ERROR_DEAD_OBJECT)
        val recorder = recorder(FakeAudioRecordFactory(session = session))

        val error = runCatching { recorder.frames().first() }.exceptionOrNull()

        assertTrue(error is AudioRecordingException)
        assertTrue(error?.message?.contains("recreated") == true)
        assertEquals(1, session.stopCallCount)
        assertEquals(1, session.releaseCallCount)
    }

    @Test
    fun preservesReadFailureWhenCleanupAlsoFails() = runTest {
        val stopFailure = IllegalStateException("stop failed")
        val session = FakeAudioRecordSession(
            readResult = AudioRecord.ERROR,
            stopFailure = stopFailure,
        )
        val recorder = recorder(FakeAudioRecordFactory(session = session))

        val error = runCatching { recorder.frames().first() }.exceptionOrNull()
        val suppressedExceptions = generateSequence(error) { it.cause }
            .flatMap { it.suppressedExceptions.asSequence() }
            .toList()

        assertTrue(error is AudioRecordingException)
        assertTrue(
            "Suppressed exceptions: ${suppressedExceptions.map { it.message }}",
            suppressedExceptions.contains(stopFailure),
        )
        assertEquals(1, session.releaseCallCount)
    }

    private fun recorder(factory: AudioRecordFactory): AndroidAudioRecorder = AndroidAudioRecorder(
        ioDispatcher = UnconfinedTestDispatcher(),
        audioRecordFactory = factory,
    )

    private fun Throwable.hasCause(expected: Throwable): Boolean =
        generateSequence(this) { it.cause }.any { it === expected }

    private class FakeAudioRecordFactory(
        private val minimumBufferSize: Int = 8_192,
        private val session: AudioRecordSession = FakeAudioRecordSession(),
    ) : AudioRecordFactory {
        var wasSessionCreated = false

        override fun minimumBufferSize(
            sampleRate: Int,
            channelConfig: Int,
            audioFormat: Int,
        ): Int = minimumBufferSize

        override fun create(
            audioSource: Int,
            sampleRate: Int,
            channelConfig: Int,
            audioFormat: Int,
            bufferSize: Int,
        ): AudioRecordSession {
            wasSessionCreated = true
            return session
        }
    }

    private class FakeAudioRecordSession(
        override val isInitialized: Boolean = true,
        private val samples: ShortArray = shortArrayOf(),
        private val readResult: Int = AudioRecord.ERROR,
        private val startFailure: Throwable? = null,
        private val stopFailure: Throwable? = null,
    ) : AudioRecordSession {
        var startCallCount = 0
        var stopCallCount = 0
        var releaseCallCount = 0

        override fun start() {
            startCallCount++
            startFailure?.let { throw it }
        }

        override fun read(buffer: ShortArray): Int {
            samples.copyInto(buffer)
            return readResult
        }

        override fun stop() {
            stopCallCount++
            stopFailure?.let { throw it }
        }

        override fun release() {
            releaseCallCount++
        }
    }
}

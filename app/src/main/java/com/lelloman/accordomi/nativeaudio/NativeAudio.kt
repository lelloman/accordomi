package com.lelloman.accordomi.nativeaudio

import java.nio.ByteBuffer

/** JNI transport only. Numerical algorithms live in native/.
 * State arrays are owned by each Kotlin caller; no native handles can outlive it.
 */
object NativeAudio {
    init { System.loadLibrary("accordomi_jni") }

    private val pitchWorkspace = Workspace()
    private val pianoWorkspace = Workspace()

    fun detect(samples: FloatArray, sampleRate: Int, method: Int): DoubleArray? {
        if (samples.size < 512 || sampleRate <= 0) return null
        return pitchWorkspace.use(samples.size) { detectNative(samples, sampleRate, method, it) }
    }
    fun correlate(samples: FloatArray): DoubleArray =
        pitchWorkspace.use(samples.size) { correlateNative(samples, it) }
    fun piano(samples: FloatArray, sampleRate: Int, expectedHz: Double): DoubleArray =
        pianoWorkspace.use(samples.size) { pianoNative(samples, sampleRate, expectedHz, -1.0, it) }

    fun measurePiano(samples: FloatArray, sampleRate: Int, targetHz: Double, b: Double): DoubleArray =
        pianoWorkspace.use(samples.size) { pianoNative(samples, sampleRate, targetHz, b, it) }
    external fun pianoTargets(notes: IntArray, stiffness: DoubleArray, reference: Double): DoubleArray?
    external fun calibrationSummary(frequencies: DoubleArray, stiffness: DoubleArray, qualities: DoubleArray): DoubleArray?
    external fun cents(frequencyHz: Double, targetHz: Double): Double
    external fun stability(frequencies: DoubleArray, seconds: DoubleArray): Int

    private external fun workspaceBytes(count: Int): Int
    private external fun initializeWorkspace(storage: ByteBuffer, count: Int)
    private external fun detectNative(samples: FloatArray, sampleRate: Int, method: Int, workspace: ByteBuffer): DoubleArray?
    private external fun correlateNative(samples: FloatArray, workspace: ByteBuffer): DoubleArray
    private external fun pianoNative(samples: FloatArray, sampleRate: Int, expectedHz: Double, knownB: Double, workspace: ByteBuffer): DoubleArray

    // JVM/Android owns the memory lifetime. Lock covers both resizing and native use.
    private class Workspace {
        private var buffer: ByteBuffer? = null
        @Synchronized fun <T> use(count: Int, action: (ByteBuffer) -> T): T {
            val bytes = workspaceBytes(count)
            require(bytes > 0) { "Audio frame must contain 2..1048576 samples" }
            val storage = buffer?.takeIf { it.capacity() >= bytes }
                ?: ByteBuffer.allocateDirect(bytes).also {
                    initializeWorkspace(it, count)
                    buffer = it
                }
            return action(storage)
        }
    }
    external fun tuning(frequencyHz: Double, referenceHz: Double): DoubleArray
    external fun equalTemperedHz(midi: Int, referenceHz: Double): Double
    external fun stabilize(state: DoubleArray, frequencyHz: Double, clarity: Float): DoubleArray?
    external fun oscillator(state: DoubleArray, frequencyHz: Double, sampleRate: Int, releasing: Boolean, output: ShortArray)
}

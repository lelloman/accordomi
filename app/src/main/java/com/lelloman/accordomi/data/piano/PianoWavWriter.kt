package com.lelloman.accordomi.data.piano

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.roundToInt

/** Long frames overlap by 49152 samples: append only the new hop after frame zero. */
internal class PianoWavWriter(file: File) : AutoCloseable {
    private val out = RandomAccessFile(file, "rw")
    private var nextSequence = 0L
    private var samplesWritten = 0
    init { out.setLength(0); out.write(ByteArray(44)) }
    fun append(samples: FloatArray, sequence: Long) {
        require(samples.size == 65536 && sequence == nextSequence++)
        val start = if (sequence == 0L) 0 else samples.size - 16384
        val bytes = ByteArray((samples.size - start) * 2)
        for (i in start until samples.size) {
            val value = (samples[i] * 32767).roundToInt().coerceIn(-32768, 32767)
            bytes[(i-start)*2] = value.toByte(); bytes[(i-start)*2+1] = (value shr 8).toByte()
        }
        out.write(bytes); samplesWritten += samples.size - start
    }
    override fun close() {
        try {
            out.seek(0); out.writeBytes("RIFF"); out.le32(36 + samplesWritten * 2)
            out.writeBytes("WAVEfmt "); out.le32(16); out.le16(1); out.le16(1)
            out.le32(44100); out.le32(88200); out.le16(2); out.le16(16)
            out.writeBytes("data"); out.le32(samplesWritten * 2)
        } finally { out.close() }
    }
    private fun RandomAccessFile.le16(value: Int) { write(value and 255); write((value shr 8) and 255) }
    private fun RandomAccessFile.le32(value: Int) { repeat(4) { write((value shr (it*8)) and 255) } }
}

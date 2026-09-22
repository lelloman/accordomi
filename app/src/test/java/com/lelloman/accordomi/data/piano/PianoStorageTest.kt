package com.lelloman.accordomi.data.piano

import com.lelloman.accordomi.domain.piano.*
import com.lelloman.accordomi.nativeaudio.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class PianoStorageTest {
    @Test fun roundTripsDraftAndCompletedProfilesIncludingRawObservations() {
        val measurement = PianoMeasurement(PianoMeasurementStatus.Usable,110.0,.0003,.1,.9,
            listOf(PianoPartial(1,110.0,.5,110.0,.0,true)),16384)
        val sample = PianoSample(45,110.0,.0003,.9,123,listOf("take-a.wav","take-b.wav"),listOf(measurement))
        val draft = PianoProfile("test-id","Upright",442.0,123,listOf(sample))
        val full = draft.copy(id = "full", samples = CalibrationNotes.map { sample.copy(midi = it) }, targets = List(88) { PianoTarget(it+21,NativeAudio.equalTemperedHz(it+21,442.0),.0003) })
        assertEquals(listOf(draft,full),PianoProfileCodec.decode(PianoProfileCodec.encode(listOf(draft,full))))
        assertTrue(PianoProfileCodec.decode(null).isEmpty())
        assertThrows(Exception::class.java) { PianoProfileCodec.decode("bad data") }
        assertThrows(Exception::class.java) { PianoProfileCodec.decode(PianoProfileCodec.encode(listOf(draft)).dropLast(8)) }
        val unsafe = draft.copy(samples = listOf(sample.copy(recordings = listOf("../secret.wav"))))
        assertThrows(IllegalArgumentException::class.java) { PianoProfileCodec.decode(PianoProfileCodec.encode(listOf(unsafe))) }
    }
    @Test fun wavStoresOverlappingFramesWithoutDuplicatingAudio() {
        val file = Files.createTempFile("piano-test", ".wav").toFile()
        try {
            PianoWavWriter(file).use { writer ->
                writer.append(FloatArray(65536) { .25f },0)
                writer.append(FloatArray(65536) { if (it < 49152) .25f else .5f },1)
            }
            val bytes = file.readBytes(); val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals(44 + (65536+16384)*2, bytes.size)
            assertEquals("RIFF",bytes.copyOfRange(0,4).toString(Charsets.US_ASCII))
            assertEquals(44100,header.getInt(24))
            assertEquals(bytes.size-44,header.getInt(40))
            assertEquals(8192,header.getShort(44).toInt())
            assertEquals(16384,header.getShort(44+65536*2).toInt())
        } finally { file.delete() }
    }
}

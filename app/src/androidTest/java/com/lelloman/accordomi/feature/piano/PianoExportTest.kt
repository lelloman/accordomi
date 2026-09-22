package com.lelloman.accordomi.feature.piano

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.lelloman.accordomi.data.piano.PianoExporter
import com.lelloman.accordomi.data.piano.PianoWavWriter
import com.lelloman.accordomi.domain.piano.*
import com.lelloman.accordomi.nativeaudio.*
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PianoExportTest {
    @Test fun exportedZipContainsReplayableWavAndObservationPositions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir,"piano-recordings").apply { mkdirs() }
        val wav = File.createTempFile("exporttest", ".wav", directory)
        val zip = File.createTempFile("pianotest", ".zip", context.cacheDir)
        try {
            PianoWavWriter(wav).use { it.append(FloatArray(65536),0) }
            val observation = PianoMeasurement(PianoMeasurementStatus.Usable,110.0,.0003,.1,.9,
                listOf(PianoPartial(1,110.0,.3,110.0,0.0,true)),0,wav.name)
            val sample = PianoSample(45,110.0,.0003,.9,123,listOf(wav.name),listOf(observation))
            val profile = PianoProfile("export-test","Piano",440.0,123,listOf(sample))
            PianoExporter(context,Dispatchers.IO).export(profile,Uri.fromFile(zip))
            ZipFile(zip).use { archive ->
                assertEquals(wav.length(),archive.getEntry(wav.name).size)
                val json = JSONObject(archive.getInputStream(archive.getEntry("profile.json")).bufferedReader().readText())
                assertEquals(44100,json.getInt("sample_rate"))
                assertEquals(0,json.getJSONArray("missing_recordings").length())
                val exported = json.getJSONArray("samples").getJSONObject(0).getJSONArray("observations").getJSONObject(0)
                assertEquals(wav.name,exported.getString("recording"))
                assertEquals(0,exported.getLong("start_sample"))
                assertEquals(110.0,exported.getJSONArray("partials").getJSONObject(0).getDouble("measured_hz"),0.0)
            }
        } finally { wav.delete(); zip.delete() }
    }
}

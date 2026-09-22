package com.lelloman.accordomi.data.piano

import android.content.Context
import android.net.Uri
import com.lelloman.accordomi.core.di.IoDispatcher
import com.lelloman.accordomi.domain.piano.PianoProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface PianoExportService {
    suspend fun export(profile: PianoProfile, destination: Uri)
}

class PianoExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : PianoExportService {
    override suspend fun export(profile: PianoProfile, destination: Uri) = withContext(io) {
        val missing = JSONArray()
        val json = JSONObject().put("schema_version", 1).put("model_version", profile.modelVersion)
            .put("name", profile.name).put("id", profile.id).put("reference_hz", profile.referenceHz)
            .put("created_at_ms", profile.createdAt).put("frequency_convention", "actual_first_partial")
            .put("sample_rate", 44100).put("window_samples", 65536).put("hop_samples", 16384)
        json.put("samples", JSONArray().apply {
            profile.samples.forEach { sample -> put(JSONObject().put("midi", sample.midi)
                .put("first_partial_hz", sample.firstPartialHz).put("B", sample.inharmonicity)
                .put("quality", sample.quality).put("measured_at_ms", sample.measuredAt)
                .put("recordings", JSONArray(sample.recordings)).put("observations", JSONArray().apply {
                    sample.observations.forEach { m -> put(JSONObject().put("status", m.status.name)
                        .put("first_partial_hz", m.firstPartialHz).put("B", m.inharmonicity)
                        .put("rms_cents", m.rmsCents).put("quality", m.quality).put("start_sample", m.startSample).put("recording", m.recording)
                        .put("partials", JSONArray().apply { m.partials.forEach { p -> put(JSONObject()
                            .put("number", p.number).put("measured_hz", p.frequencyHz).put("amplitude", p.amplitude)
                            .put("predicted_hz", p.predictedHz).put("residual_cents", p.residualCents).put("used", p.used)) } })) }
                })) }
        })
        json.put("targets", JSONArray().apply { profile.targets.forEach { put(JSONObject()
            .put("midi", it.midi).put("target_hz", it.frequencyHz).put("B", it.inharmonicity)) } })
        ZipOutputStream(requireNotNull(context.contentResolver.openOutputStream(destination))).use { zip ->
            profile.samples.flatMap { it.recordings }.distinct().forEach { name ->
                require(name.matches(Regex("[a-zA-Z0-9-]+\\.wav")))
                val file = File(File(context.filesDir, "piano-recordings"), name)
                if (file.exists()) { zip.putNextEntry(ZipEntry(name)); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry() }
                else missing.put(name)
            }
            json.put("missing_recordings", missing)
            zip.putNextEntry(ZipEntry("profile.json")); zip.write(json.toString(2).toByteArray()); zip.closeEntry()
        }
    }
}

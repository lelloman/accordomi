package com.lelloman.accordomi.data.piano

import com.lelloman.accordomi.domain.piano.*
import com.lelloman.accordomi.nativeaudio.*
import java.io.*
import java.util.Base64

/** Bounded, versioned storage independent of Android APIs; targets are frozen with the profile. */
internal object PianoProfileCodec {
    fun encode(profiles: List<PianoProfile>): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(1); out.writeInt(profiles.size)
            profiles.forEach { p ->
                out.writeUTF(p.id); out.writeUTF(p.name); out.writeDouble(p.referenceHz)
                out.writeLong(p.createdAt); out.writeInt(p.modelVersion); out.writeInt(p.samples.size)
                p.samples.forEach { s ->
                    out.writeInt(s.midi); out.writeDouble(s.firstPartialHz); out.writeDouble(s.inharmonicity)
                    out.writeDouble(s.quality); out.writeLong(s.measuredAt)
                    out.writeInt(s.recordings.size); s.recordings.forEach(out::writeUTF)
                    out.writeInt(s.observations.size)
                    s.observations.forEach { m ->
                        out.writeInt(m.status.ordinal); out.writeDouble(m.firstPartialHz)
                        out.writeDouble(m.inharmonicity); out.writeDouble(m.rmsCents); out.writeDouble(m.quality); out.writeLong(m.startSample); out.writeUTF(m.recording)
                        out.writeInt(m.partials.size)
                        m.partials.forEach { part ->
                            out.writeInt(part.number); out.writeDouble(part.frequencyHz); out.writeDouble(part.amplitude)
                            out.writeDouble(part.predictedHz); out.writeDouble(part.residualCents); out.writeBoolean(part.used)
                        }
                    }
                }
                out.writeInt(p.targets.size)
                p.targets.forEach { out.writeInt(it.midi); out.writeDouble(it.frequencyHz); out.writeDouble(it.inharmonicity) }
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray()).also { require(it.length <= 8_000_000) }
    }

    fun decode(encoded: String?): List<PianoProfile> {
        if (encoded == null) return emptyList()
        require(encoded.length <= 8_000_000)
        DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(encoded))).use { input ->
            require(input.readInt() == 1)
            val profiles = List(input.count(20)) {
                val id = input.readUTF().also { require(it.matches(Regex("[a-zA-Z0-9-]{1,64}"))) }
                val name = input.readUTF().also { require(it.length in 1..80) }
                val reference = input.number(400.0, 480.0)
                val created = input.readLong(); val model = input.readInt().also { require(it == 1) }
                val samples = List(input.count(88)) {
                    val midi = input.readInt().also { require(it in 21..108) }
                    val hz = input.number(20.0, 6000.0); val b = input.number(0.0, .02)
                    val quality = input.number(0.0, 1.0); val timestamp = input.readLong()
                    val files = List(input.count(2)) { input.readUTF().also { file ->
                        require(file.matches(Regex("[a-zA-Z0-9-]+\\.wav")))
                    } }
                    val observations = List(input.count(32)) {
                        val status = PianoMeasurementStatus.entries[input.readInt().also { require(it in 0..4) }]
                        val first = input.number(0.0, 6000.0); val stiffness = input.number(0.0, .02)
                        val rms = input.number(0.0, 100.0); val q = input.number(0.0, 1.0); val startSample = input.readLong().also { require(it >= -1) }
                        val recording = input.readUTF().also { require(it.isEmpty() || it.matches(Regex("[a-zA-Z0-9-]+\\.wav"))) }
                        val partials = List(input.count(24)) {
                            PianoPartial(input.readInt().also { require(it in 1..24) }, input.number(0.0, 100000.0),
                                input.number(0.0, 10.0), input.number(0.0, 100000.0), input.number(-10000.0, 10000.0), input.readBoolean())
                        }
                        PianoMeasurement(status, first, stiffness, rms, q, partials, startSample, recording)
                    }
                    PianoSample(midi, hz, b, quality, timestamp, files, observations)
                }
                require(samples.map { it.midi }.distinct().size == samples.size)
                val targetCount = input.count(88).also { require(it == 0 || it == 88) }
                if (targetCount == 88) require(CalibrationNotes.all { note -> samples.any { it.midi == note } })
                val targets = List(targetCount) { index ->
                    PianoTarget(input.readInt().also { require(it == index + 21) }, input.number(20.0, 6000.0), input.number(0.0, .02))
                }
                require(targets.zipWithNext().all { (a, b) -> a.frequencyHz < b.frequencyHz })
                PianoProfile(id, name, reference, created, samples, targets, model)
            }
            require(profiles.map { it.id }.distinct().size == profiles.size && input.available() == 0)
            return profiles
        }
    }
    private fun DataInputStream.count(max: Int) = readInt().also { require(it in 0..max) }
    private fun DataInputStream.number(min: Double, max: Double) = readDouble().also { require(it.isFinite() && it in min..max) }
}

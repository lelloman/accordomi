package com.lelloman.accordomi.core.di

import com.lelloman.accordomi.data.audio.AndroidAudioRecorder
import com.lelloman.accordomi.data.audio.AndroidAudioRecordFactory
import com.lelloman.accordomi.data.audio.AudioRecordFactory
import com.lelloman.accordomi.data.audio.AudioRecorder
import com.lelloman.accordomi.data.audio.ReferenceTonePlayer
import com.lelloman.accordomi.domain.tone.ReferenceToneOutput
import com.lelloman.accordomi.data.pitch.AutoCorrelationPitchDetector
import com.lelloman.accordomi.data.pitch.McLeodPitchDetector
import com.lelloman.accordomi.data.pitch.PitchDetector
import com.lelloman.accordomi.data.pitch.YinPitchDetector
import com.lelloman.accordomi.data.settings.DataStoreSettingsRepository
import com.lelloman.accordomi.data.tone.DefaultToneDetectionRepository
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.domain.tone.ToneDetectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindPianoExporter(exporter: com.lelloman.accordomi.data.piano.PianoExporter): com.lelloman.accordomi.data.piano.PianoExportService

    @Binds
    abstract fun bindPianoProfiles(repository: com.lelloman.accordomi.data.piano.PianoProfileRepository): com.lelloman.accordomi.domain.piano.PianoProfiles

    @Binds
    abstract fun bindPianoCapture(source: com.lelloman.accordomi.data.piano.AndroidPianoCapture): com.lelloman.accordomi.domain.piano.PianoCaptureSource

    @Binds
    abstract fun bindReferenceToneOutput(player: ReferenceTonePlayer): ReferenceToneOutput

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        repository: DataStoreSettingsRepository,
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAudioRecorder(
        recorder: AndroidAudioRecorder,
    ): AudioRecorder

    @Binds
    @Singleton
    abstract fun bindAudioRecordFactory(
        factory: AndroidAudioRecordFactory,
    ): AudioRecordFactory

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindYinPitchDetector(
        detector: YinPitchDetector,
    ): PitchDetector

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindAutoCorrelationPitchDetector(
        detector: AutoCorrelationPitchDetector,
    ): PitchDetector

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindMcLeodPitchDetector(
        detector: McLeodPitchDetector,
    ): PitchDetector

    @Binds
    @Singleton
    abstract fun bindToneDetectionRepository(
        repository: DefaultToneDetectionRepository,
    ): ToneDetectionRepository
}

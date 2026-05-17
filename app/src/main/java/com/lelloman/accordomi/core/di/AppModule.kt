package com.lelloman.accordomi.core.di

import com.lelloman.accordomi.data.audio.AndroidAudioRecorder
import com.lelloman.accordomi.data.audio.AudioRecorder
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
    @IntoSet
    @Singleton
    abstract fun bindYinPitchDetector(
        detector: YinPitchDetector,
    ): PitchDetector

    @Binds
    @Singleton
    abstract fun bindToneDetectionRepository(
        repository: DefaultToneDetectionRepository,
    ): ToneDetectionRepository
}

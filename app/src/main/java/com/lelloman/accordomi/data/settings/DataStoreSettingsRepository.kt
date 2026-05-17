package com.lelloman.accordomi.data.settings

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lelloman.accordomi.domain.settings.AppSettings
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences ->
            AppSettings(
                referencePitchHz = preferences[ReferencePitchHzKey]
                    ?: AppSettings.DefaultReferencePitchHz,
                toneDetectionMethod = ToneDetectionMethod.fromStorageKey(
                    preferences[ToneDetectionMethodKey],
                ),
            )
        }

    override suspend fun setReferencePitchHz(referencePitchHz: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[ReferencePitchHzKey] = referencePitchHz
        }
    }

    override suspend fun setToneDetectionMethod(method: ToneDetectionMethod) {
        context.settingsDataStore.edit { preferences ->
            preferences[ToneDetectionMethodKey] = method.storageKey
        }
    }

    private companion object {
        val ReferencePitchHzKey = doublePreferencesKey("reference_pitch_hz")
        val ToneDetectionMethodKey = stringPreferencesKey("tone_detection_method")
    }
}

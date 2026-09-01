package com.lelloman.accordomi.data.settings

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lelloman.accordomi.domain.settings.AppSettings
import com.lelloman.accordomi.domain.settings.BuiltInTheme
import com.lelloman.accordomi.domain.settings.CustomTheme
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.domain.settings.ThemeId
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import com.lelloman.accordomi.domain.tone.DetectionRate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal val SettingsCorruptionHandler = ReplaceFileCorruptionHandler<Preferences> {
    emptyPreferences()
}

private val Context.settingsDataStore by preferencesDataStore(
    name = "settings",
    corruptionHandler = SettingsCorruptionHandler,
)

internal fun Flow<Preferences>.recoverFromSettingsReadFailure(): Flow<Preferences> = catch { error ->
    if (error is IOException) {
        emit(emptyPreferences())
    } else {
        throw error
    }
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .recoverFromSettingsReadFailure()
        .map { preferences ->
            AppSettings(
                referencePitchHz = preferences[ReferencePitchHzKey]
                    ?: AppSettings.DefaultReferencePitchHz,
                toneDetectionMethod = ToneDetectionMethod.fromStorageKey(
                    preferences[ToneDetectionMethodKey],
                ),
                detectionRate = DetectionRate.fromStorageKey(preferences[DetectionRateKey]),
                toneVisualizationStyle = ToneVisualizationStyle.fromStorageKey(
                    preferences[ToneVisualizationStyleKey],
                ),
                selectedThemeId = ThemeId(
                    preferences[SelectedThemeIdKey] ?: BuiltInTheme.System.id.value,
                ),
                customThemes = decodeCustomThemes(preferences[CustomThemesKey]),
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

    override suspend fun setDetectionRate(rate: DetectionRate) {
        context.settingsDataStore.edit { preferences ->
            preferences[DetectionRateKey] = rate.storageKey
        }
    }

    override suspend fun setToneVisualizationStyle(style: ToneVisualizationStyle) {
        context.settingsDataStore.edit { preferences ->
            preferences[ToneVisualizationStyleKey] = style.storageKey
        }
    }

    override suspend fun setSelectedThemeId(themeId: ThemeId) {
        context.settingsDataStore.edit { preferences ->
            preferences[SelectedThemeIdKey] = themeId.value
        }
    }

    override suspend fun upsertCustomTheme(theme: CustomTheme) {
        context.settingsDataStore.edit { preferences ->
            val themes = decodeCustomThemes(preferences[CustomThemesKey]).toMutableList()
            val existingIndex = themes.indexOfFirst { it.id == theme.id }
            if (existingIndex >= 0) {
                themes[existingIndex] = theme
            } else {
                themes += theme
            }
            preferences[CustomThemesKey] = encodeCustomThemes(themes)
        }
    }

    override suspend fun deleteCustomTheme(themeId: ThemeId) {
        context.settingsDataStore.edit { preferences ->
            val themes = decodeCustomThemes(preferences[CustomThemesKey])
                .filterNot { it.id == themeId }
            preferences[CustomThemesKey] = encodeCustomThemes(themes)
            if (preferences[SelectedThemeIdKey] == themeId.value) {
                preferences[SelectedThemeIdKey] = BuiltInTheme.System.id.value
            }
        }
    }

    private companion object {
        val ReferencePitchHzKey = doublePreferencesKey("reference_pitch_hz")
        val ToneDetectionMethodKey = stringPreferencesKey("tone_detection_method")
        val DetectionRateKey = stringPreferencesKey("detection_rate")
        val ToneVisualizationStyleKey = stringPreferencesKey("tone_visualization_style")
        val SelectedThemeIdKey = stringPreferencesKey("selected_theme_id")
        val CustomThemesKey = stringPreferencesKey("custom_themes")
    }
}

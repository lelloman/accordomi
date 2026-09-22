package com.lelloman.accordomi.data.piano

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lelloman.accordomi.domain.piano.PianoProfile
import com.lelloman.accordomi.domain.piano.PianoProfiles
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

private val Context.pianoDataStore by preferencesDataStore(name = "piano_profiles")

@Singleton
class PianoProfileRepository @Inject constructor(@param:ApplicationContext private val context: Context) : PianoProfiles {
    private val key = stringPreferencesKey("profiles_v1")
    override val profiles = context.pianoDataStore.data.map { PianoProfileCodec.decode(it[key]) }
    override suspend fun save(profile: PianoProfile) {
        context.pianoDataStore.edit { preferences ->
            val all = PianoProfileCodec.decode(preferences[key]).toMutableList()
            val index = all.indexOfFirst { it.id == profile.id }
            if (index >= 0) all[index] = profile else { require(all.size < 20); all += profile }
            preferences[key] = PianoProfileCodec.encode(all)
        }
    }
}

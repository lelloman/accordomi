package com.lelloman.accordomi.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreSettingsRepositoryTest {
    @Test
    fun ioReadFailureEmitsEmptyPreferences() = runTest {
        val preferences = failingPreferencesFlow(java.io.IOException("read failed"))
            .recoverFromSettingsReadFailure()
            .first()

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun corruptionHandlerReplacesDataWithEmptyPreferences() = runTest {
        val preferences = SettingsCorruptionHandler.handleCorruption(
            CorruptionException("invalid preferences"),
        )

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun unexpectedReadFailureIsRethrown() = runTest {
        val expected = IllegalStateException("unexpected")
        val actual = try {
            failingPreferencesFlow(expected)
                .recoverFromSettingsReadFailure()
                .first()
            null
        } catch (error: IllegalStateException) {
            error
        }

        assertSame(expected, actual)
    }

    private fun failingPreferencesFlow(error: Throwable) = flow<Preferences> {
        throw error
    }
}

package com.lelloman.accordomi.feature.piano

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.lelloman.accordomi.data.piano.PianoExportService
import com.lelloman.accordomi.domain.piano.*
import com.lelloman.accordomi.domain.settings.*
import com.lelloman.accordomi.domain.tone.*
import com.lelloman.accordomi.nativeaudio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PianoViewModelTest {
    private class Profiles : PianoProfiles {
        override val profiles = MutableStateFlow<List<PianoProfile>>(emptyList())
        override suspend fun save(profile: PianoProfile) { profiles.value = profiles.value.filterNot { it.id == profile.id } + profile }
    }
    private class Capture : PianoCaptureSource {
        var active = 0; var bad = false
        val discarded = mutableListOf<String>()
        override fun readings(expectedHz: Double, knownB: Double?, recording: String?) = flow {
            active++
            try {
                repeat(if (knownB == null) 20 else 9) { index ->
                    delay(400)
                    val m = PianoMeasurement(if (bad) PianoMeasurementStatus.PoorFit else PianoMeasurementStatus.Usable,
                        expectedHz, knownB ?: .0003, .1, .9, emptyList())
                    emit(PianoFrame(m,index.toLong(),1.5+index*.4))
                }
                awaitCancellation()
            } finally { active-- }
        }
        override suspend fun discard(recording: String) { discarded += recording }
    }
    private class Settings : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())
        override suspend fun setReferencePitchHz(referencePitchHz: Double) { settings.value = settings.value.copy(referencePitchHz = referencePitchHz) }
        override suspend fun setToneDetectionMethod(method: ToneDetectionMethod) = Unit
        override suspend fun setDetectionRate(rate: DetectionRate) = Unit
        override suspend fun setToneVisualizationStyle(style: ToneVisualizationStyle) = Unit
        override suspend fun setSelectedThemeId(themeId: ThemeId) = Unit
        override suspend fun upsertCustomTheme(theme: CustomTheme) = Unit
        override suspend fun deleteCustomTheme(themeId: ThemeId) = Unit
    }
    private val exporter = object : PianoExportService { override suspend fun export(profile: PianoProfile, destination: Uri) = Unit }

    @Test fun completeCalibrationProducesSavedNonEtTargetsAndStopsTuningOnPermissionLoss() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler); Dispatchers.setMain(dispatcher)
        val profiles = Profiles(); val capture = Capture(); val settings = Settings()
        val vm = PianoViewModel(profiles,capture,settings,exporter,dispatcher)
        val store = ViewModelStore().apply { put("piano",vm) }
        try {
            runCurrent(); vm.create("My upright"); advanceUntilIdle(); vm.permission(true)
            CalibrationNotes.forEach { midi ->
                assertEquals(midi,vm.uiState.value.calibrationMidi)
                vm.captureTake(); advanceUntilIdle()
                assertNotNull(vm.uiState.value.firstTake)
                assertFalse(vm.uiState.value.profile!!.samples.any { it.midi == midi })
                vm.captureTake(); advanceUntilIdle()
                assertNull(vm.uiState.value.firstTake)
                assertTrue(vm.uiState.value.profile!!.samples.any { it.midi == midi })
                assertEquals(0,capture.active)
                assertEquals(midi,vm.uiState.value.completedCalibrationMidi)
                vm.captureTake(); runCurrent() // An accepted note cannot accidentally start another capture.
                assertEquals(0,capture.active)
                vm.nextCalibrationNote()
            }
            vm.buildTuning(); advanceUntilIdle()
            val profile = vm.uiState.value.profile!!
            assertTrue(profile.ready)
            assertEquals(440.0,profile.targets[48].frequencyHz,1e-9)
            assertTrue(profile.targets.last().frequencyHz > NativeAudio.equalTemperedHz(108,440.0))
            settings.setReferencePitchHz(442.0)
            assertEquals(440.0,vm.uiState.value.profile!!.referenceHz,0.0)
            vm.selectNote(108); vm.listen(); advanceTimeBy(4000); runCurrent()
            assertEquals(1,capture.active)
            assertEquals(0.0,vm.uiState.value.cents!!,.001)
            assertEquals(1,vm.uiState.value.stability)
            vm.permission(false); runCurrent()
            assertEquals(0,capture.active)
            assertNull(vm.uiState.value.cents)
            assertFalse(vm.uiState.value.listening)
        } finally { store.clear(); Dispatchers.resetMain() }
    }
    @Test fun rejectedMeasurementsCannotAdvanceCalibrationAndPendingTakesAreDiscarded() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler); Dispatchers.setMain(dispatcher)
        val profiles = Profiles(); val capture = Capture()
        val vm = PianoViewModel(profiles,capture,Settings(),exporter,dispatcher)
        val store = ViewModelStore().apply { put("piano",vm) }
        try {
            runCurrent(); vm.create("Piano"); advanceUntilIdle(); vm.permission(true)
            capture.bad = true; vm.captureTake(); advanceUntilIdle()
            assertNotNull(vm.uiState.value.error)
            assertTrue(vm.uiState.value.profile!!.samples.isEmpty())
            assertEquals(0,capture.active)
            assertEquals(1,capture.discarded.size)
            capture.bad = false; vm.captureTake(); advanceUntilIdle()
            val first = vm.uiState.value.firstTake!!.recording
            vm.profiles(); runCurrent()
            assertTrue(first in capture.discarded)
        } finally { store.clear(); Dispatchers.resetMain() }
    }
}

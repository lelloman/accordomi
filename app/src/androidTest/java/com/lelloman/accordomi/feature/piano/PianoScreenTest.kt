package com.lelloman.accordomi.feature.piano

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lelloman.accordomi.domain.piano.*
import com.lelloman.accordomi.nativeaudio.NativeAudio
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PianoScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun newPianoRequiresANameAndAnExplicitAction() {
        var created: String? = null
        compose.setContent { MaterialTheme { PianoScreen(PianoUiState(loaded = true), PianoActions(create = { created = it })) } }
        compose.onNodeWithTag("piano_new").assertIsNotEnabled()
        compose.onNodeWithTag("piano_name").performTextInput("Upright")
        compose.onNodeWithTag("piano_new").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Upright",created) }
    }
    @Test fun calibrationDoesNotRecordWithoutPermission() {
        val profile = PianoProfile("id","Upright",440.0,0)
        compose.setContent { MaterialTheme { PianoScreen(PianoUiState(profile = profile, page = PianoPage.Calibration),PianoActions()) } }
        compose.onNodeWithTag("piano_calibration_note").assertTextEquals("A0")
        compose.onNodeWithTag("piano_record").assertIsDisplayed().assertIsNotEnabled()
    }
    @Test fun tuningDisplaysTheProfileTarget() {
        val midi = intArrayOf(21,33,45,57,60,69,81,88,93)
        val curve = NativeAudio.pianoTargets(midi,DoubleArray(9) { .0003 },440.0)!!
        val profile = PianoProfile("id","Upright",440.0,0,
            midi.map { PianoSample(it,NativeAudio.equalTemperedHz(it,440.0),.0003,.9,0,emptyList(),emptyList()) },
            List(88) { PianoTarget(it+21,curve[it+88],curve[it]) })
        compose.setContent { MaterialTheme { PianoScreen(PianoUiState(profile = profile,page = PianoPage.Tuning,midi = 108,hasPermission = true),PianoActions()) } }
        compose.onNodeWithTag("piano_target").assertTextContains(String.format(java.util.Locale.getDefault(),"%.3f",profile.targets.last().frequencyHz),substring = true)
        compose.onNodeWithTag("piano_listen").assertIsDisplayed().assertIsEnabled()
    }
}

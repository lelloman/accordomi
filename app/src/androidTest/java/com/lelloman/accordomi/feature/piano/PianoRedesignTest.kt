package com.lelloman.accordomi.feature.piano

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.lelloman.accordomi.R
import com.lelloman.accordomi.domain.piano.*
import com.lelloman.accordomi.domain.settings.BuiltInTheme
import com.lelloman.accordomi.nativeaudio.*
import com.lelloman.accordomi.ui.theme.AccordomiTheme
import com.lelloman.lellodesign.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PianoRedesignTest {
    @get:Rule val compose = createComposeRule()
    private fun profile(): PianoProfile {
        val notes=CalibrationNotes.toIntArray()
        val curve=NativeAudio.pianoTargets(notes,DoubleArray(notes.size) { .0003 },440.0)!!
        return PianoProfile("preview","Living room upright",440.0,0,
            notes.map { PianoSample(it,NativeAudio.equalTemperedHz(it,440.0),.0003,.9,0,emptyList(),emptyList()) },
            List(88) { PianoTarget(it+21,curve[it+88],curve[it]) })
    }
    private fun measured()=PianoMeasurement(PianoMeasurementStatus.Usable,439.39,.0003,.1,.9,
        List(5) { PianoPartial(it+1,440.0*(it+1),.1,440.0*(it+1),.1,true) })
    @Test fun readingLossKeepsMeterAndActionsAndDetailsPreserveTask() {
        var state by mutableStateOf(PianoUiState(profile=profile(),page=PianoPage.Tuning,hasPermission=true,listening=true))
        compose.setContent { AccordomiTheme { PianoScreen(state,PianoActions()) } }
        val meter=compose.onNodeWithTag("piano_meter").getUnclippedBoundsInRoot()
        val actions=compose.onNodeWithTag("piano_actions").getUnclippedBoundsInRoot()
        compose.runOnIdle {state=state.copy(cents=-2.4,measurement=measured())}
        compose.onNodeWithTag("piano_status").assertTextEquals("↑ Raise pitch")
        assertEquals(meter,compose.onNodeWithTag("piano_meter").getUnclippedBoundsInRoot())
        assertEquals(actions,compose.onNodeWithTag("piano_actions").getUnclippedBoundsInRoot())
        compose.runOnIdle {state=state.copy(cents=.4,stability=2)}
        compose.onNodeWithTag("piano_status").assertTextEquals("Pitch drifting")
        compose.runOnIdle {state=state.copy(cents=null,measurement=null,stability=0)}
        compose.onNodeWithTag("piano_cents").assertTextEquals("— cents")
        compose.onNodeWithTag("piano_details").performClick()
        compose.onNodeWithText("Piano details").assertIsDisplayed()
        compose.onNodeWithText("‹ Back").performClick()
        compose.onNodeWithTag("piano_meter").assertIsDisplayed()
        compose.runOnIdle { assertEquals(69,state.midi);assertTrue(state.listening) }
    }
    @Test fun nativeLightDarkPreviewsAndLargeTextKeepActionsAccessible() {
        val profile=profile()
        var state by mutableStateOf(PianoUiState(profile=profile,page=PianoPage.Tuning,hasPermission=true,listening=true,
            cents=-2.4,measurement=measured()))
        var dark by mutableStateOf(false)
        var large by mutableStateOf(false)
        compose.setContent {
            AccordomiTheme(selectedThemeId=if(dark) BuiltInTheme.Dark.id else BuiltInTheme.Light.id) {
                val density=LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density,if(large) 1.5f else 1f)) {
                    Box(if(large) Modifier.width(320.dp).fillMaxHeight() else Modifier.fillMaxSize()) {
                        LelloScaffold("Accordomi","Accordomi", listOf(
                            LelloDestination("tuner","Tuner"){Icon(painterResource(R.drawable.ic_nav_tuner),null)},
                            LelloDestination("piano","Piano"){Icon(painterResource(R.drawable.ic_nav_piano),null)},
                            LelloDestination("tone","Tone"){Icon(painterResource(R.drawable.ic_nav_tone),null)},
                            LelloDestination("settings","Settings"){Icon(painterResource(R.drawable.ic_nav_settings),null)}),"piano",{},
                            mobileNavigation=LelloMobileNavigation.Bottom,
                            logo={Image(painterResource(R.drawable.ic_brand),null,Modifier.size(36.dp).background(Color.White,RoundedCornerShape(8.dp)))},
                            actions={LelloAppearanceSelector(if(dark) LelloAppearance.Dark else LelloAppearance.Light,{dark=it==LelloAppearance.Dark})}
                        ) {padding->Box(Modifier.padding(padding).consumeWindowInsets(padding)) { PianoScreen(state,PianoActions()) }}
                    }
                }
            }
        }
        // Capture real composables using deterministic test measurements, never a simulated capture service in the app.
        for (calibration in listOf(false,true)) for (isDark in listOf(false,true)) {
            compose.runOnIdle { dark=isDark;state=if(calibration) state.copy(page=PianoPage.Calibration,
                profile=profile.copy(samples=profile.samples.take(4),targets=emptyList()),listening=false,cents=null,measurement=null)
                else state.copy(page=PianoPage.Tuning,profile=profile) }
            compose.waitForIdle()
            screenshot("${if(calibration) "calibration" else "tuning"}-${if(isDark) "dark" else "light"}")
        }
        compose.runOnIdle {large=true}
        compose.onNodeWithTag("piano_record").assertIsDisplayed().assertIsEnabled()
        screenshot("calibration-large-text")
    }
    private fun screenshot(name: String) {
        val image=compose.onRoot().captureToImage().asAndroidBitmap()
        val folder=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"ui-review").apply {mkdirs()}
        File(folder,"$name.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
}

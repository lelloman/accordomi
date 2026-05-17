package com.lelloman.accordomi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lelloman.accordomi.ui.AccordomiApp
import com.lelloman.accordomi.ui.theme.AccordomiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccordomiTheme {
                AccordomiApp()
            }
        }
    }
}

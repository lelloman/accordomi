package com.lelloman.accordomi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lelloman.accordomi.ui.theme.AccordomiTheme
import com.lelloman.accordomi.ui.theme.ThemeViewModel

@Composable
fun AccordomiRoot(
    viewModel: ThemeViewModel = hiltViewModel(),
) {
    val selectedThemeId by viewModel.selectedThemeId.collectAsStateWithLifecycle()
    AccordomiTheme(selectedThemeId = selectedThemeId) {
        AccordomiApp()
    }
}

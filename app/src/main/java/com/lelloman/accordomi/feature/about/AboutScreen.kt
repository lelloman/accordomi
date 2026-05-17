package com.lelloman.accordomi.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutRoute() {
    AboutScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("About") })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Accordomi",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "A minimal piano tuner focused on live tone detection.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}


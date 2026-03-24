package com.example.custompicker.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.custompicker.R

@Composable
fun MediaOptionActionBar(
    onSettingsClick: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
    ) {
        Button(
            onClick = onSettingsClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
        ) {
            Text(text = stringResource(id = R.string.settings))
        }
    }
}

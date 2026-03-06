package com.example.custompicker.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp

@Composable
fun CustomPickerTopBar() {
    val titleTextSize = with(LocalDensity.current) { 17.sp.toDp().toSp() }

    Surface {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
                    .statusBarsPadding(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "제목",
                fontSize = titleTextSize,
            )
        }
    }
}

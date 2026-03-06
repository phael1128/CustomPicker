package com.example.custompicker.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.custompicker.model.ItemGalleryMedia
import com.example.custompicker.screen.tab.imageandvideo.ImageAndVideoScreen
import com.example.custompicker.screen.tab.photo.PhotoScreen
import com.example.custompicker.screen.tab.video.VideoScreen
import com.example.custompicker.topbar.CustomPickerTopBar

@Composable
fun MainTabScreen(
    mediaList: List<ItemGalleryMedia>,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabTitles = listOf("사진", "비디오", "사진&비디오")

    Scaffold(
        topBar = {
            CustomPickerTopBar()
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = title) },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> PhotoScreen(mediaList = mediaList)
                    1 -> VideoScreen()
                    2 -> ImageAndVideoScreen()
                }
            }
        }
    }
}

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.custompicker.R
import com.example.custompicker.model.ItemGalleryMedia
import com.example.custompicker.model.PickerDir
import com.example.custompicker.screen.tab.imageandvideo.ImageAndVideoScreen
import com.example.custompicker.screen.tab.photo.PhotoScreen
import com.example.custompicker.screen.tab.video.VideoScreen
import com.example.custompicker.topbar.CustomPickerTopBar

@Composable
fun MainTabScreen(
    title: String,
    selectedTabIndex: Int,
    directoryList: List<PickerDir>,
    mediaList: List<ItemGalleryMedia>,
    currentSortingType: Int,
    onTabSelected: (Int) -> Unit,
    onDirectorySelected: (PickerDir) -> Unit,
    onMediaOptionsSaved: (Int) -> Unit,
) {
    var showOptionSheet by rememberSaveable { mutableStateOf(false) }
    val tabTitles =
        listOf(
            stringResource(id = R.string.photo_tab),
            stringResource(id = R.string.video_tab),
            stringResource(id = R.string.photo_video_tab),
        )

    LaunchedEffect(Unit) {
        onTabSelected(selectedTabIndex)
    }

    Scaffold(
        topBar = {
            CustomPickerTopBar(
                title = title,
                directoryList = directoryList,
                onDirectorySelected = onDirectorySelected,
            )
        },
        bottomBar = {
            MediaOptionActionBar(
                onSettingsClick = { showOptionSheet = true },
            )
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
                        onClick = { onTabSelected(index) },
                        text = { Text(text = title) },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> PhotoScreen(mediaList = mediaList)
                    1 -> VideoScreen(mediaList = mediaList)
                    2 -> ImageAndVideoScreen(mediaList = mediaList)
                }
            }
        }
    }

    if (showOptionSheet) {
        MediaOptionSettingBottomSheet(
            currentSortingType = currentSortingType,
            onDismissRequest = { showOptionSheet = false },
            onSaveClick = { sortingType ->
                showOptionSheet = false
                onMediaOptionsSaved(sortingType)
            },
        )
    }
}

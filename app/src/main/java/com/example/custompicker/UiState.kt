package com.example.custompicker

import com.example.custompicker.model.ItemGalleryMedia
import com.example.custompicker.model.PickerDir

data class UiState(
    val selectedTabIndex: Int = 0,
    val selectedBucketId: Long = 0L,
    val selectedDirectoryName: String = "전체",
    val directoryList: List<PickerDir> = emptyList(),
    val mediaList: List<ItemGalleryMedia> = emptyList(),
)

package com.example.custompicker

import com.example.custompicker.model.ItemGalleryMedia

data class UiState(
    val selectedTabIndex: Int = 0,
    val mediaList: List<ItemGalleryMedia> = emptyList(),
)

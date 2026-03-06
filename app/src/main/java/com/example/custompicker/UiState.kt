package com.example.custompicker

import com.example.custompicker.model.ItemGalleryMedia

data class UiState(
    val mediaList: List<ItemGalleryMedia> = emptyList(),
)
package com.example.custompicker.screen.tab.photo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.custompicker.model.ItemGalleryMedia
import com.example.custompicker.screen.list.CustomMediaGridList

@Composable
fun PhotoScreen(
    mediaList: List<ItemGalleryMedia>,
    modifier: Modifier = Modifier,
) {
    CustomMediaGridList(
        mediaList = mediaList,
        modifier = modifier.fillMaxSize(),
    )
}

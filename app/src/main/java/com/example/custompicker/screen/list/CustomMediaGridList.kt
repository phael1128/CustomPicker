package com.example.custompicker.screen.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.example.custompicker.R
import com.example.custompicker.model.ItemGalleryMedia

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CustomMediaGridList(
    mediaList: List<ItemGalleryMedia>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(
            items = mediaList,
            key = { it.id },
        ) { media ->
            val model = media.path ?: R.drawable.media_placeholder
            GlideImage(
                model = model,
                contentDescription = null,
                loading = placeholder(R.drawable.media_placeholder),
                failure = placeholder(R.drawable.media_placeholder),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

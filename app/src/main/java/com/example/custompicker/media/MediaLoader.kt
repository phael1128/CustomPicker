package com.example.custompicker.media

import android.os.CancellationSignal
import com.example.custompicker.model.ContentQuery
import com.example.custompicker.model.ItemGalleryMedia
import com.example.custompicker.model.PickerDir

interface MediaLoader {
    fun getAllViewDirectory(
        contentQuery: ContentQuery,
        bucketName: String,
    ): PickerDir?

    suspend fun getDirectoryList(
        emitSize: Int,
        contentQuery: ContentQuery,
        emit: suspend (List<PickerDir>) -> Unit,
        end: suspend () -> Unit,
    )

    suspend fun getMediaList(
        bucketId: Long,
        contentQuery: ContentQuery,
        sortingType: Int,
        emitSize: Int,
        cancellationSignal: CancellationSignal,
        emit: suspend (list: List<ItemGalleryMedia>?, canceled: Boolean) -> Unit,
    )

    suspend fun queryMediaItem(
        savePath: String,
        contentQuery: ContentQuery,
    ): ItemGalleryMedia?
}

package com.example.custompicker.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import com.example.custompicker.constants.PickerDefine
import com.example.custompicker.model.ContentQuery
import com.example.custompicker.model.ItemGalleryMedia
import com.example.custompicker.model.PickerDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import javax.inject.Inject

class MediaLoaderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MediaLoader {

    override fun getAllViewDirectory(
        contentQuery: ContentQuery,
        bucketName: String,
    ): PickerDir? {
        context.contentResolver
            .query(
                contentQuery.contentUri,
                arrayOf(contentQuery.columnsMediaData),
                "${contentQuery.columnsMediaSize} <> ?",
                arrayOf("0"),
                "${contentQuery.columnsMediaId} DESC",
            )?.use { cursor ->
                val dataIndex = cursor.getColumnIndex(contentQuery.columnsMediaData)
                if (cursor.moveToFirst()) {
                    val dataPath = cursor.getString(dataIndex)
                    if (!dataPath.isNullOrEmpty()) {
                        return PickerDir(
                            bucketId = PickerDefine.TYPE_ALL_VIEW.toLong(),
                            bucketName = bucketName,
                            thumbnailPath = dataPath,
                            counter = cursor.count,
                            driveType = PickerDefine.DRIVE_DEVICE_ALBUM,
                        )
                    }
                }
            }
        return null
    }

    override suspend fun getDirectoryList(
        emitSize: Int,
        contentQuery: ContentQuery,
        emit: suspend (List<PickerDir>) -> Unit,
        end: suspend () -> Unit,
    ) {
        val folderMap = LinkedHashMap<Long, PickerDir>()

        context.contentResolver
            .query(
                contentQuery.contentUri,
                arrayOf(
                    contentQuery.columnsMediaBucketId,
                    contentQuery.columnsMediaBucketDisplayName,
                    contentQuery.columnsMediaData,
                ),
                "${contentQuery.columnsMediaSize} <> ?",
                arrayOf("0"),
                "${contentQuery.columnsMediaBucketDisplayName} ASC, " +
                    "${contentQuery.columnsMediaBucketId} ASC, " +
                    "${contentQuery.columnsMediaDateModified} DESC",
            )?.use { cursor ->
                if (cursor.count == 0) {
                    end()
                    return
                }

                val bucketIdIndex = cursor.getColumnIndex(contentQuery.columnsMediaBucketId)
                val bucketNameIndex = cursor.getColumnIndex(contentQuery.columnsMediaBucketDisplayName)
                val pathIndex = cursor.getColumnIndex(contentQuery.columnsMediaData)

                if (cursor.moveToFirst()) {
                    do {
                        if (!currentCoroutineContext().isActive) {
                            end()
                            return
                        }

                        val bucketId = cursor.getLong(bucketIdIndex)
                        val path = cursor.getString(pathIndex)
                        if (path.isNullOrEmpty()) continue

                        val existing = folderMap[bucketId]
                        if (existing == null) {
                            folderMap[bucketId] =
                                PickerDir(
                                    bucketId = bucketId,
                                    bucketName = cursor.getString(bucketNameIndex).orEmpty(),
                                    thumbnailPath = path,
                                    counter = 1,
                                    driveType = PickerDefine.DRIVE_DEVICE_ALBUM,
                                )
                        } else {
                            existing.counter += 1
                        }
                    } while (cursor.moveToNext())
                }
            }

        emitByChunk(
            source = folderMap.values.toList(),
            emitSize = emitSize,
            emit = emit,
        )
        end()
    }

    override suspend fun getMediaList(
        bucketId: Long,
        contentQuery: ContentQuery,
        sortingType: Int,
        emitSize: Int,
        cancellationSignal: CancellationSignal,
        emit: suspend (list: List<ItemGalleryMedia>?, canceled: Boolean) -> Unit,
    ) {
        val sortOrder =
            if (sortingType == PickerDefine.TYPE_SORTING_MODIFIED_DATE) {
                "${contentQuery.columnsMediaDateModified} DESC, " +
                    "${contentQuery.columnsMediaDateAdded} DESC, " +
                    "${contentQuery.columnsMediaId} DESC"
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "${contentQuery.columnsMediaDateTaken} DESC, " +
                        "${contentQuery.columnsMediaDateAdded} DESC, " +
                        "${contentQuery.columnsMediaId} DESC"
                } else {
                    "${contentQuery.columnsMediaDateAdded} DESC, " +
                        "${contentQuery.columnsMediaId} DESC"
                }
            }

        val result = ArrayList<ItemGalleryMedia>()
        val chunkSize = if (emitSize > 0) emitSize else Int.MAX_VALUE

        val projection =
            mutableListOf(
                contentQuery.columnsMediaData,
                contentQuery.columnsMediaId,
                contentQuery.columnsMediaDateAdded,
                contentQuery.columnsMediaDateModified,
                contentQuery.columnsMediaSize,
            ).also { columns ->
                if (contentQuery == ContentQuery.Video) {
                    columns.add(contentQuery.columnsMediaDuration)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    columns.add(contentQuery.columnsMediaDateTaken)
                }
            }.toTypedArray()

        val (selection, selectionArgs) = buildBucketSelection(bucketId, contentQuery)

        try {
            context.contentResolver
                .query(
                    contentQuery.contentUri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder,
                    cancellationSignal,
                )?.use { cursor ->
                    if (cursor.count == 0) {
                        emit(emptyList(), false)
                        return
                    }

                    val dataIndex = cursor.getColumnIndex(contentQuery.columnsMediaData)
                    val idIndex = cursor.getColumnIndex(contentQuery.columnsMediaId)
                    val dateModifiedIndex = cursor.getColumnIndex(contentQuery.columnsMediaDateModified)
                    val dateAddedIndex = cursor.getColumnIndex(contentQuery.columnsMediaDateAdded)
                    val durationIndex = cursor.getColumnIndex(contentQuery.columnsMediaDuration)
                    val sizeIndex = cursor.getColumnIndex(contentQuery.columnsMediaSize)
                    val dateTakenIndex =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            cursor.getColumnIndex(contentQuery.columnsMediaDateTaken)
                        } else {
                            -1
                        }

                    if (cursor.moveToFirst()) {
                        do {
                            if (!currentCoroutineContext().isActive || cancellationSignal.isCanceled) {
                                emit(null, true)
                                return
                            }

                            val path = cursor.getString(dataIndex)
                            if (path.isNullOrEmpty()) continue

                            val mediaId = cursor.getLong(idIndex)
                            val dateModified = cursor.getLong(dateModifiedIndex) * 1000
                            val dateAdded = cursor.getLong(dateAddedIndex) * 1000
                            val duration = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L
                            val size = if (sizeIndex >= 0) cursor.getInt(sizeIndex) else 0

                            val item =
                                ItemGalleryMedia(
                                    id = mediaId,
                                    path = path,
                                    size = size,
                                    originalUri = ContentUris.withAppendedId(contentQuery.contentUri, mediaId),
                                    dateModified = dateModified,
                                    dateAdded = dateAdded,
                                    isVideo = contentQuery == ContentQuery.Video,
                                    duration = duration,
                                )

                            val dateTaken =
                                if (dateTakenIndex >= 0) {
                                    cursor.getLong(dateTakenIndex)
                                } else {
                                    0L
                                }
                            item.dateTaken = if (dateTaken == 0L) dateAdded else dateTaken

                            result.add(item)
                            if (result.size == chunkSize) {
                                emit(result.toList(), false)
                                result.clear()
                            }
                        } while (cursor.moveToNext())
                    }
                }
        } catch (_: Exception) {
            emit(null, false)
            return
        }

        if (result.isNotEmpty()) {
            emit(result.toList(), false)
        } else if (emitSize > 0) {
            emit(emptyList(), false)
        }
    }

    override suspend fun queryMediaItem(
        savePath: String,
        contentQuery: ContentQuery,
    ): ItemGalleryMedia? {
        val isVideo = contentQuery == ContentQuery.Video
        val projection =
            mutableListOf(
                contentQuery.columnsMediaData,
                contentQuery.columnsMediaId,
                contentQuery.columnsMediaDateAdded,
                contentQuery.columnsMediaDateModified,
                contentQuery.columnsMediaSize,
            ).also {
                if (isVideo) {
                    it.add(contentQuery.columnsMediaDuration)
                }
            }.toTypedArray()

        val queryUri: Uri
        val selection: String?
        val selectionArgs: Array<String>?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryUri = Uri.parse(savePath)
            selection = null
            selectionArgs = null
        } else {
            queryUri = contentQuery.contentUri
            selection = "${contentQuery.columnsMediaData} = ?"
            selectionArgs = arrayOf(savePath)
        }

        return try {
            context.contentResolver
                .query(queryUri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null

                    val dataIndex = cursor.getColumnIndex(contentQuery.columnsMediaData)
                    val idIndex = cursor.getColumnIndex(contentQuery.columnsMediaId)
                    val dateModifiedIndex = cursor.getColumnIndex(contentQuery.columnsMediaDateModified)
                    val dateAddedIndex = cursor.getColumnIndex(contentQuery.columnsMediaDateAdded)
                    val durationIndex = cursor.getColumnIndex(contentQuery.columnsMediaDuration)
                    val sizeIndex = cursor.getColumnIndex(contentQuery.columnsMediaSize)

                    val path = cursor.getString(dataIndex)
                    val mediaId = cursor.getLong(idIndex)
                    val dateModified = cursor.getLong(dateModifiedIndex) * 1000
                    val dateAdded = cursor.getLong(dateAddedIndex) * 1000
                    val duration = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L
                    val size = if (sizeIndex >= 0) cursor.getInt(sizeIndex) else 0

                    ItemGalleryMedia(
                        id = mediaId,
                        path = path,
                        size = size,
                        originalUri = ContentUris.withAppendedId(contentQuery.contentUri, mediaId),
                        dateModified = dateModified,
                        dateAdded = dateAdded,
                        isVideo = isVideo,
                        duration = duration,
                    )
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildBucketSelection(
        bucketId: Long,
        contentQuery: ContentQuery,
    ): Pair<String, Array<String>> {
        return if (bucketId == PickerDefine.TYPE_ALL_VIEW.toLong()) {
            "${contentQuery.columnsMediaSize} <> ?" to arrayOf("0")
        } else {
            "${contentQuery.columnsMediaBucketId} = ? AND ${contentQuery.columnsMediaSize} <> ?" to
                arrayOf(bucketId.toString(), "0")
        }
    }

    private suspend fun <T> emitByChunk(
        source: List<T>,
        emitSize: Int,
        emit: suspend (List<T>) -> Unit,
    ) {
        if (source.isEmpty()) {
            emit(emptyList())
            return
        }

        if (emitSize <= 0) {
            emit(source)
            return
        }

        var from = 0
        while (from < source.size) {
            if (!currentCoroutineContext().isActive) {
                return
            }
            val to = minOf(source.size, from + emitSize)
            emit(source.subList(from, to))
            from = to
        }
    }
}

package com.example.custompicker.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
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
        val (selection, selectionArgs) = buildBaseSelection(contentQuery)

        context.contentResolver
            .query(
                contentQuery.contentUri,
                arrayOf(contentQuery.columnsMediaData),
                selection,
                selectionArgs,
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
        val (selection, selectionArgs) = buildBaseSelection(contentQuery)

        context.contentResolver
            .query(
                contentQuery.contentUri,
                arrayOf(
                    contentQuery.columnsMediaBucketId,
                    contentQuery.columnsMediaBucketDisplayName,
                    contentQuery.columnsMediaData,
                ),
                selection,
                selectionArgs,
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
                if (supportsVideoColumns(contentQuery)) {
                    columns.add(contentQuery.columnsMediaDuration)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    columns.add(contentQuery.columnsMediaDateTaken)
                }
                if (requiresMediaTypeColumn(contentQuery)) {
                    columns.add(contentQuery.columnsMediaType)
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
                    val mediaTypeIndex =
                        if (requiresMediaTypeColumn(contentQuery)) {
                            cursor.getColumnIndex(contentQuery.columnsMediaType)
                        } else {
                            -1
                        }
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
                            val mediaType =
                                if (mediaTypeIndex >= 0) {
                                    cursor.getInt(mediaTypeIndex)
                                } else if (contentQuery == ContentQuery.Video) {
                                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                                } else {
                                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                                }
                            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

                            val item =
                                ItemGalleryMedia(
                                    id = mediaId,
                                    path = path,
                                    size = size,
                                    originalUri = buildOriginalUri(mediaId = mediaId, isVideo = isVideo),
                                    dateModified = dateModified,
                                    dateAdded = dateAdded,
                                    isVideo = isVideo,
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
        val mixedMediaQuery = requiresMediaTypeColumn(contentQuery)
        val projection =
            mutableListOf(
                contentQuery.columnsMediaData,
                contentQuery.columnsMediaId,
                contentQuery.columnsMediaDateAdded,
                contentQuery.columnsMediaDateModified,
                contentQuery.columnsMediaSize,
            ).also {
                if (supportsVideoColumns(contentQuery)) {
                    it.add(contentQuery.columnsMediaDuration)
                }
                if (mixedMediaQuery) {
                    it.add(contentQuery.columnsMediaType)
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
                    val mediaTypeIndex =
                        if (mixedMediaQuery) {
                            cursor.getColumnIndex(contentQuery.columnsMediaType)
                        } else {
                            -1
                        }

                    val path = cursor.getString(dataIndex)
                    val mediaId = cursor.getLong(idIndex)
                    val dateModified = cursor.getLong(dateModifiedIndex) * 1000
                    val dateAdded = cursor.getLong(dateAddedIndex) * 1000
                    val duration = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L
                    val size = if (sizeIndex >= 0) cursor.getInt(sizeIndex) else 0
                    val isVideo =
                        if (mediaTypeIndex >= 0) {
                            cursor.getInt(mediaTypeIndex) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                        } else {
                            contentQuery == ContentQuery.Video
                        }

                    ItemGalleryMedia(
                        id = mediaId,
                        path = path,
                        size = size,
                        originalUri = buildOriginalUri(mediaId = mediaId, isVideo = isVideo),
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
        val selectionClauses = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (bucketId != PickerDefine.TYPE_ALL_VIEW.toLong()) {
            selectionClauses += "${contentQuery.columnsMediaBucketId} = ?"
            selectionArgs += bucketId.toString()
        }

        appendMediaTypeSelection(contentQuery, selectionClauses, selectionArgs)

        selectionClauses += "${contentQuery.columnsMediaSize} <> ?"
        selectionArgs += "0"

        return selectionClauses.joinToString(separator = " AND ") to selectionArgs.toTypedArray()
    }

    private fun buildBaseSelection(contentQuery: ContentQuery): Pair<String, Array<String>> {
        val selectionClauses = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        appendMediaTypeSelection(contentQuery, selectionClauses, selectionArgs)

        selectionClauses += "${contentQuery.columnsMediaSize} <> ?"
        selectionArgs += "0"

        return selectionClauses.joinToString(separator = " AND ") to selectionArgs.toTypedArray()
    }

    /**
     * MediaStore.Files는 이미지/비디오 외의 다른 파일 타입도 함께 반환할 수 있다.
     * 그래서 혼합 탭에서는 디렉터리 조회와 미디어 조회 모두에서
     * MEDIA_TYPE_IMAGE / MEDIA_TYPE_VIDEO 조건을 동일하게 붙여줘야 한다.
     *
     * @param contentQuery
     * @param selectionClauses
     * @param selectionArgs
     */
    private fun appendMediaTypeSelection(
        contentQuery: ContentQuery,
        selectionClauses: MutableList<String>,
        selectionArgs: MutableList<String>,
    ) {
        if (contentQuery.mediaTypes.isEmpty()) return

        val placeholders = contentQuery.mediaTypes.joinToString(separator = " OR ") {
            "${contentQuery.columnsMediaType} = ?"
        }
        selectionClauses += "($placeholders)"
        selectionArgs += contentQuery.mediaTypes.map(Int::toString)
    }

    private fun requiresMediaTypeColumn(contentQuery: ContentQuery): Boolean =
        contentQuery == ContentQuery.Files

    private fun supportsVideoColumns(contentQuery: ContentQuery): Boolean =
        contentQuery == ContentQuery.Video || contentQuery == ContentQuery.Files

    private fun buildOriginalUri(
        mediaId: Long,
        isVideo: Boolean,
    ): Uri =
        ContentUris.withAppendedId(
            if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            },
            mediaId,
        )

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

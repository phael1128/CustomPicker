package com.example.custompicker.model

import android.net.Uri
import android.provider.MediaStore

enum class ContentQuery(
    val contentUri: Uri,
    val columnsMediaId: String,
    val columnsMediaData: String,
    val columnsMediaBucketDisplayName: String,
    val columnsMediaBucketId: String,
    val columnsMediaDateModified: String,
    val columnsMediaDateAdded: String,
    val columnsMediaDuration: String,
    val columnsMediaDateTaken: String,
    val columnsMediaSize: String,
    val type: String,
) {
    Image(
        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        columnsMediaId = MediaStore.Images.Media._ID,
        columnsMediaData = MediaStore.Images.Media.DATA,
        columnsMediaBucketDisplayName = MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        columnsMediaBucketId = MediaStore.Images.Media.BUCKET_ID,
        columnsMediaDateModified = MediaStore.Images.Media.DATE_MODIFIED,
        columnsMediaDateAdded = MediaStore.Images.Media.DATE_ADDED,
        columnsMediaDuration = MediaStore.Images.Media.DURATION,
        columnsMediaDateTaken = MediaStore.Images.Media.DATE_TAKEN,
        columnsMediaSize = MediaStore.Images.Media.SIZE,
        type = "image/*",
    ),
    Video(
        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        columnsMediaId = MediaStore.Video.Media._ID,
        columnsMediaData = MediaStore.Video.Media.DATA,
        columnsMediaBucketDisplayName = MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        columnsMediaBucketId = MediaStore.Video.Media.BUCKET_ID,
        columnsMediaDateModified = MediaStore.Video.Media.DATE_MODIFIED,
        columnsMediaDateAdded = MediaStore.Video.Media.DATE_ADDED,
        columnsMediaDuration = MediaStore.Video.Media.DURATION,
        columnsMediaDateTaken = MediaStore.Video.Media.DATE_TAKEN,
        columnsMediaSize = MediaStore.Video.Media.SIZE,
        type = "video/*",
    ),
}

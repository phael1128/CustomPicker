package com.example.custompicker.model

data class PickerDir(
    val bucketId: Long = 0L,
    val bucketName: String = "",
    val thumbnailPath: String = "",
    var counter: Int = 1,
    val driveType: String = "",
)

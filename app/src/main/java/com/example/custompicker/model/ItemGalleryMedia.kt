package com.example.custompicker.model

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow

class ItemGalleryMedia(
    val id: Long,
    var path: String? = null,
    var size: Int = 0,
    val originalUri: Uri?,
    val dateModified: Long,
    val dateAdded: Long,
    val isVideo: Boolean,
    val duration: Long,
) {
    var dateTaken: Long = 0
    var selectedNumber = -1
    val selectNumberString = MutableStateFlow("")
    val selectedVisible = MutableStateFlow(false)
    var selectedIndex = -1
    var index = -1

    fun setSelectedIndex(num: Int?) {
        selectedIndex = num ?: -1
        selectedNumber = if (selectedIndex != -1) (selectedIndex + 1) else -1
        selectNumberString.value = if (selectedIndex != -1) (selectedIndex + 1).toString() else ""
        selectedVisible.value = selectedIndex != -1
    }

    fun isSelected(): Boolean = selectedIndex != -1
}

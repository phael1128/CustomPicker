package com.example.custompicker

import com.example.custompicker.constants.PickerDefine
import com.example.custompicker.core.compose.ui.ViewEffect
import com.example.custompicker.core.compose.ui.ViewEvent
import com.example.custompicker.core.compose.ui.ViewState
import com.example.custompicker.model.ItemGalleryMedia
import com.example.custompicker.model.PickerDir

class MainContract {
    sealed class Event : ViewEvent {
        data object Initialize : Event()

        data class OnTabSelected(
            val tabIndex: Int,
        ) : Event()

        data class OnDirectorySelected(
            val directory: PickerDir,
        ) : Event()

        data class OnSortingTypeChanged(
            val sortingType: Int,
        ) : Event()
    }

    data class UiState(
        val isInitialized: Boolean = false,
        val selectedTabIndex: Int = 0,
        val selectedBucketId: Long = 0L,
        val selectedDirectoryName: String = "전체",
        val directoryList: List<PickerDir> = emptyList(),
        val mediaList: List<ItemGalleryMedia> = emptyList(),
        val sortingType: Int = PickerDefine.TYPE_SORTING_MODIFIED_DATE,
    ) : ViewState

    sealed class Effect : ViewEffect
}

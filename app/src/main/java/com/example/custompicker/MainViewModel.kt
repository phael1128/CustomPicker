package com.example.custompicker

import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.custompicker.constants.PickerDefine
import com.example.custompicker.di.IoDispatcher
import com.example.custompicker.handler.coroutineExceptionHandler
import com.example.custompicker.media.MediaLoader
import com.example.custompicker.model.ContentQuery
import com.example.custompicker.model.ItemGalleryMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaLoader: MediaLoader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _mediaList = MutableStateFlow(UiState())
    val mediaList = _mediaList.asStateFlow()

    private var loadMediaJob: Job? = null
    private var loadCancellationSignal: CancellationSignal? = null

    fun onTabSelected(tabIndex: Int) {
        when (tabIndex) {
            0 -> loadSingleMediaList(tabIndex = tabIndex, contentQuery = ContentQuery.Image)
            1 -> loadSingleMediaList(tabIndex = tabIndex, contentQuery = ContentQuery.Video)
            2 -> loadImageAndVideoMediaList()
        }
    }

    private fun loadSingleMediaList(
        tabIndex: Int,
        contentQuery: ContentQuery,
    ) {
        loadMediaJob?.cancel()
        loadCancellationSignal?.cancel()

        val cancellationSignal = CancellationSignal()
        loadCancellationSignal = cancellationSignal
        _mediaList.value = UiState(selectedTabIndex = tabIndex)

        loadMediaJob =
            viewModelScope.launch(coroutineExceptionHandler) {
                withContext(ioDispatcher) {
                    mediaLoader.getMediaList(
                        bucketId = PickerDefine.TYPE_ALL_VIEW.toLong(),
                        contentQuery = contentQuery,
                        sortingType = PickerDefine.TYPE_SORTING_MODIFIED_DATE,
                        emitSize = 3000,
                        cancellationSignal = cancellationSignal,
                    ) { list, canceled ->
                        if (canceled || list.isNullOrEmpty()) return@getMediaList
                        _mediaList.update { state ->
                            if (state.selectedTabIndex != tabIndex) {
                                state
                            } else {
                                state.copy(mediaList = state.mediaList + list)
                            }
                        }
                    }
                }
            }
    }

    private fun loadImageAndVideoMediaList() {
        loadMediaJob?.cancel()
        loadCancellationSignal?.cancel()

        val cancellationSignal = CancellationSignal()
        loadCancellationSignal = cancellationSignal
        _mediaList.value = UiState(selectedTabIndex = IMAGE_AND_VIDEO_TAB_INDEX)

        loadMediaJob =
            viewModelScope.launch(coroutineExceptionHandler) {
                withContext(ioDispatcher) {
                    val imageMediaList = mutableListOf<ItemGalleryMedia>()
                    val videoMediaList = mutableListOf<ItemGalleryMedia>()

                    suspend fun publishCombinedList() {
                        _mediaList.update { state ->
                            if (state.selectedTabIndex != IMAGE_AND_VIDEO_TAB_INDEX) {
                                state
                            } else {
                                state.copy(
                                    mediaList = mergeAndSortMediaList(imageMediaList, videoMediaList),
                                )
                            }
                        }
                    }

                    mediaLoader.getMediaList(
                        bucketId = PickerDefine.TYPE_ALL_VIEW.toLong(),
                        contentQuery = ContentQuery.Image,
                        sortingType = PickerDefine.TYPE_SORTING_MODIFIED_DATE,
                        emitSize = 3000,
                        cancellationSignal = cancellationSignal,
                    ) { list, canceled ->
                        if (canceled || list.isNullOrEmpty()) return@getMediaList
                        imageMediaList += list
                        publishCombinedList()
                    }

                    mediaLoader.getMediaList(
                        bucketId = PickerDefine.TYPE_ALL_VIEW.toLong(),
                        contentQuery = ContentQuery.Video,
                        sortingType = PickerDefine.TYPE_SORTING_MODIFIED_DATE,
                        emitSize = 3000,
                        cancellationSignal = cancellationSignal,
                    ) { list, canceled ->
                        if (canceled || list.isNullOrEmpty()) return@getMediaList
                        videoMediaList += list
                        publishCombinedList()
                    }
                }
            }
    }

    private fun mergeAndSortMediaList(
        imageMediaList: List<ItemGalleryMedia>,
        videoMediaList: List<ItemGalleryMedia>,
    ): List<ItemGalleryMedia> =
        (imageMediaList + videoMediaList).sortedWith(
            compareByDescending<ItemGalleryMedia> { it.dateModified }
                .thenByDescending { it.dateAdded }
                .thenByDescending { it.id },
        )

    private companion object {
        const val IMAGE_AND_VIDEO_TAB_INDEX = 2
    }
}

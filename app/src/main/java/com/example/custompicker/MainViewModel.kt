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
import com.example.custompicker.model.PickerDir
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
    private var loadDirectoryJob: Job? = null
    private var loadCancellationSignal: CancellationSignal? = null

    fun onTabSelected(tabIndex: Int) {
        _mediaList.update {
            it.copy(
                selectedTabIndex = tabIndex,
                selectedBucketId = PickerDefine.TYPE_ALL_VIEW.toLong(),
                selectedDirectoryName = ALL_DIRECTORY_NAME,
                directoryList = emptyList(),
                mediaList = emptyList(),
            )
        }
        loadDirectoryList(tabIndex)
        loadMediaList(
            tabIndex = tabIndex,
            bucketId = PickerDefine.TYPE_ALL_VIEW.toLong(),
        )
    }

    fun onDirectorySelected(directory: PickerDir) {
        val currentTabIndex = _mediaList.value.selectedTabIndex
        _mediaList.update {
            it.copy(
                selectedBucketId = directory.bucketId,
                selectedDirectoryName = directory.bucketName,
                mediaList = emptyList(),
            )
        }
        loadMediaList(
            tabIndex = currentTabIndex,
            bucketId = directory.bucketId,
        )
    }

    private fun loadDirectoryList(tabIndex: Int) {
        loadDirectoryJob?.cancel()

        loadDirectoryJob =
            viewModelScope.launch(coroutineExceptionHandler) {
                withContext(ioDispatcher) {
                    val directories =
                        when (tabIndex) {
                            0 -> buildDirectoryMenuList(loadDirectoryEntries(ContentQuery.Image))
                            1 -> buildDirectoryMenuList(loadDirectoryEntries(ContentQuery.Video))
                            2 -> buildDirectoryMenuList(loadDirectoryEntries(ContentQuery.Files))
                            else -> listOf(buildAllDirectory(counter = 0))
                        }

                    _mediaList.update { state ->
                        if (state.selectedTabIndex != tabIndex) {
                            state
                        } else {
                            val selectedDirectory =
                                directories.firstOrNull { it.bucketId == state.selectedBucketId }
                                    ?: directories.firstOrNull()
                                    ?: buildAllDirectory(counter = 0)

                            state.copy(
                                selectedBucketId = selectedDirectory.bucketId,
                                selectedDirectoryName = selectedDirectory.bucketName,
                                directoryList = directories,
                            )
                        }
                    }
                }
            }
    }

    private suspend fun loadDirectoryEntries(contentQuery: ContentQuery): List<PickerDir> {
        var directories = emptyList<PickerDir>()
        mediaLoader.getDirectoryList(
            emitSize = 0,
            contentQuery = contentQuery,
            emit = { list ->
                directories = list
            },
            end = {},
        )
        return directories
    }

    private fun loadMediaList(
        tabIndex: Int,
        bucketId: Long,
    ) {
        when (tabIndex) {
            0 -> loadSingleMediaList(tabIndex, bucketId, ContentQuery.Image)
            1 -> loadSingleMediaList(tabIndex, bucketId, ContentQuery.Video)
            2 -> loadSingleMediaList(tabIndex, bucketId, ContentQuery.Files)
        }
    }

    private fun loadSingleMediaList(
        tabIndex: Int,
        bucketId: Long,
        contentQuery: ContentQuery,
    ) {
        loadMediaJob?.cancel()
        loadCancellationSignal?.cancel()

        val cancellationSignal = CancellationSignal()
        loadCancellationSignal = cancellationSignal

        loadMediaJob =
            viewModelScope.launch(coroutineExceptionHandler) {
                withContext(ioDispatcher) {
                    mediaLoader.getMediaList(
                        bucketId = bucketId,
                        contentQuery = contentQuery,
                        sortingType = PickerDefine.TYPE_SORTING_MODIFIED_DATE,
                        emitSize = 3000,
                        cancellationSignal = cancellationSignal,
                    ) { list, canceled ->
                        if (canceled || list.isNullOrEmpty()) return@getMediaList
                        _mediaList.update { state ->
                            if (state.selectedTabIndex != tabIndex || state.selectedBucketId != bucketId) {
                                state
                            } else {
                                state.copy(mediaList = state.mediaList + list)
                            }
                        }
                    }
                }
            }
    }

    private fun buildDirectoryMenuList(directories: List<PickerDir>): List<PickerDir> =
        listOf(buildAllDirectory(counter = directories.sumOf { it.counter })) + directories

    private fun buildAllDirectory(counter: Int): PickerDir =
        PickerDir(
            bucketId = PickerDefine.TYPE_ALL_VIEW.toLong(),
            bucketName = ALL_DIRECTORY_NAME,
            counter = counter,
            driveType = PickerDefine.DRIVE_DEVICE_ALBUM,
        )

    private companion object {
        const val ALL_DIRECTORY_NAME = "전체"
    }
}

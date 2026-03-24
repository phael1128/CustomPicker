package com.example.custompicker

import android.os.CancellationSignal
import androidx.lifecycle.viewModelScope
import com.example.custompicker.constants.PickerDefine
import com.example.custompicker.core.compose.ui.BaseViewModelWithState
import com.example.custompicker.di.IoDispatcher
import com.example.custompicker.handler.coroutineExceptionHandler
import com.example.custompicker.media.MediaLoader
import com.example.custompicker.model.ContentQuery
import com.example.custompicker.model.PickerDir
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaLoader: MediaLoader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BaseViewModelWithState<MainContract.UiState, MainContract.Event, MainContract.Effect>() {

    private var loadMediaJob: Job? = null
    private var loadDirectoryJob: Job? = null
    private var loadCancellationSignal: CancellationSignal? = null

    override fun setInitialState(): MainContract.UiState = MainContract.UiState()

    override fun handleEvents(event: MainContract.Event) {
        when (event) {
            MainContract.Event.Initialize -> initialize()
            is MainContract.Event.OnTabSelected -> onTabSelected(event.tabIndex)
            is MainContract.Event.OnDirectorySelected -> onDirectorySelected(event.directory)
            is MainContract.Event.OnSortingTypeChanged -> onMediaOptionsSaved(event.sortingType)
        }
    }

    private fun initialize() {
        if (currentState.isInitialized) return

        setState {
            copy(isInitialized = true)
        }

        onTabSelected(currentState.selectedTabIndex)
    }

    private fun onTabSelected(tabIndex: Int) {
        setState {
            copy(
                isInitialized = true,
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

    private fun onDirectorySelected(directory: PickerDir) {
        val currentTabIndex = currentState.selectedTabIndex
        setState {
            copy(
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

    private fun onMediaOptionsSaved(sortingType: Int) {
        val sortingChanged = currentState.sortingType != sortingType

        if (!sortingChanged) return

        setState {
            copy(
                sortingType = sortingType,
                mediaList = emptyList(),
            )
        }

        loadMediaList(
            tabIndex = currentState.selectedTabIndex,
            bucketId = currentState.selectedBucketId,
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

                    if (currentState.selectedTabIndex != tabIndex) {
                        return@withContext
                    }

                    val selectedDirectory =
                        directories.firstOrNull { it.bucketId == currentState.selectedBucketId }
                            ?: directories.firstOrNull()
                            ?: buildAllDirectory(counter = 0)

                    setState {
                        copy(
                            selectedBucketId = selectedDirectory.bucketId,
                            selectedDirectoryName = selectedDirectory.bucketName,
                            directoryList = directories,
                        )
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
        val sortingType = currentState.sortingType

        loadMediaJob =
            viewModelScope.launch(coroutineExceptionHandler) {
                withContext(ioDispatcher) {
                    mediaLoader.getMediaList(
                        bucketId = bucketId,
                        contentQuery = contentQuery,
                        sortingType = sortingType,
                        emitSize = 3000,
                        cancellationSignal = cancellationSignal,
                    ) { list, canceled ->
                        if (canceled || list.isNullOrEmpty()) return@getMediaList

                        if (currentState.selectedTabIndex != tabIndex || currentState.selectedBucketId != bucketId) {
                            return@getMediaList
                        }

                        setState {
                            copy(mediaList = mediaList + list)
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

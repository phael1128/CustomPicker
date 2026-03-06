package com.example.custompicker

import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.custompicker.constants.PickerDefine
import com.example.custompicker.di.IoDispatcher
import com.example.custompicker.handler.coroutineExceptionHandler
import com.example.custompicker.media.MediaLoader
import com.example.custompicker.model.ContentQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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

    init {
        loadMediaList()
    }

    private fun loadMediaList() {
        viewModelScope.launch {
            withContext(ioDispatcher + coroutineExceptionHandler) {
                mediaLoader.getMediaList(
                    bucketId = PickerDefine.TYPE_ALL_VIEW.toLong(),
                    contentQuery = ContentQuery.Image,
                    sortingType = PickerDefine.TYPE_SORTING_MODIFIED_DATE,
                    emitSize = 3000,
                    cancellationSignal = CancellationSignal(),
                ) { list, canceled ->
                    if (canceled || list.isNullOrEmpty()) return@getMediaList
                    _mediaList.update { state ->
                        state.copy(mediaList = state.mediaList + list)
                    }
                }
            }
        }
    }
}

package com.example.custompicker.core.compose.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

interface ViewState

interface ViewEvent

interface ViewEffect

abstract class BaseViewModelWithState<UiState : ViewState, UiEvent : ViewEvent, UiEffect : ViewEffect> :
    ViewModel() {
    private val initialState: UiState by lazy { setInitialState() }

    abstract fun setInitialState(): UiState

    val currentState: UiState
        get() = _viewState.value

    private val _viewState: MutableStateFlow<UiState> = MutableStateFlow(initialState)
    val viewState: StateFlow<UiState> = _viewState.asStateFlow()

    private val _event: MutableSharedFlow<UiEvent> = MutableSharedFlow()

    private val _effect: Channel<UiEffect> = Channel()
    val effect = _effect.receiveAsFlow()

    init {
        subscribeToEvents()
    }

    fun setEvent(event: UiEvent) {
        viewModelScope.launch { _event.emit(event) }
    }

    protected fun setState(reducer: UiState.() -> UiState) {
        _viewState.value = _viewState.value.reducer()
    }

    private fun subscribeToEvents() {
        viewModelScope.launch {
            _event.collect { handleEvents(it) }
        }
    }

    abstract fun handleEvents(event: UiEvent)

    protected fun setEffect(builder: () -> UiEffect) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
    }
}

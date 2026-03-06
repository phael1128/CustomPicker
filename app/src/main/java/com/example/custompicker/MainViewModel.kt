package com.example.custompicker

import androidx.lifecycle.ViewModel
import com.example.custompicker.media.MediaLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaLoader: MediaLoader
) : ViewModel() {

}
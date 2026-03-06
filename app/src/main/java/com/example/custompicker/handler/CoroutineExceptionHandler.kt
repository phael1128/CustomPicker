package com.example.custompicker.handler

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler

val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    Log.e("phael", "Error Message : ${throwable.message}")
}
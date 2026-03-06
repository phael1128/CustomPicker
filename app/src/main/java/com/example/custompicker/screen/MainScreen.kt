package com.example.custompicker.screen

import androidx.compose.runtime.Composable

@Composable
fun MainScreen(
    hasStoragePermission: Boolean,
    onInitializeClick: () -> Unit
) {
    NavigationConfiguration(
        hasStoragePermission = hasStoragePermission,
        onInitializeClick = onInitializeClick
    )
}

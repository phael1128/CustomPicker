package com.example.custompicker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.custompicker.screen.MainScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var hasStoragePermission by mutableStateOf(false)

    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            hasStoragePermission = hasStorageReadPermission()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasStoragePermission = hasStorageReadPermission()
        setContent {
            MainScreen(
                hasStoragePermission = hasStoragePermission,
                onInitializeClick = {
                    requestStoragePermission()
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        hasStoragePermission = hasStorageReadPermission()
    }

    private fun requestStoragePermission() {
        val deniedPermissions =
            requiredStorageReadPermissions().filter { permission ->
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
            }

        if (deniedPermissions.isEmpty()) {
            hasStoragePermission = true
            return
        }

        requestStoragePermissionLauncher.launch(deniedPermissions.toTypedArray())
    }

    private fun hasStorageReadPermission(): Boolean =
        requiredStorageReadPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

    private fun requiredStorageReadPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
}

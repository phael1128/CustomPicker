package com.example.custompicker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.custompicker.screen.MainScreen

class MainActivity : ComponentActivity() {
    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Permission result is handled when the user responds.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(
                hasStoragePermission = hasStorageReadPermission(),
                onInitializeClick = {
                    requestStoragePermission()
                }
            )
        }
    }

    private fun requestStoragePermission() {
        val deniedPermissions =
            requiredStorageReadPermissions().filter { permission ->
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
            }

        if (deniedPermissions.isEmpty()) return

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

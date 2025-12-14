package com.example.newgallery.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * 通用权限处理组件
 * 统一处理Android 13+的权限请求逻辑
 */
@Composable
fun rememberPermissionState(
    permission: String = Manifest.permission.READ_MEDIA_IMAGES,
    onPermissionResult: (Boolean) -> Unit = {}
): PermissionState {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
        if (!granted) {
            showRationale = true
        } else {
            showRationale = false
        }
        onPermissionResult(granted)
    }
    
    return PermissionState(
        isGranted = isGranted,
        showRationale = showRationale,
        requestPermission = { permissionLauncher.launch(permission) }
    )
}

data class PermissionState(
    val isGranted: Boolean,
    val showRationale: Boolean,
    val requestPermission: () -> Unit
)
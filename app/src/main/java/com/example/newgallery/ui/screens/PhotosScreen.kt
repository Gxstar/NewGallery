package com.example.newgallery.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import com.example.newgallery.ui.viewmodel.ViewModelFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.delay
import com.example.newgallery.ui.components.PhotoGrid

/**
 * 照片屏幕 - 展示所有照片，按日期分组
 */
@SuppressLint("InlinedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    onPhotoClick: (photo: com.example.newgallery.data.model.Photo, index: Int) -> Unit,
    sharedPhotoViewModel: SharedPhotoViewModel
) {
    val context = LocalContext.current
    
    // 使用共享的ViewModel实例
    
    // 标准Android权限处理 - 向后兼容不同API级别
    val permissionGranted = remember { mutableStateOf(false) }
    val showPermissionRationale = remember { mutableStateOf(false) }
    val permissionRequested = remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 根据API级别检查不同的权限
        val allGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // API 33+ 使用新的权限
            val imagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            val videoGranted = permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
            imagesGranted && videoGranted
        } else {
            // API 30-32 使用旧的权限
            val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            readGranted
        }
        
        permissionGranted.value = allGranted
        // 检查是否需要显示权限说明
        showPermissionRationale.value = !allGranted
    }
    
    // 检查是否已经拥有权限，避免重复请求
    LaunchedEffect(Unit) {
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        permissionGranted.value = hasPermission
        
        // 如果已经拥有权限，标记为已请求过，避免重复请求
        if (hasPermission) {
            permissionRequested.value = true
        }
    }
    
    // 状态观察 - 从SharedPhotoViewModel获取
    val isLoading by sharedPhotoViewModel.isLoading.collectAsState()
    val photosByDate by sharedPhotoViewModel.photosByDate.collectAsState()
    val error by sharedPhotoViewModel.error.collectAsState()
    
    // 预计算照片数据，避免重复计算
    val allPhotos = remember(photosByDate) {
        photosByDate.values.flatten()
    }
    
    // 请求权限 - 只在应用首次启动且需要时请求
    LaunchedEffect(permissionRequested.value) {
        // 只在权限未请求过且未授予的情况下请求
        if (!permissionRequested.value && !permissionGranted.value) {
            // 延迟一小段时间再请求权限，避免UI闪烁
            kotlinx.coroutines.delay(300)
            val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // API 33+ 使用新的权限
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                // API 30-32 使用旧的权限
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionRequested.value = true
            permissionLauncher.launch(permissions)
        }
    }
    
    // 加载照片 - 优化加载顺序避免资源竞争
    LaunchedEffect(permissionGranted.value) {
        if (permissionGranted.value) {
            // 使用合并后的ViewModel方法
            sharedPhotoViewModel.loadPhotosByDate()
            // 延迟一小段时间再加载共享数据，避免同时大量IO操作
            delay(100)
            sharedPhotoViewModel.loadAllPhotos()
        }
    }
    
    // 设置点击回调
    val handlePhotoClick = remember<(com.example.newgallery.data.model.Photo, Int) -> Unit> {
        { photo, index ->
            // 保存滚动位置 - 使用合并后的ViewModel方法
            sharedPhotoViewModel.clearScrollState() // 先清除之前的状态
            
            // 如果是视频，使用系统播放器播放
            if (photo.mimeType.startsWith("video/")) {
                try {
                    val intent: android.content.Intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(photo.uri, photo.mimeType)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("PhotosScreen", "无法播放视频: ${e.message}")
                    // 如果播放失败，仍然导航到详情页作为备选方案
                    onPhotoClick(photo, index)
                }
            } else {
                // 图片正常导航到详情页
                onPhotoClick(photo, index)
            }
        }
    }
    
    // 使用Flow监听收藏状态
    val favoritePhotoIds by sharedPhotoViewModel.favoritePhotoIds.collectAsState(initial = emptyList())
    
    // 直接使用PhotoGrid组件，不再依赖PhotoListScreen
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "照片和视频") },
                actions = {
                    Text(
                        text = "${allPhotos.size} 张照片",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { sharedPhotoViewModel.loadPhotosByDate() }) {
                            Text("重试")
                        }
                    }
                }
                allPhotos.isEmpty() -> {
                    Text(
                        text = "未找到照片或视频",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    // 照片网格
                    PhotoGrid(
                        photos = allPhotos,
                        columnCount = 5,
                        showDateHeaders = true,
                        onPhotoClick = handlePhotoClick,
                        modifier = Modifier.fillMaxSize(),
                        enableZoom = true,
                        favoritePhotoIds = favoritePhotoIds
                    )
                }
            }
        }
    }
}
package com.example.newgallery.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.newgallery.ui.components.PhotoItem
import com.example.newgallery.ui.viewmodel.PhotosViewModel
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import com.example.newgallery.ui.viewmodel.ViewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun PhotosScreen(
onPhotoClick: (photo: com.example.newgallery.data.model.Photo, index: Int) -> Unit
) {
// 网格缩放状态
var gridScale by remember { mutableFloatStateOf(1f) }
val baseColumnCount = 4
val minColumnCount = 2
val maxColumnCount = 8
val currentColumnCount = remember(gridScale) {
(baseColumnCount * gridScale).toInt().coerceIn(minColumnCount, maxColumnCount)
}
    
    // 权限处理
    val permissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )
    
    val context = LocalContext.current
    val viewModel: PhotosViewModel = viewModel(factory = ViewModelFactory(context))
    val sharedViewModel: SharedPhotoViewModel = viewModel(factory = ViewModelFactory(context))
    
    val isLoading by viewModel.isLoading.collectAsState()
    val photosByYear by viewModel.photosByYear.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // 请求权限
    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }
    
    // 加载照片
    LaunchedEffect(permissionState.status) {
        if (permissionState.status == PermissionStatus.Granted) {
            viewModel.loadPhotos()
            sharedViewModel.loadAllPhotos()
        }
    }
    
    val allPhotos = photosByYear.values.flatten()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (permissionState.status) {
            PermissionStatus.Granted -> {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    error != null -> {
                        Text(
                            text = error ?: "未知错误",
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }
                    photosByYear.isEmpty() -> {
                        Text(
                            text = "未找到照片",
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        // 主内容
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    // 双指缩放手势处理
                                    detectTransformGestures(
                                        onGesture = { _, _, zoom, _ ->
                                            if (zoom != 1f) {
                                                // 计算缩放变化量：
                                                // iOS相册逻辑：根据zoom变化量调整列数
                                                // zoom > 1.0 时减少列数（放大）
                                                // zoom < 1.0 时增加列数（缩小）
                                                val scaleChange = if (zoom > 1f) {
                                                    // 放大：减少列数，gridScale应该减小
                                                    -(zoom - 1f) * 0.5f
                                                } else {
                                                    // 缩小：增加列数，gridScale应该增大
                                                    (1f - zoom) * 0.5f
                                                }
                                                
                                                // 更新缩放状态
                                                val newScale = (gridScale + scaleChange).coerceIn(0.5f, 2f)
                                                if (newScale != gridScale) {
                                                    gridScale = newScale
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            // 照片网格
                            PhotosGridWithIOSZoom(
                                photosByYear = photosByYear,
                                allPhotos = allPhotos,
                                currentColumnCount = currentColumnCount,
                                onPhotoClick = onPhotoClick
                            )
                        }
                    }
                }
            }
            is PermissionStatus.Denied -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "需要权限来访问照片",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    if (permissionState.status.shouldShowRationale) {
                        Text(
                            text = "请授予权限以查看您的照片",
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = {
                            permissionState.launchPermissionRequest()
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(text = "授予权限")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotosGridWithIOSZoom(
    photosByYear: Map<Int, List<com.example.newgallery.data.model.Photo>>,
    allPhotos: List<com.example.newgallery.data.model.Photo>,
    currentColumnCount: Int,
    onPhotoClick: (photo: com.example.newgallery.data.model.Photo, index: Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(currentColumnCount),
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        photosByYear.keys.sortedDescending().forEach { year ->
            val photos = photosByYear[year] ?: emptyList()
            
            // 年份标题
            item(span = { GridItemSpan(maxLineSpan) }) {
                YearHeader(year = year)
            }
            
            // 照片项目
            items(photos) { photo ->
                PhotoItem(
                    photo = photo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    onClick = {
                        val index = allPhotos.indexOf(photo)
                        onPhotoClick(photo, index)
                    }
                )
            }
        }
    }
}

@Composable
private fun YearHeader(year: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = year.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
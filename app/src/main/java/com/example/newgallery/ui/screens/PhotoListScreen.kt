package com.example.newgallery.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.newgallery.data.model.Photo
import com.example.newgallery.ui.components.*

/**
 * 统一的照片列表展示组件
 * 支持两种模式：按日期分组模式（PhotosScreen）和纯网格模式（相册详情）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoListScreen(
    title: String,
    photos: List<Photo>,
    isLoading: Boolean,
    error: String?,
    showDateHeaders: Boolean = true,
    baseColumnCount: Int = 5,
    onPhotoClick: (Photo, Int) -> Unit,
    onRetry: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    showActions: Boolean = true,
    onSettingsClick: (() -> Unit)? = null,
    enableZoom: Boolean = true
) {
    // 网格缩放状态
    var gridScale by remember { mutableStateOf(1f) }
    val minColumnCount = 2
    val maxColumnCount = 8
    val currentColumnCount = remember(gridScale) {
        (baseColumnCount * gridScale).toInt().coerceIn(minColumnCount, maxColumnCount)
    }
    
    // 网格状态
    val lazyGridState = rememberLazyGridState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                },
                actions = {
                    if (showActions) {
                        // 显示照片数量
                        Text(
                            text = "${photos.size} 张照片",
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (onSettingsClick != null) {
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多选项"
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            text = error,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text("重试")
                        }
                    }
                }
                photos.isEmpty() -> {
                    Text(
                        text = if (showDateHeaders) "未找到照片或视频" else "此相册中没有照片",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    // 照片网格
                    PhotoGrid(
                        photos = photos,
                        columnCount = currentColumnCount,
                        showDateHeaders = showDateHeaders,
                        onPhotoClick = onPhotoClick,
                        modifier = Modifier.fillMaxSize(),
                        enableZoom = enableZoom,
                        onZoomChange = { newScale ->
                            gridScale = newScale
                        }
                    )
                }
            }
        }
    }
}